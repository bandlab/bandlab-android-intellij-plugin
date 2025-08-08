package com.bandlab.intellij.plugin.module

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import com.android.tools.idea.observable.core.BoolValueProperty
import com.bandlab.intellij.plugin.module.ui.WizardState
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

internal class BandLabModuleWizardViewModel(
    wizardScope: CoroutineScope,
    project: Project,
    moduleParent: String,
) {
    private val moduleNameRegex = "^(:[a-z-]+)+$".toRegex()

    private val moduleName = TextFieldState(moduleParent)
    private val featureName = TextFieldState()

    private val apiConfig = MutableStateFlow(BandLabModuleConfig.Api())
    private val implConfig = MutableStateFlow(BandLabModuleConfig.Impl())
    private val uiConfig = MutableStateFlow(BandLabModuleConfig.Ui())
    private val screenConfig = MutableStateFlow(BandLabModuleConfig.Screen())

    private val moduleNameTextFlow = snapshotFlow { moduleName.text.toString() }
    private val validationErrors: StateFlow<Set<ModuleValidationError>> = combine(
        apiConfig.map { it.isSelected }.distinctUntilChanged(),
        implConfig.map { it.isSelected }.distinctUntilChanged(),
        uiConfig.map { it.isSelected }.distinctUntilChanged(),
        screenConfig.map { it.isSelected }.distinctUntilChanged(),
        moduleNameTextFlow
    ) { isApiSelected, isImplSelected, isUiSelected, isScreenSelected, name ->
        buildSet {
            if (name.isBlank() || name == ":") {
                add(ModuleValidationError.ModuleNameEmpty)
                return@buildSet
            }
            if (!name.startsWith(':')) {
                add(ModuleValidationError.ModuleNameShouldStartWithColon)
                return@buildSet
            }
            if (name.endsWith(':')) {
                add(ModuleValidationError.ModuleNameEndsWithColon)
                return@buildSet
            }
            if (!moduleNameRegex.matches(name)) {
                add(ModuleValidationError.ModuleNameInvalidChar)
                return@buildSet
            }

            // Check for modules existence
            if (isApiSelected && "$name:api" in existingModuleNames) {
                add(ModuleValidationError.ApiModuleExist)
            }
            if (isImplSelected && "$name:impl" in existingModuleNames) {
                add(ModuleValidationError.ImplModuleExist)
            }
            if (isUiSelected && "$name:ui" in existingModuleNames) {
                add(ModuleValidationError.UiModuleExist)
            }
            if (isScreenSelected && "$name:screen" in existingModuleNames) {
                add(ModuleValidationError.ScreenModuleExist)
            }
        }
    }
        // Skip the initial result when the wizard is just opened.
        .drop(1)
        .stateIn(wizardScope, SharingStarted.WhileSubscribed(), emptySet())

    private val existingModuleNames = project.modules.map { module ->
        ':' + module.name
            .split('.')
            .drop(1) // drop the root folder
            .joinToString(":")
    }

    val canCreate = BoolValueProperty(false)

    val state = WizardState(
        moduleName = moduleName,
        apiConfig = apiConfig,
        implConfig = implConfig,
        uiConfig = uiConfig,
        screenConfig = screenConfig,
        onConfigClick = ::onConfigClick,
        onModuleTypeClick = ::onModuleTypeClick,
        onPluginClick = ::onPluginClick,
        onExposureClick = ::onExposureClick,
        onGenerateActivityClick = ::onGenerateActivityClick,
        onGeneratePageClick = ::onGeneratePageClick,
        featureName = featureName,
        validationErrors = validationErrors
    )

    init {
        println(existingModuleNames)

        // Map feature name with the module path by default
        moduleNameTextFlow
            .onEach { name ->
                featureName.setTextAndPlaceCursorAtEnd(
                    name.split(':', '-')
                        .joinToString("") {
                            it.replaceFirstChar { c -> c.uppercaseChar() }
                        }
                )
            }
            .launchIn(wizardScope)

        combine(
            apiConfig.map { it.isSelected }.distinctUntilChanged(),
            implConfig.map { it.isSelected }.distinctUntilChanged(),
            uiConfig.map { it.isSelected }.distinctUntilChanged(),
            screenConfig.map { it.isSelected }.distinctUntilChanged(),
            validationErrors
        ) { isApiSelected, isImplSelected, isUiSelected, isScreenSelected, errors ->
            val isAnyModuleSelected = isApiSelected || isImplSelected || isUiSelected || isScreenSelected
            val isValid = isAnyModuleSelected && errors.isEmpty()
            canCreate.set(isValid)
        }
            .launchIn(wizardScope)
    }

    private fun onConfigClick(config: BandLabModuleConfig) {
        when (config) {
            is BandLabModuleConfig.Api -> apiConfig.update { it.copy(isSelected = !it.isSelected) }
            is BandLabModuleConfig.Impl -> implConfig.update { it.copy(isSelected = !it.isSelected) }
            is BandLabModuleConfig.Ui -> uiConfig.update { it.copy(isSelected = !it.isSelected) }
            is BandLabModuleConfig.Screen -> {
                screenConfig.update { it.copy(isSelected = !it.isSelected) }
                // Select ui config as well when the screen is selected
                val isScreenSelected = screenConfig.value.isSelected
                if (isScreenSelected) {
                    uiConfig.update { it.copy(isSelected = true) }
                }
            }
        }
    }

    private fun onModuleTypeClick(config: BandLabModuleConfig, type: BandLabModuleType) {
        if (config !is BandLabModuleConfig.Impl) {
            error("Not supported yet")
        }
        implConfig.update { it.copy(type = type) }
    }

    private fun onPluginClick(config: BandLabModuleConfig, plugin: ModulePlugin) {
        val currentSelected = when (config) {
            is BandLabModuleConfig.Api -> apiConfig.value.selectedPlugins
            is BandLabModuleConfig.Impl -> implConfig.value.selectedPlugins
            is BandLabModuleConfig.Ui -> uiConfig.value.selectedPlugins
            is BandLabModuleConfig.Screen -> screenConfig.value.selectedPlugins
        }
        val selectedPlugins = if (plugin in currentSelected) {
            currentSelected - plugin
        } else {
            currentSelected + plugin
        }
        when (config) {
            is BandLabModuleConfig.Api -> apiConfig.update { it.copy(selectedPlugins = selectedPlugins) }
            is BandLabModuleConfig.Impl -> implConfig.update { it.copy(selectedPlugins = selectedPlugins) }
            is BandLabModuleConfig.Ui -> uiConfig.update { it.copy(selectedPlugins = selectedPlugins) }
            is BandLabModuleConfig.Screen -> screenConfig.update { it.copy(selectedPlugins = selectedPlugins) }
        }
    }

    private fun onExposureClick(config: BandLabModuleConfig, exposure: ModuleExposure) {
        when (config) {
            is BandLabModuleConfig.Impl -> implConfig.update { it.copy(exposure = exposure) }
            is BandLabModuleConfig.Screen -> screenConfig.update { it.copy(exposure = exposure) }
            is BandLabModuleConfig.Api, is BandLabModuleConfig.Ui -> error("Api and Ui module can't be exposed")
        }
    }

    private fun onGenerateActivityClick() {
        screenConfig.update { it.copy(generateActivityTemplate = !it.generateActivityTemplate) }
    }

    private fun onGeneratePageClick() {
        screenConfig.update { it.copy(generatePageTemplate = !it.generatePageTemplate) }
    }
}