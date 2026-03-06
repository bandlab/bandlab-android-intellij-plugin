package com.bandlab.intellij.plugin.module.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bandlab.intellij.plugin.module.BandLabModuleConfig
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.jewel.ui.component.RadioButtonRow
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField

@Composable
internal fun BandLabScreenModuleSelector(
    state: BandLabModuleConfig.Screen,
    onGenerateActivityClick: () -> Unit,
    onGeneratePageClick: () -> Unit,
    featureName: TextFieldState,
) {
    SettingsGroup("Grab a screen template to go?") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RadioButtonRow(
                text = "Generate Activity Template",
                selected = state.template == BandLabModuleConfig.Screen.Template.Activity,
                onClick = { onGenerateActivityClick() }
            )

            RadioButtonRow(
                text = "Generate Page Template",
                selected = state.template == BandLabModuleConfig.Screen.Template.Page,
                onClick = { onGeneratePageClick() }
            )
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
}

@Preview
@Composable
private fun PreviewBandLabScreenModuleSelector() {
    BandLabScreenModuleSelector(
        state = BandLabModuleConfig.Screen(),
        onGenerateActivityClick = {},
        onGeneratePageClick = {},
        featureName = TextFieldState()
    )
}