package com.bandlab.intellij.plugin.strings

import com.bandlab.intellij.plugin.localizer.LocalizerConfigService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import kotlin.io.path.readText
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtTypeAlias

/**
 * Detection of Android `R.string.X` / `R.plurals.X` references in Kotlin code, shared by the
 * "Add string" unresolved-reference quick fix ([AddStringUnresolvedQuickFixRegistrar]) and the
 * "Update String" intention ([UpdateStringIntention]).
 */

/**
 * Referenced name of an Android string/plurals resource reference (`R.string.X` / `R.plurals.X`)
 * when [offset] sits on the trailing name part, else null. Recognizes the reference in every form:
 * bare `R.string.X`, package-qualified `com.app.R.string.X`, the `R`-class alias `appR.string.X`
 * (`typealias appR = com.app.R`), and the **member alias** `Strings.X` (`typealias Strings =
 * R.string`) — the dominant bandlab-android form. The `offset - 1` fallback covers a caret at the
 * name's trailing boundary.
 *
 * The key (`X`) itself never needs to resolve, so this works for not-yet-defined keys too (the Add
 * case). See [resStringRClassFqn] for how each form maps to its `R` class.
 */
internal fun resStringKeyAt(psiFile: PsiFile, offset: Int): String? {
    val element = psiFile.findElementAt(offset)
        ?: (if (offset > 0) psiFile.findElementAt(offset - 1) else null)
        ?: return null
    val ref = PsiTreeUtil.getParentOfType(element, KtNameReferenceExpression::class.java, false) ?: return null
    return resStringKey(ref)
}

/** Resource key of [ref] when it is the trailing name of an `R.string.X`/`R.plurals.X` reference. */
internal fun resStringKey(ref: KtNameReferenceExpression): String? =
    if (resStringRClassFqn(ref) != null) ref.getReferencedName() else null

/**
 * Fully-qualified name of the `R` class behind [ref] when it is the trailing name of an
 * `R.string.X`/`R.plurals.X` reference, else null. Doubles as the detection predicate for
 * [resStringKey] (non-null ⇔ [ref] is a string/plurals key). Covers every form:
 * - bare `R.string.X` → `"R"` (won't match a module mapping, which is fine),
 * - package-qualified `com.app.R.string.X` → `"com.app.R"`,
 * - `R`-class alias `appR.string.X` (`import com.app.R as appR` / `typealias appR = com.app.R`) → resolved R FQN,
 * - **member alias** `Strings.X` (`typealias Strings = R.string` / `import com.app.R.string as Strings`) →
 *   resolved R FQN. This last form is the common one in bandlab-android.
 */
internal fun resStringRClassFqn(ref: KtNameReferenceExpression): String? {
    val qualified = ref.parent as? KtDotQualifiedExpression ?: return null
    if (qualified.selectorExpression !== ref) return null // ref must be the trailing name
    return rClassFqnOfStringReceiver(qualified.receiverExpression)
}

/**
 * R class FQN when [receiver] is the receiver of a string/plurals key access, else null. Two shapes:
 * - `<R>.string` / `<R>.plurals` — bare/qualified text, or an alias head resolving to an `R` class;
 * - a plain name aliasing the `R.string`/`R.plurals` member itself (`typealias Strings = R.string`).
 */
private fun rClassFqnOfStringReceiver(receiver: KtExpression): String? = when (receiver) {
    is KtDotQualifiedExpression -> {
        val selector = (receiver.selectorExpression as? KtNameReferenceExpression)?.getReferencedName()
        if (selector != "string" && selector != "plurals") null
        else rClassFqnFromText(receiver.text) ?: aliasedRClassFqn(receiver)
    }
    is KtNameReferenceExpression -> rClassFqnFromMemberAlias(receiver)
    else -> null
}

/** R class FQN for a bare/package-qualified `R.string`/`R.plurals` receiver text, else null. */
private fun rClassFqnFromText(receiver: String): String? = when {
    receiver == "R.string" || receiver == "R.plurals" -> "R"
    receiver.endsWith(".R.string") -> receiver.removeSuffix(".string")
    receiver.endsWith(".R.plurals") -> receiver.removeSuffix(".plurals")
    else -> null
}

/**
 * R class FQN behind an aliased `<alias>.string`/`<alias>.plurals` receiver where `<alias>` stands
 * for an `R` class — `import com.app.R as appR` or `typealias appR = com.app.R`. The head reference
 * is resolved (works even when the aliased *member* doesn't resolve — e.g. before indexing).
 */
private fun aliasedRClassFqn(receiver: KtDotQualifiedExpression): String? {
    val head = receiver.receiverExpression as? KtNameReferenceExpression ?: return null
    val fqn = head.mainReference.resolve()?.resolvedFqnText() ?: return null
    return fqn.takeIf { it == "R" || it.endsWith(".R") }
}

/**
 * R class FQN for the bandlab convention where a strings module exposes `Strings` (= `R.string`) and
 * `Plurals` (= `R.plurals`) as siblings of `R` in the same package, referenced as `Strings.key`.
 * This is the dominant form in bandlab-android.
 *
 * Resolved purely from the file's import (text only — no type resolution, so it holds without a
 * Gradle sync, when the other module isn't indexed): `import <pkg>.Strings` ⇒ R class `<pkg>.R`.
 * Requiring a matching import also avoids false positives on unrelated locals named `Strings`. An
 * unexpected import shape still detects the reference, with the target picked explicitly (bare `R`).
 */
private fun rClassFqnFromMemberAlias(name: KtNameReferenceExpression): String? {
    val referenced = name.getReferencedName()
    if (referenced != STRINGS_ALIAS && referenced != PLURALS_ALIAS) return null
    val imported = name.containingKtFile.importDirectives
        .firstOrNull { (it.aliasName ?: it.importedFqName?.shortName()?.asString()) == referenced }
        ?.importedFqName ?: return null
    return when (imported.shortName().asString()) {
        STRINGS_ALIAS, PLURALS_ALIAS -> imported.parent().asString() + ".R" // import <pkg>.Strings
        else -> "R" // e.g. import <pkg>.R.string as Strings — detected, target picked explicitly
    }
}

private const val STRINGS_ALIAS = "Strings"
private const val PLURALS_ALIAS = "Plurals"

/** FQN/text a resolved declaration stands for: a class/object FQN, or a type alias's target text. */
private fun PsiElement.resolvedFqnText(): String? = when (this) {
    is KtTypeAlias -> getTypeReference()?.text
    is PsiClass -> qualifiedName
    is KtClassOrObject -> fqName?.asString()
    else -> null
}

/**
 * Every `<string>`/`<plurals>` key already defined across the manifest's base files. Cached and
 * recomputed only on a PSI change (covers edits to the base files) so a moving caret — which
 * recomputes the intention list — doesn't re-read every base file from disk each time.
 */
internal fun localBaseKeys(project: Project): Set<String> =
    CachedValuesManager.getManager(project).getCachedValue(project) {
        // `name` is not necessarily the first attribute (e.g. `<string translatable="false" name="…">`),
        // so match it anywhere inside the opening tag.
        val regex = Regex("<(?:string|plurals)\\b[^>]*?\\bname\\s*=\\s*\"([^\"]+)\"")
        val keys = project.service<LocalizerConfigService>().targets()
            .flatMap { target ->
                runCatching { target.baseFile.readText() }.getOrDefault("")
                    .let { text -> regex.findAll(text).map { it.groupValues[1] }.toList() }
            }
            .toSet()
        CachedValueProvider.Result.create(keys, PsiModificationTracker.MODIFICATION_COUNT)
    }
