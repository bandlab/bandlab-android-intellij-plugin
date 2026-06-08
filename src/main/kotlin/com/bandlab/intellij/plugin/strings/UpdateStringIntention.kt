package com.bandlab.intellij.plugin.strings

import com.bandlab.intellij.plugin.localizer.LocalizerConfigService
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

/**
 * Alt+Enter intention to re-fetch a string fresh from Tolgee and overwrite it in place (base + every
 * translation), via `update-strings --update-keys`. Available from two caret positions:
 * - on a `<string>`/`<plurals>` element in a manifest string file, and
 * - on an `R.string.X` / `R.plurals.X` reference whose key **is** already defined locally.
 *
 * (Mirror of [AddStringFromReferenceIntention], which fires on a reference whose key is *not* local.)
 * PSI-only, so it works regardless of Gradle sync.
 */
class UpdateStringIntention : IntentionAction {

    private var key: String? = null

    override fun getFamilyName(): String = "Localizer: Update String"

    override fun getText(): String = key?.let { "Localizer: Update \"$it\"" } ?: familyName

    override fun startInWriteAction(): Boolean = false

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        key = null
        if (editor == null || file == null) return false
        if (!project.service<LocalizerConfigService>().isConfigured()) return false
        key = managedKeyAt(project, file, editor.caretModel.offset)
        return key != null
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) return
        val target = managedKeyAt(project, file, editor.caretModel.offset) ?: return
        LocalizerOps.updateKey(project, target)
    }
}

/**
 * The managed string key at [offset]: the enclosing `<string>`/`<plurals>` key when [file] is a
 * manifest string file, otherwise the `R.string.X`/`R.plurals.X` key under the caret when that key
 * is already defined in a base file. Null when neither applies.
 */
internal fun managedKeyAt(project: Project, file: PsiFile, offset: Int): String? {
    val service = project.service<LocalizerConfigService>()
    val virtualFile = file.virtualFile
    if (virtualFile != null && service.isManagedStringFile(virtualFile)) {
        return stringKeyAt(file, offset)
    }
    return resStringKeyAt(file, offset)?.takeIf { it in localBaseKeys(project) }
}
