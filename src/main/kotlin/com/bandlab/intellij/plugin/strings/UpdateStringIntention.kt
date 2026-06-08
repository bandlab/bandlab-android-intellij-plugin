package com.bandlab.intellij.plugin.strings

import com.bandlab.intellij.plugin.localizer.LocalizerConfigService
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

/**
 * Alt+Enter intention on a `<string>` / `<plurals>` element in a manifest string file: re-fetches
 * that one string fresh from Tolgee and overwrites it in place (base + every translation), via
 * `update-strings --update-keys`. Available from any caret position inside the element. PSI-only,
 * so it works regardless of Gradle sync.
 */
class UpdateStringIntention : IntentionAction {

    private var key: String? = null

    override fun getFamilyName(): String = "Localizer: Update String"

    override fun getText(): String = key?.let { "Localizer: Update String \"$it\"" } ?: familyName

    override fun startInWriteAction(): Boolean = false

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        key = null
        if (editor == null || file == null) return false
        val virtualFile = file.virtualFile ?: return false
        if (!project.service<LocalizerConfigService>().isManagedStringFile(virtualFile)) return false
        key = stringKeyAt(file, editor.caretModel.offset)
        return key != null
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        val key = stringKeyAt(file, editor.caretModel.offset) ?: return
        LocalizerOps.updateKey(project, key)
    }
}
