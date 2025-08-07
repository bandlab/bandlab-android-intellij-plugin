package com.bandlab.intellij.plugin.module.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bandlab.intellij.plugin.module.BandLabModuleVariant
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.jewel.ui.Outline
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@Stable
internal data class WizardState(
    val moduleName: TextFieldState,
    val apiVariant: StateFlow<BandLabModuleVariant.Api>,
    val implVariant: StateFlow<BandLabModuleVariant.Impl>,
    val uiVariant: StateFlow<BandLabModuleVariant.Ui>,
    val screenVariant: StateFlow<BandLabModuleVariant.Screen>,
    val onVariantClick: (BandLabModuleVariant) -> Unit
)

@Composable
internal fun BandLabModuleWizard(state: WizardState) {
    Column(
        modifier = Modifier.padding(24.dp)
    ) {
        Text(
            text = "BandLab Module Structure Convention",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        Row {
            Text(
                text = "Module Name",
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.width(16.dp))

            Column {
                TextField(
                    state = state.moduleName,
                    trailingIcon = null,
                    outline = Outline.of(warning = false, error = true)
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "ex :user:profile",
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Select the module variants you need",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(4.dp))

        val uriHandler = LocalUriHandler.current
        Row(
            modifier = Modifier.clickable {
                uriHandler.openUri(MODULE_STRUCTURE_CONVENTION_URL)
            }
        ) {
            Text(
                text = "See the convention doc",
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic
            )

            Icon(
                key = AllIconsKeys.Ide.External_link_arrow,
                contentDescription = "Link",
            )
        }

        Spacer(Modifier.height(4.dp))

        BandLabModuleVariantSelector(
            state = state.apiVariant.collectAsState().value,
            onClick = state.onVariantClick
        )

        BandLabModuleVariantSelector(
            state = state.implVariant.collectAsState().value,
            onClick = state.onVariantClick
        )

        BandLabModuleVariantSelector(
            state = state.screenVariant.collectAsState().value,
            onClick = state.onVariantClick
        )

        BandLabModuleVariantSelector(
            state = state.uiVariant.collectAsState().value,
            onClick = state.onVariantClick
        )
    }
}

private const val MODULE_STRUCTURE_CONVENTION_URL =
    "https://bandlab.atlassian.net/wiki/spaces/Android/pages/3319365634/Module+structure+convention"