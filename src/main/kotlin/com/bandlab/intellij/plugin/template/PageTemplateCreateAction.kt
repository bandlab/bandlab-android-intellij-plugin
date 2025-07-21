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
import com.intellij.ui.components.JBRadioButton
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import org.jetbrains.kotlin.idea.refactoring.psiElement
import java.awt.Dimension
import java.awt.event.InputEvent
import java.util.function.Consumer
import javax.swing.*

class PageTemplateCreateAction : CreateFileAction(
    /* text = */ "Page Template",
    /* description = */ "Create a Page template with latest convention.",
    /* icon = */ BandLabIcons.logo
) {

    private var injectionMode = InjectionMode.ContributesComponent

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
        val labelFont = JBFont.small().asItalic()

        val contributesComponent = JBRadioButton("ContributesComponent", /* selected = */ true)
        val contributesComponentLabel = JLabel("Recommended, making a Page a standalone graph.")
            .apply { font = labelFont }

        val contributesInjector = JBRadioButton("ContributesInjector")
        val contributesInjectorLabel = JLabel("Use it if you have an activity as a graph to \"host\" the page.")
            .apply { font = labelFont }

        // Group the radio buttons to make them mutually exclusive
        val buttonGroup = ButtonGroup()
        buttonGroup.add(contributesComponent)
        buttonGroup.add(contributesInjector)

        contributesComponent.addActionListener {
            if (contributesComponent.isSelected) {
                injectionMode = InjectionMode.ContributesComponent
            }
        }

        contributesInjector.addActionListener {
            if (contributesInjector.isSelected) {
                injectionMode = InjectionMode.ContributesInjector
            }
        }

        val injectionPanel = JPanel().apply {
            border = JBUI.Borders.empty(/* topAndBottom = */ 5, /* leftAndRight = */ 20)
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(JPopupMenu.Separator())
            add(
                JLabel("Choose your injection strategy:").apply {
                    border = JBUI.Borders.empty(/* topAndBottom = */ 10, /* leftAndRight = */ 0)
                    font = JBFont.medium().asBold()
                }
            )
            add(contributesComponent)
            add(contributesComponentLabel)
            add(Box.createRigidArea(Dimension(0, 10)))
            add(contributesInjector)
            add(contributesInjectorLabel)
        }

        contentPanel.add(injectionPanel)
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
        val pageBuilder = PageTemplateBuilder(
            name = newName,
            filePackage = directory.filePackage
        )
        return arrayOf(
            directory.writeFile(
                fileName = "${newName}Page.kt",
                content = when (injectionMode) {
                    InjectionMode.ContributesComponent -> pageBuilder.createPageWithContributesComponent()
                    InjectionMode.ContributesInjector -> pageBuilder.createPageWithContributesInjector()
                },
            ),
            directory.writeFile("${newName}ViewModel.kt", pageBuilder.createViewModel()),
        )
    }

    override fun hashCode(): Int = 9433

    override fun equals(other: Any?): Boolean = other is PageTemplateCreateAction

    private enum class InjectionMode {
        ContributesComponent, ContributesInjector
    }
}