package com.bandlab.intellij.plugin.module.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.ui.component.Text

@Stable
internal data class WizardState(
    val a: Boolean,
)

@Composable
internal fun BandLabModuleWizard(state: WizardState) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            "Hello CMP!",
            fontSize = 48.sp
        )
    }
}


