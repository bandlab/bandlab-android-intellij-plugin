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
                onClick = { onTemplateSelection(Template.Page) }
            )
            RadioButtonRow(
                text = "Page + Nav Key",
                selected = state.template == Template.PageWithNavKey,
                onClick = { onTemplateSelection(Template.PageWithNavKey) }
            )
        }
    }

    if (state.template != null) {
        Spacer(Modifier.height(16.dp))

        Row {
            Text(
                text = "Feature Name",
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.width(16.dp))

            Column {
                TextField(state = featureName)
                HintText("ex: UserProfile, don't include Activity or Page")
            }
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