package com.bandlab.intellij.plugin.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.awt.ComposePanel
import org.jetbrains.jewel.bridge.theme.SwingBridgeTheme
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import javax.swing.JLayeredPane

@Suppress("FunctionName")
internal fun ComposePanelWithSwingBridgeTheme(
    content: @Composable () -> Unit
): JLayeredPane {
    return ComposePanel().apply {
        setContent {
            @OptIn(ExperimentalJewelApi::class)
            @Suppress("UnstableApiUsage")
            SwingBridgeTheme {
                content()
            }
        }
    }
}