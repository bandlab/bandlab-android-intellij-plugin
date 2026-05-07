package com.bandlab.intellij.plugin.template

import com.bandlab.intellij.plugin.utils.filePackage
import com.bandlab.intellij.plugin.utils.writeFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.ui.dsl.builder.panel
import java.util.function.Consumer
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JTextField

class PageTemplateCreateAction : CreateSimpleFileAction(
    text = "Page Template",
    description = "Create a Page template with latest convention.",
    inputHint = "Feature Name (Ex: UserLibrary)",
    availability = Availability.MainOnly
) {
    override fun invokeDialog(
        project: Project,
        directory: PsiDirectory,
        elementsConsumer: Consumer<in Array<PsiElement>>
    ) {
        val dialog = PageTemplateDialog(project)
        if (dialog.showAndGet()) {
            val name = dialog.getName()
            val includeNav = dialog.includeNav()
            val elements = create(name, directory, includeNav)
            elementsConsumer.accept(elements)
        }
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
            files.add(directory.writeFile("${newName}NavKey.kt", pageBuilder.createNavKey()))
            files.add(directory.writeFile("${newName}NavEntry.kt", pageBuilder.createNavEntry()))
        }
        return files.toTypedArray()
    }

    /**
     * Satisfies the base class contract and is used in tests via [invokeCreate].
     * In production UI flow, [invokeDialog] is fully overridden and captures the name from the dialog's own field.
     */
    override fun create(newName: String, directory: PsiDirectory): Array<PsiElement> {
        return create(newName, directory, includeNav = false)
    }

    override fun hashCode(): Int = 9433

    override fun equals(other: Any?): Boolean = other is PageTemplateCreateAction

    private class PageTemplateDialog(project: Project) : DialogWrapper(project) {
        private val nameField = JTextField()
        private val navCheckBox = JCheckBox("Include GlobalPageNavKey and GlobalPageNavEntry")

        init {
            title = "Page Template"
            init()
        }

        override fun createCenterPanel(): JComponent {
            return panel {
                row("Feature Name:") {
                    cell(nameField)
                        .focused()
                        .validationOnApply {
                            if (it.text.isNullOrBlank()) {
                                error("Feature name is required")
                            } else {
                                null
                            }
                        }
                }
                row {
                    cell(navCheckBox)
                }
            }
        }

        fun getName(): String = nameField.text
        fun includeNav(): Boolean = navCheckBox.isSelected
    }
}
