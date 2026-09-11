// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.module.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bandlab.intellij.plugin.module.BandLabModuleConfig
import com.bandlab.intellij.plugin.module.BandLabModuleConfig.Screen.Template
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.ui.component.RadioButtonRow
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField

@Composable
internal fun BandLabScreenModuleSelector(
    state: BandLabModuleConfig.Screen,
    featureName: TextFieldState,
    onTemplateSelection: (Template) -> Unit,
) {
    SettingsGroup("Grab a screen template to go?") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RadioButtonRow(
                text = "Page",
                selected = state.template == Template.Page,
                onClick = { onTemplateSelection(Template.Page) },
            )
            RadioButtonRow(
                text = "Page + Nav Key",
                selected = state.template == Template.PageWithNavKey,
                onClick = { onTemplateSelection(Template.PageWithNavKey) },
            )
        }
    }

    if (state.template != null) {
        Column(modifier = Modifier.padding(start = 32.dp, top = 16.dp)) {
            Text(text = "Feature Name")

            Spacer(Modifier.height(4.dp))

            TextField(state = featureName)

            HintText("ex: UserProfile, don't include Activity or Page")
        }
    }
}

@Preview
@Composable
private fun PreviewBandLabScreenModuleSelector() {
    BandLabScreenModuleSelector(
        state = BandLabModuleConfig.Screen(),
        featureName = TextFieldState("Preview"),
        onTemplateSelection = {},
    )
}
