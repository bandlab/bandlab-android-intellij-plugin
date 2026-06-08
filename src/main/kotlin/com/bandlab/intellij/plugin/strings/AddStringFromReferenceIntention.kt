package com.bandlab.intellij.plugin.strings

import com.bandlab.intellij.plugin.localizer.LocalizerConfigService
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import javax.swing.Icon
import kotlin.io.path.readText
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtTypeAlias

/**
 * Alt+Enter intention on the name part of an Android resource reference (`R.string.X` /
 * `R.plurals.X`) in Kotlin code: pulls `X` from Tolgee into the project's base files via
 * `update-strings --add-keys`. Available only when `X` is not yet defined locally — once the key
 * exists in a base file the intention disappears. PSI-only, so it works regardless of Gradle sync.
 */
class AddStringFromReferenceIntention : IntentionAction, Iconable {

    private var key: String? = null

    override fun getIcon(flags: Int): Icon = AllIcons.General.Add

    override fun getFamilyName(): String = "Localizer: Add string"

    override fun getText(): String = key?.let { "Localizer: Add string \"$it\"" } ?: familyName

    override fun startInWriteAction(): Boolean = false

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        key = null
        if (editor == null || file == null) return false
        if (!project.service<LocalizerConfigService>().isConfigured()) return false
        val k = resStringKeyAt(file, editor.caretModel.offset) ?: return false
        if (k in localBaseKeys(project)) return false
        key = k
        return true
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        val k = resStringKeyAt(file, editor.caretModel.offset) ?: return
        LocalizerOps.addKey(project, k)
    }
}

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
    val element = psiFile.findElementAt(offset) ?: psiFile.findElementAt(offset - 1) ?: return null
    val ref = PsiTreeUtil.getParentOfType(element, KtNameReferenceExpression::class.java, false) ?: return null
    return resStringKey(ref)
}

/** Resource key of [ref] when it is the trailing name of an `R.string.X`/`R.plurals.X` reference. */
internal fun resStringKey(ref: KtNameReferenceExpression): String? {
    val qualified = ref.parent as? KtDotQualifiedExpression ?: return null
    if (qualified.selectorExpression !== ref) return null // ref must be the trailing name
    val receiver = qualified.receiverExpression
    return if (isResStringReceiverText(receiver.text) || isAliasedResStringReceiver(receiver)) {
        ref.getReferencedName()
    } else {
        null
    }
}

/** Recognizes a bare or package-qualified `R.string`/`R.plurals` receiver by text. */
private fun isResStringReceiverText(receiver: String): Boolean =
    receiver == "R.string" || receiver == "R.plurals" ||
        receiver.endsWith(".R.string") || receiver.endsWith(".R.plurals")

/**
 * Recognizes an aliased receiver `<alias>.string`/`<alias>.plurals` where `<alias>` stands for an
 * `R` class — covering both an import alias (`import com.app.R as appR`, resolves straight to the
 * class) and a top-level `typealias appR = com.app.R`. The head reference is resolved (this works
 * even when the aliased *member* doesn't resolve — e.g. in tests or before indexing) and its `R`
 * name checked. The bandlab-android convention is import aliases (`audiostretchCommonStringsR`) that
 * disambiguate the several per-module `R` classes.
 */
private fun isAliasedResStringReceiver(receiver: KtExpression): Boolean {
    val typeQualified = receiver as? KtDotQualifiedExpression ?: return false
    val typeSelector = typeQualified.selectorExpression as? KtNameReferenceExpression ?: return false
    if (typeSelector.getReferencedName() !in setOf("string", "plurals")) return false
    val head = typeQualified.receiverExpression as? KtNameReferenceExpression ?: return false
    val rName = head.mainReference.resolve()?.resolvedRClassName() ?: return false
    return rName == "R" || rName.endsWith(".R")
}

/** Name an aliased/plain receiver head stands for: a class FQN, or a type alias's target text. */
private fun PsiElement.resolvedRClassName(): String? = when (this) {
    is KtTypeAlias -> getTypeReference()?.text
    is PsiClass -> qualifiedName
    is KtClassOrObject -> fqName?.asString()
    else -> null
}

/** Every `<string>`/`<plurals>` key already defined across the manifest's base files. */
internal fun localBaseKeys(project: Project): Set<String> {
    val regex = Regex("<(?:string|plurals)\\s+name=\"([^\"]+)\"")
    return project.service<LocalizerConfigService>().targets()
        .flatMap { target ->
            runCatching { target.baseFile.readText() }.getOrDefault("")
                .let { text -> regex.findAll(text).map { it.groupValues[1] }.toList() }
        }
        .toSet()
}
