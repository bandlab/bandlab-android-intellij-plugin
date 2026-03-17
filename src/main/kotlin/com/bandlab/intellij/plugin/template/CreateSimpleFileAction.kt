package com.bandlab.intellij.plugin.template

import com.bandlab.intellij.plugin.BandLabIcons
import com.bandlab.intellij.plugin.utils.resolvePath
import com.intellij.ide.actions.CreateFileAction
import com.intellij.ide.ui.newItemPopup.NewItemPopupUtil
import com.intellij.ide.ui.newItemPopup.NewItemSimplePopupPanel
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.refactoring.psiElement
import java.awt.event.InputEvent
import java.util.function.Consumer

abstract class CreateSimpleFileAction(
    text: String,
    description: String,
    private val inputHint: String,
    private val availability: Availability
) : CreateFileAction(
    { text },
    { description },
    { BandLabIcons.logo },
) {
    /**
     * Make the action only available for main source set.
     */
    override fun isAvailable(dataContext: DataContext): Boolean {
        return when (availability) {
            Availability.Always -> true
            Availability.MainOnly -> {
                val targetPath = dataContext.psiElement?.resolvePath() ?: return false
                targetPath.contains("/src/main/")
            }
        }
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
        val popup = NewItemPopupUtil.createNewItemPopup(inputHint, contentPanel, nameField)

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

    enum class Availability {
        Always, MainOnly
    }
}