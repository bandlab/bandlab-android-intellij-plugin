package com.bandlab.intellij.plugin.template

import com.bandlab.intellij.plugin.utils.filePackage
import com.bandlab.intellij.plugin.utils.writeFile
import com.intellij.ide.ui.newItemPopup.NewItemSimplePopupPanel
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.util.ui.JBUI
import javax.swing.JCheckBox

class PageTemplateCreateAction : CreateSimpleFileAction(
    text = "Page Template",
    description = "Create a Page template with latest convention.",
    inputHint = "Feature Name (Ex: UserLibrary)",
    availability = Availability.MainOnly
) {
    private var includeNavValue: Boolean = false

    override fun onContentPanelCreated(panel: NewItemSimplePopupPanel) {
        val includeNavCheckBox = JCheckBox("Generate nav key", includeNavValue).apply {
            border = JBUI.Borders.empty(0, 8)
            isOpaque = false
        }
        panel.add(includeNavCheckBox)
    }

    internal fun create(newName: String, directory: PsiDirectory, includeNav: Boolean): Array<PsiElement> {
        val pageBuilder = PageTemplateBuilder(
            name = newName,
            filePackage = directory.filePackage
        )
        val files = mutableListOf(
            directory.writeFile(
                fileName = "${newName}Page.kt",
                content = pageBuilder.createPageWithContributesComponent(),
            ),
            directory.writeFile("${newName}ViewModel.kt", pageBuilder.createViewModel()),
        )
        if (includeNav) {
            files.add(directory.writeFile("${newName}Key.kt", pageBuilder.createNavKey()))
            files.add(directory.writeFile("${newName}NavEntry.kt", pageBuilder.createNavEntry()))
        }
        return files.toTypedArray()
    }

    /**
     * Satisfies the base class contract and is used in tests via [invokeCreate].
     * In production UI flow, [invokeDialog] captures the state into [includeNavValue].
     */
    override fun create(newName: String, directory: PsiDirectory): Array<PsiElement> {
        return create(newName, directory, includeNavValue)
    }

    override fun hashCode(): Int = 9433

    override fun equals(other: Any?): Boolean = other is PageTemplateCreateAction
}