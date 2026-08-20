// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.strings

import com.bandlab.intellij.plugin.localizer.parseKeyList
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * "Update Strings" scope dialog — choose **All strings** (full sync) or **Selected strings**
 * (re-fetch just the listed keys via `--update-keys`). Makes the intent of an Update explicit up
 * front rather than always re-pulling everything.
 */
class UpdateStringsDialog(project: Project) : DialogWrapper(project) {

    private val allButton = JBRadioButton("All strings", true)
    private val selectedButton = JBRadioButton("Selected strings")
    private val keysArea =
        JBTextArea(6, 48).apply {
            lineWrap = true
            isEnabled = false
        }

    init {
        ButtonGroup().apply {
            add(allButton)
            add(selectedButton)
        }
        val syncEnabled = { keysArea.isEnabled = selectedButton.isSelected }
        allButton.addActionListener { syncEnabled() }
        selectedButton.addActionListener { syncEnabled() }
        title = "Update Strings"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val scope =
            JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(allButton)
                add(selectedButton)
            }
        val keys =
            JPanel(BorderLayout(0, 4)).apply {
                add(
                    JBLabel("Keys to update (comma, space, or newline separated):"),
                    BorderLayout.NORTH,
                )
                add(JBScrollPane(keysArea), BorderLayout.CENTER)
            }
        return JPanel(BorderLayout(0, 10)).apply {
            add(scope, BorderLayout.NORTH)
            add(keys, BorderLayout.CENTER)
            preferredSize = Dimension(480, 240)
        }
    }

    val allStrings: Boolean
        get() = allButton.isSelected

    val keys: List<String>
        get() = parseKeyList(keysArea.text)
}
