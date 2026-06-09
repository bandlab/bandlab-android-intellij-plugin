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
 * bare `R.string.X`, package-qualified `com.app.R.string.X`, and **type-aliased** `appR.string.X`
 * (`typealias appR = com.app.R`). The `offset - 1` fallback covers a caret at the name's trailing
 * boundary.
 *
 * The bare/qualified forms are recognized textually; the aliased forms can't be — a text check can't
 * see through `appR` — so they are resolved. The key (`X`) itself never needs to resolve, so this
 * works for not-yet-defined keys too (the Add case).
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
    if (resStringReceiver(ref) != null) ref.getReferencedName() else null

/**
 * Fully-qualified name of the `R` class behind [ref] when it is the trailing name of an
 * `R.string.X`/`R.plurals.X` reference, else null. Parallel to [resStringKey]:
 * - bare `R.string.X` → `"R"` (won't match any module mapping, which is fine),
 * - package-qualified `com.app.R.string.X` → `"com.app.R"`,
 * - import-aliased / typealiased (`appR.string.X`) → the resolved R class FQN (e.g. `com.app.R`).
 */
internal fun resStringRClassFqn(ref: KtNameReferenceExpression): String? {
    val receiver = resStringReceiver(ref) ?: return null
    // [receiver] is the `<R>.string` / `<R>.plurals` expression; drop the trailing selector.
    rClassFqnFromText(receiver.text)?.let { return it }
    return aliasedRClassFqn(receiver)
}

/**
 * The `<R>.string`/`<R>.plurals` qualified receiver of [ref] when [ref] is the trailing name of an
 * `R.string.X`/`R.plurals.X` reference (in any form: bare, package-qualified, or aliased), else
 * null. Shared classifier for [resStringKey] and [resStringRClassFqn].
 */
private fun resStringReceiver(ref: KtNameReferenceExpression): KtDotQualifiedExpression? {
    val qualified = ref.parent as? KtDotQualifiedExpression ?: return null
    if (qualified.selectorExpression !== ref) return null // ref must be the trailing name
    val receiver = qualified.receiverExpression
    val typeQualified = receiver as? KtDotQualifiedExpression ?: return null
    return if (isResStringReceiverText(receiver.text) || isAliasedResStringReceiver(typeQualified)) {
        typeQualified
    } else {
        null
    }
}

/** Recognizes a bare or package-qualified `R.string`/`R.plurals` receiver by text. */
private fun isResStringReceiverText(receiver: String): Boolean =
    receiver == "R.string" || receiver == "R.plurals" ||
        receiver.endsWith(".R.string") || receiver.endsWith(".R.plurals")

/** R class FQN for a bare/package-qualified `R.string`/`R.plurals` receiver text, else null. */
private fun rClassFqnFromText(receiver: String): String? = when {
    receiver == "R.string" || receiver == "R.plurals" -> "R"
    receiver.endsWith(".R.string") -> receiver.removeSuffix(".string")
    receiver.endsWith(".R.plurals") -> receiver.removeSuffix(".plurals")
    else -> null
}

/**
 * Recognizes an aliased receiver `<alias>.string`/`<alias>.plurals` where `<alias>` stands for an
 * `R` class — covering both an import alias (`import com.app.R as appR`, resolves straight to the
 * class) and a top-level `typealias appR = com.app.R`. The head reference is resolved (this works
 * even when the aliased *member* doesn't resolve — e.g. in tests or before indexing) and its `R`
 * name checked. The bandlab-android convention is import aliases (`audiostretchCommonStringsR`) that
 * disambiguate the several per-module `R` classes.
 */
private fun isAliasedResStringReceiver(receiver: KtDotQualifiedExpression): Boolean =
    aliasedRClassFqn(receiver) != null

/** FQN of the aliased `R` class behind a `<alias>.string`/`<alias>.plurals` receiver, else null. */
private fun aliasedRClassFqn(receiver: KtDotQualifiedExpression): String? {
    val typeSelector = receiver.selectorExpression as? KtNameReferenceExpression ?: return null
    if (typeSelector.getReferencedName() !in setOf("string", "plurals")) return null
    val head = receiver.receiverExpression as? KtNameReferenceExpression ?: return null
    val rName = head.mainReference.resolve()?.resolvedRClassName() ?: return null
    return rName.takeIf { it == "R" || it.endsWith(".R") }
}

/** Name an aliased/plain receiver head stands for: a class FQN, or a type alias's target text. */
private fun PsiElement.resolvedRClassName(): String? = when (this) {
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
