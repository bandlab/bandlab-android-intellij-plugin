package com.bandlab.intellij.plugin.module.ui

import androidx.compose.animation.animateContentSize
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.jewel.ui.Outline
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@Stable
internal data class WizardState(
    val moduleRoot: TextFieldState,
    val apiConfig: StateFlow<BandLabModuleConfig.Api>,
    val implConfig: StateFlow<BandLabModuleConfig.Impl>,
    val uiConfig: StateFlow<BandLabModuleConfig.Ui>,
    val screenConfig: StateFlow<BandLabModuleConfig.Screen>,
    val onConfigClick: (BandLabModuleConfig) -> Unit,
    val onModuleTypeClick: (BandLabModuleConfig, BandLabModuleType) -> Unit,
    val onPluginClick: (BandLabModuleConfig, ModulePlugin) -> Unit,
    val onExposureClick: (BandLabModuleConfig, ModuleExposure) -> Unit,
    val onGenerateActivityClick: () -> Unit,
    val onGeneratePageClick: () -> Unit,
    val featureName: TextFieldState,
    val existingModuleNames: Flow<Set<String>>,
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
                text = "Module Root",
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.animateContentSize()
            ) {
                AutoCompleteTextField(
                    state = state.moduleRoot,
                    suggestionsFlow = state.existingModuleNames,
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
            state.apiConfig,
            state.implConfig,
            state.screenConfig,
            state.uiConfig
        )
            .forEach { configState ->
                val config = configState.collectAsState().value
                val errorMessage = validationErrors.errorMessageOrNull(
                    config = config,
                    parentModule = state.moduleRoot.text
                )

                BandLabModuleConfigSelector(
                    state = config,
                    onConfigClick = state.onConfigClick,
                    onModuleTypeClick = state.onModuleTypeClick,
                    onPluginClick = state.onPluginClick,
                    onExposureClick = state.onExposureClick,
                    screenSettingsSlot = if (config is BandLabModuleConfig.Screen) {
                        {
                            BandLabScreenModuleSelector(
                                state = config,
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