package com.bandlab.intellij.plugin.module.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.bandlab.intellij.plugin.module.BandLabModuleConfig
import com.bandlab.intellij.plugin.module.BandLabModuleConfig.Screen.Template
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.ui.component.RadioButtonRow

@Composable
internal fun BandLabScreenModuleSelector(
    state: BandLabModuleConfig.Screen,
    onTemplateSelection: (Template) -> Unit,
) {
    SettingsGroup("Grab a screen template to go?") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RadioButtonRow(
                text = "Page",
                selected = state.template == Template.Page,
                onClick = { onTemplateSelection(Template.Page) }
            )
            RadioButtonRow(
                text = "Page + Nav Key",
                selected = state.template == Template.PageWithNavKey,
                onClick = { onTemplateSelection(Template.PageWithNavKey) }
            )
        }
    }
}

@Preview
@Composable
private fun PreviewBandLabScreenModuleSelector() {
    BandLabScreenModuleSelector(
        state = BandLabModuleConfig.Screen(),
        onTemplateSelection = {},
    )
}