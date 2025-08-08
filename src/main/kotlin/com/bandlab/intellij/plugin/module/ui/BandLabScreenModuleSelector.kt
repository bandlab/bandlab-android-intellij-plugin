package com.bandlab.intellij.plugin.module.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bandlab.intellij.plugin.module.BandLabModuleVariant
import org.jetbrains.jewel.ui.component.CheckboxRow
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField

@Composable
internal fun BandLabScreenModuleSelector(
    state: BandLabModuleVariant.Screen,
    onGenerateActivityClick: () -> Unit,
    onGeneratePageClick: () -> Unit,
    featureName: TextFieldState,
) {
    SubTitle("Grab some templates to go?")

    CheckboxRow(
        text = "Generate Activity Template",
        checked = state.generateActivityTemplate,
        onCheckedChange = { onGenerateActivityClick() }
    )

    CheckboxRow(
        text = "Generate Page Template",
        checked = state.generatePageTemplate,
        onCheckedChange = { onGeneratePageClick() }
    )

    if (state.generateActivityTemplate || state.generatePageTemplate) {
        Spacer(Modifier.height(16.dp))

        Row {
            Text(
                text = "Feature Name",
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.width(16.dp))

            Column {
                TextField(state = featureName)
                TextFieldHint("ex: UserProfile, don't include Activity or Page")
            }
        }
    }
}