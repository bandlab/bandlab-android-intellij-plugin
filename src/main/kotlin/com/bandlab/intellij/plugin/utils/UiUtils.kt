// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.awt.ComposePanel
import javax.swing.JLayeredPane
import org.jetbrains.jewel.bridge.theme.SwingBridgeTheme
import org.jetbrains.jewel.foundation.ExperimentalJewelApi

@Suppress("FunctionName")
internal fun ComposePanelWithSwingBridgeTheme(content: @Composable () -> Unit): JLayeredPane {
    return ComposePanel().apply {
        setContent {
            @OptIn(ExperimentalJewelApi::class) @Suppress("UnstableApiUsage")
            SwingBridgeTheme {
                content()
            }
        }
    }
}
