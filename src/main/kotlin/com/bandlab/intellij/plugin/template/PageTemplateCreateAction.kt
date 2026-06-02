package com.bandlab.intellij.plugin.template

import com.bandlab.intellij.plugin.BandLabIcons
import com.bandlab.intellij.plugin.utils.filePackage
import com.bandlab.intellij.plugin.utils.writeFile
import com.intellij.ide.ui.newItemPopup.NewItemSimplePopupPanel
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
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
            cellRenderer = object : ColoredListCellRenderer<String>() {
                override fun customizeCellRenderer(
                    list: JList<out String>,
                    value: String?,
                    index: Int,
                    selected: Boolean,
                    hasFocus: Boolean
                ) {
                    append(value.orEmpty())
                    icon = BandLabIcons.logo
                    border = JBUI.Borders.empty(4, 4)
                    isOpaque = selected
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
            filePackage = directory.filePackage,
            includeNavKey = includeNav,
        )
        val files = mutableListOf(
            directory.writeFile(
                fileName = "${newName}Page.kt",
                content = pageBuilder.createPage(),
            ),
            directory.writeFile("${newName}ViewModel.kt", pageBuilder.createViewModel()),
        )
        if (includeNav) {
            val navKeyBuilder = NavKeyTemplateBuilder(
                name = newName,
                filePackage = directory.filePackage,
            )

            files.add(directory.writeFile("${newName}Key.kt", navKeyBuilder.createNavKey()))
            files.add(directory.writeFile("${newName}NavEntry.kt", navKeyBuilder.createNavEntry()))
        }
        return files.toTypedArray()
    }

    override fun create(newName: String, directory: PsiDirectory): Array<PsiElement> {
        return create(newName, directory, isIncludeNavKey)
    }

    override fun hashCode(): Int = 9433

    override fun equals(other: Any?): Boolean = other is PageTemplateCreateAction
}