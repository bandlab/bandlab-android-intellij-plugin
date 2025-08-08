package com.bandlab.intellij.plugin.module.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bandlab.intellij.plugin.module.*
import com.bandlab.intellij.plugin.module.ModuleValidationError.Companion.errorMessageOrNull
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
    val onVariantClick: (BandLabModuleVariant) -> Unit,
    val onModuleTypeClick: (BandLabModuleVariant, BandLabModuleType) -> Unit,
    val onPluginClick: (BandLabModuleVariant, ModulePlugin) -> Unit,
    val onExposureClick: (BandLabModuleVariant, ModuleExposure) -> Unit,
    val onGenerateActivityClick: () -> Unit,
    val onGeneratePageClick: () -> Unit,
    val featureName: TextFieldState,
    val validationErrors: StateFlow<Set<ModuleValidationError>>,
)

@Composable
internal fun BandLabModuleWizard(state: WizardState) {
    val validationErrors by state.validationErrors.collectAsState()

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
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
                    outline = Outline.of(
                        warning = false,
                        error = validationErrors.any { it.isNameError }
                    )
                )

                val nameError = validationErrors.firstOrNull { it.isNameError }
                if (nameError != null) {
                    ErrorText(
                        errorMessage = nameError.errorMessage,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                HintText("ex :user:profile")
            }
        }

        SubTitle("Select the module variants you need")

        val uriHandler = LocalUriHandler.current
        Row(
            modifier = Modifier.clickable {
                uriHandler.openUri(MODULE_STRUCTURE_CONVENTION_URL)
            }
        ) {
            HintText(hint = "See the convention doc")

            Icon(
                key = AllIconsKeys.Ide.External_link_arrow,
                contentDescription = "Link",
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        listOf(
            state.apiVariant,
            state.implVariant,
            state.screenVariant,
            state.uiVariant
        )
            .forEach { variantState ->
                val variant = variantState.collectAsState().value
                val errorMessage = validationErrors.errorMessageOrNull(
                    variant = variant,
                    parentModule = state.featureName.text
                )

                BandLabModuleVariantSelector(
                    state = variant,
                    onVariantClick = state.onVariantClick,
                    onModuleTypeClick = state.onModuleTypeClick,
                    onPluginClick = state.onPluginClick,
                    onExposureClick = state.onExposureClick,
                    screenSettingsSlot = if (variant is BandLabModuleVariant.Screen) {
                        {
                            BandLabScreenModuleSelector(
                                state = variant,
                                onGenerateActivityClick = state.onGenerateActivityClick,
                                onGeneratePageClick = state.onGeneratePageClick,
                                featureName = state.featureName
                            )
                        }
                    } else {
                        null
                    },
                    errorMessage = errorMessage
                )
            }
    }
}

private const val MODULE_STRUCTURE_CONVENTION_URL =
    "https://bandlab.atlassian.net/wiki/spaces/Android/pages/3319365634/Module+structure+convention"