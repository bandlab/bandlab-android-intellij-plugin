package com.bandlab.intellij.plugin.strings

import com.bandlab.intellij.plugin.localizer.LocalizerConfigService
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import javax.swing.Icon

/**
 * Alt+Enter intention on a `<string>` / `<plurals>` element in a manifest string file: deletes that
 * one string and all its translations via `update-strings --delete-keys`. Available from any caret
 * position inside the element (tag, `name` attribute, or text) — it resolves the enclosing keyed
 * element. PSI-only, so it works regardless of Gradle sync.
 */
class DeleteStringIntention : IntentionAction, Iconable, PriorityAction {

    private var key: String? = null

    // TOP, matching Update, so the two sit together at the top of the ⌥⏎ list rather than being
    // split by other TOP-priority actions (e.g. Android's "Open editor"). Same priority tie-breaks
    // alphabetically, so Delete lists just above Update — an accepted trade for keeping them grouped.
    override fun getPriority(): PriorityAction.Priority = PriorityAction.Priority.TOP

    override fun getIcon(flags: Int): Icon = AllIcons.General.Remove

    override fun getFamilyName(): String = "Localizer: Delete String"

    override fun getText(): String = key?.let { "Localizer: Delete \"$it\"" } ?: familyName

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
        LocalizerOps.deleteKey(project, key)
    }
}

/**
 * Name of the `<string>`/`<plurals>` element enclosing [offset] in [psiFile], or null. Resolves
 * from any caret position inside the element — tag name, `name` attribute, or text content — by
 * walking up to the nearest keyed element. The `offset - 1` fallback covers a caret sitting right
 * at the element's trailing boundary.
 */
internal fun stringKeyAt(psiFile: PsiFile, offset: Int): String? {
    val element = psiFile.findElementAt(offset)
        ?: (if (offset > 0) psiFile.findElementAt(offset - 1) else null)
        ?: return null
    val tag = generateSequence(PsiTreeUtil.getParentOfType(element, XmlTag::class.java, false)) { it.parentTag }
        .firstOrNull { it.name == "string" || it.name == "plurals" } ?: return null
    return tag.getAttributeValue("name")?.takeIf { it.isNotBlank() }
}
