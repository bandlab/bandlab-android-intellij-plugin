package com.bandlab.intellij.plugin.template

import com.bandlab.intellij.plugin.BandLabIcons
import com.bandlab.intellij.plugin.utils.filePackage
import com.bandlab.intellij.plugin.utils.writeFile
import com.intellij.ide.ui.newItemPopup.NewItemSimplePopupPanel
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.DefaultListCellRenderer
import javax.swing.JLabel
import javax.swing.JList

class PageTemplateCreateAction : CreateSimpleFileAction(
    text = "Page Template",
    description = "Create a Page template with latest convention.",
    inputHint = "Feature Name (Ex: UserLibrary)",
    availability = Availability.MainOnly
) {
    private var selectedTemplateIndex: Int = 0
    private val isIncludeNavKey: Boolean get() = selectedTemplateIndex == 1

    override fun onContentPanelCreated(panel: NewItemSimplePopupPanel) {
        val options = listOf("Page", "Page + NavKey")
        val list = JBList(options).apply {
            isFocusable = false
            isOpaque = false
            border = JBUI.Borders.empty(4, 0)
            selectedIndex = selectedTemplateIndex
            cellRenderer = object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: JList<*>?,
                    value: Any?,
                    index: Int,
                    isSelected: Boolean,
                    cellHasFocus: Boolean
                ): Component {
                    val label =
                        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
                    label.icon = BandLabIcons.logo
                    label.border = JBUI.Borders.empty(4, 8)
                    label.isOpaque = false
                    return label
                }
            }
            addListSelectionListener {
                if (selectedIndex != -1) {
                    selectedTemplateIndex = selectedIndex
                }
            }
        }
        panel.add(list)

        panel.textField.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_DOWN) {
                    list.selectedIndex = (list.selectedIndex + 1) % options.size
                    e.consume()
                } else if (e.keyCode == KeyEvent.VK_UP) {
                    list.selectedIndex = (list.selectedIndex - 1 + options.size) % options.size
                    e.consume()
                }
            }
        })
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
            directory.writeFile("${newName}ViewModel.kt", pageBuilder.createViewModel(includeNavKey = includeNav)),
        )
        if (includeNav) {
            files.add(directory.writeFile("${newName}Key.kt", pageBuilder.createNavKey()))
            files.add(directory.writeFile("${newName}NavEntry.kt", pageBuilder.createNavEntry()))
        }
        return files.toTypedArray()
    }

    override fun create(newName: String, directory: PsiDirectory): Array<PsiElement> {
        return create(newName, directory, isIncludeNavKey)
    }

    override fun hashCode(): Int = 9433

    override fun equals(other: Any?): Boolean = other is PageTemplateCreateAction
}