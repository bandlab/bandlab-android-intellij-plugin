package com.bandlab.intellij.plugin.automation

import com.bandlab.intellij.plugin.BandLabIcons
import com.bandlab.intellij.plugin.utils.resolvePath
import com.intellij.ide.actions.CreateFileAction
import com.intellij.ide.ui.newItemPopup.NewItemPopupUtil
import com.intellij.ide.ui.newItemPopup.NewItemSimplePopupPanel
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.refactoring.psiElement
import java.awt.event.InputEvent
import java.util.function.Consumer

class AutomationTemplateCreateAction : CreateFileAction(
    { "Automation Template" },
    { "Create template files for automation testing." },
    { BandLabIcons.logo },
), DumbAware {

    /**
     * Make the action only available for androidTest source set.
     */
    override fun isAvailable(dataContext: DataContext): Boolean {
        if (!super.isAvailable(dataContext)) return false

        val targetPath = dataContext.psiElement?.resolvePath() ?: return false
        return targetPath.contains("androidTest/")
    }

    // There is no way to override the "New File" text from the file creation popup,
    // so copied the code from the parent and override the popup title.
    override fun invokeDialog(
        project: Project,
        directory: PsiDirectory,
        elementsConsumer: Consumer<in Array<PsiElement>>
    ) {
        val validator = MyValidator(project, directory)
        val contentPanel = NewItemSimplePopupPanel()
        val nameField = contentPanel.textField
        val popup = NewItemPopupUtil.createNewItemPopup("Feature Name (Ex: UserLibrary)", contentPanel, nameField)

        contentPanel.applyAction = com.intellij.util.Consumer { event: InputEvent? ->
            val name = nameField.text
            if (validator.checkInput(name) && validator.canClose(name)) {
                popup.closeOk(event)
                elementsConsumer.accept(validator.createdElements)
            } else {
                val errorMessage = validator.getErrorText(name)
                contentPanel.setError(errorMessage)
            }
        }

        popup.showCenteredInCurrentWindow(project)
    }

    override fun create(newName: String, directory: PsiDirectory): Array<PsiElement> {
        val templateBuilder = AutomationTemplateBuilder(newName, directory)
        return arrayOf(
            templateBuilder.createRobot(),
            templateBuilder.createSemantics(),
            templateBuilder.createVerifier(),
        )
    }

    override fun hashCode(): Int = 3452

    override fun equals(other: Any?): Boolean = other is AutomationTemplateCreateAction

}