package com.bandlab.intellij.plugin.module.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.enableNewSwingCompositing
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.theme.linkStyle
import javax.swing.Action
import javax.swing.JComponent

internal class BandLabModuleFollowUpActionsDialog(
    project: Project,
    private val viewModel: BandLabModuleFollowUpActionsViewModel
) : DialogWrapper(project) {

    init {
        // Set the dialog to be non-modal
        isModal = true
        title = "Recommended Actions"
        init()
    }

    override fun createCenterPanel(): JComponent {
        @OptIn(ExperimentalJewelApi::class)
        enableNewSwingCompositing()

        return JewelComposePanel {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                viewModel.actions.forEach { action ->
                    Text(
                        text = action.text,
                        modifier = Modifier.clickable(onClick = action.onClick),
                        style = TextStyle(
                            // Use the official theme color for links. This adapts to light/dark themes automatically.
                            color = JewelTheme.linkStyle.colors.content,
                            // Add an underline, which is standard for links.
                            textDecoration = TextDecoration.Underline
                        )
                    )
                }
            }
        }
    }

    /**
     * Show only OK button
     */
    override fun createActions(): Array<Action> = arrayOf(okAction)
}