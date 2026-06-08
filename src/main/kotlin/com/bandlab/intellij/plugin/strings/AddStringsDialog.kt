package com.bandlab.intellij.plugin.strings

import com.bandlab.intellij.plugin.localizer.LocalizerConfigService.Target
import com.bandlab.intellij.plugin.localizer.parseKeyList
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Single dialog for "Add Localization Key" — pick the target `[[file]]` (pre-selected from the
 * clicked file's group, or the first base file) and paste the keys. One dialog instead of a
 * chooser + input, so the target is always explicit and visible.
 */
class AddStringsDialog(
    project: Project,
    private val targets: List<Target>,
    preselected: Target,
) : DialogWrapper(project) {

    private val targetCombo = ComboBox(targets.map { it.addKeysToFile }.toTypedArray()).apply {
        selectedIndex = targets.indexOf(preselected).coerceAtLeast(0)
    }

    private val keysArea = JBTextArea(6, 48).apply { lineWrap = true }

    init {
        title = "Add Localization Keys"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val targetRow = JPanel(BorderLayout(8, 0)).apply {
            add(JBLabel("Target file:"), BorderLayout.WEST)
            add(targetCombo, BorderLayout.CENTER)
        }
        val keysRow = JPanel(BorderLayout(0, 4)).apply {
            add(JBLabel("Keys (comma, space, or newline separated; each must already exist on Tolgee):"), BorderLayout.NORTH)
            add(JBScrollPane(keysArea), BorderLayout.CENTER)
        }
        return JPanel(BorderLayout(0, 10)).apply {
            add(targetRow, BorderLayout.NORTH)
            add(keysRow, BorderLayout.CENTER)
            preferredSize = Dimension(520, 260)
        }
    }

    override fun getPreferredFocusedComponent(): JComponent = keysArea

    val selectedTarget: Target get() = targets[targetCombo.selectedIndex]

    val keys: List<String> get() = parseKeyList(keysArea.text)
}
