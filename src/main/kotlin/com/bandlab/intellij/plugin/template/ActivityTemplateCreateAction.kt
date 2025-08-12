package com.bandlab.intellij.plugin.template

import com.bandlab.intellij.plugin.BandLabIcons
import com.bandlab.intellij.plugin.utils.filePackage
import com.bandlab.intellij.plugin.utils.resolvePath
import com.bandlab.intellij.plugin.utils.writeFile
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

class ActivityTemplateCreateAction : CreateFileAction(
    { "Activity Template" },
    { "Create an activity template with latest convention." },
    { BandLabIcons.logo },
) {

    /**
     * Make the action only available for main source set.
     */
    override fun isAvailable(dataContext: DataContext): Boolean {
        if (!super.isAvailable(dataContext)) return false

        val targetPath = dataContext.psiElement?.resolvePath() ?: return false
        return targetPath.contains("main/")
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
        val activityBuilder = ActivityTemplateBuilder(
            name = newName,
            filePackage = directory.filePackage
        )
        return arrayOf(
            directory.writeFile("${newName}Activity.kt", activityBuilder.createActivity()),
            directory.writeFile("${newName}ViewModel.kt", activityBuilder.createViewModel()),
            // Known issue, if AndroidManifest exist already, the plugin cannot create a new instance, you'll see
            // the error and will need to add the declaration manually. I don't think we need to support this case,
            // having multiple activities in the same module is generally discouraged.
            directory.requireMainDir().writeFile("AndroidManifest.xml", activityBuilder.createManifest()),
        )
    }

    /**
     * Go through the parents of the [PsiDirectory] and find the main directory.
     */
    private fun PsiDirectory.requireMainDir(): PsiDirectory {
        var parentDir = parentDirectory
        while (parentDir != null) {
            if (parentDir.resolvePath()?.endsWith("main") == true) {
                return parentDir
            } else {
                parentDir = parentDir.parentDirectory
            }
        }
        error("Cannot find main dir under $this")
    }

    override fun hashCode(): Int = 9432

    override fun equals(other: Any?): Boolean = other is ActivityTemplateCreateAction

}