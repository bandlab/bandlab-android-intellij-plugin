package com.bandlab.intellij.plugin.strings

import com.bandlab.intellij.plugin.localizer.LocalizerConfigService.Target
import com.bandlab.intellij.plugin.localizer.parseKeyList
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Single dialog for "Add Localization Key" — pick the target `[[file]]` and paste the keys. One
 * dialog instead of a chooser + input, so the target is always explicit and visible.
 *
 * When [preselected] is null the user must pick a target explicitly: a placeholder entry sits at the
 * top of the combo (selected initially) and OK stays disabled until a real target is chosen. When
 * [preselected] is non-null it is preselected and there is no placeholder. [initialKeys] pre-fills
 * the keys text area (e.g. the key from an unresolved-reference quick fix).
 */
class AddStringsDialog(
    project: Project,
    private val targets: List<Target>,
    preselected: Target?,
    initialKeys: String = "",
) : DialogWrapper(project) {

    private val showPlaceholder = preselected == null

    private val targetCombo = ComboBox(comboItems().toTypedArray()).apply {
        selectedIndex = if (showPlaceholder) 0 else targets.indexOf(preselected).coerceAtLeast(0)
    }

    private val keysArea = JBTextArea(6, 48).apply {
        lineWrap = true
        text = initialKeys
    }

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

    override fun doValidate(): ValidationInfo? =
        if (showPlaceholder && targetCombo.selectedIndex == 0) {
            ValidationInfo("Select a target file", targetCombo)
        } else {
            null
        }

    override fun getPreferredFocusedComponent(): JComponent = keysArea

    val selectedTarget: Target
        get() = targets[targetCombo.selectedIndex - placeholderOffset]

    val keys: List<String> get() = parseKeyList(keysArea.text)

    private val placeholderOffset: Int get() = if (showPlaceholder) 1 else 0

    private fun comboItems(): List<String> {
        val labels = targets.map { it.addKeysToFile }
        return if (showPlaceholder) listOf(PLACEHOLDER) + labels else labels
    }

    private companion object {
        const val PLACEHOLDER = "— Select target file —"
    }
}
