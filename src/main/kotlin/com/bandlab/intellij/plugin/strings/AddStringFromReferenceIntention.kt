package com.bandlab.intellij.plugin.strings

import com.bandlab.intellij.plugin.localizer.LocalizerConfigService
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import kotlin.io.path.readText
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

/**
 * Alt+Enter intention on the name part of an Android resource reference (`R.string.X` /
 * `R.plurals.X`) in Kotlin code: pulls `X` from Tolgee into the project's base files via
 * `update-strings --add-keys`. Available only when `X` is not yet defined locally — once the key
 * exists in a base file the intention disappears. PSI-only, so it works regardless of Gradle sync.
 */
class AddStringFromReferenceIntention : IntentionAction {

    private var key: String? = null

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
 * Referenced name of an Android string/plurals resource reference (`R.string.X` / `R.plurals.X`,
 * or a package-qualified `com.app.R.string.X`) when [offset] sits on the trailing name part, else
 * null. The `offset - 1` fallback covers a caret sitting right at the name's trailing boundary.
 */
internal fun resStringKeyAt(psiFile: PsiFile, offset: Int): String? {
    val element = psiFile.findElementAt(offset) ?: psiFile.findElementAt(offset - 1) ?: return null
    val ref = PsiTreeUtil.getParentOfType(element, KtNameReferenceExpression::class.java, false) ?: return null
    val qualified = ref.parent as? KtDotQualifiedExpression ?: return null
    if (qualified.selectorExpression !== ref) return null // caret must be on the trailing name
    val receiver = qualified.receiverExpression.text
    return if (
        receiver == "R.string" || receiver == "R.plurals" ||
        receiver.endsWith(".R.string") || receiver.endsWith(".R.plurals")
    ) {
        ref.getReferencedName()
    } else {
        null
    }
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
