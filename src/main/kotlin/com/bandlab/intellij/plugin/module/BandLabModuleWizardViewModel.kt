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
    private val featureName = TextFieldState("UserProfile")

    private val apiVariant = MutableStateFlow(BandLabModuleVariant.Api())
    private val implVariant = MutableStateFlow(BandLabModuleVariant.Impl())
    private val uiVariant = MutableStateFlow(BandLabModuleVariant.Ui())
    private val screenVariant = MutableStateFlow(BandLabModuleVariant.Screen())

    private val moduleNameTextFlow = snapshotFlow { moduleName.text.toString() }
    private val validationErrors: StateFlow<Set<ModuleValidationError>> = combine(
        apiVariant.map { it.isSelected }.distinctUntilChanged(),
        implVariant.map { it.isSelected }.distinctUntilChanged(),
        uiVariant.map { it.isSelected }.distinctUntilChanged(),
        screenVariant.map { it.isSelected }.distinctUntilChanged(),
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
        apiVariant = apiVariant,
        implVariant = implVariant,
        uiVariant = uiVariant,
        screenVariant = screenVariant,
        onVariantClick = ::onVariantClick,
        onModuleTypeClick = ::onModuleTypeClick,
        onPluginClick = ::onPluginClick,
        onExposureClick = ::onExposureClick,
        onGenerateActivityClick = ::onGenerateActivityClick,
        onGeneratePageClick = ::onGeneratePageClick,
        featureName = featureName,
        validationErrors = validationErrors
    )

    init {
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
    }

    private fun onVariantClick(variant: BandLabModuleVariant) {
        when (variant) {
            is BandLabModuleVariant.Api -> apiVariant.update { it.copy(isSelected = !it.isSelected) }
            is BandLabModuleVariant.Impl -> implVariant.update { it.copy(isSelected = !it.isSelected) }
            is BandLabModuleVariant.Ui -> uiVariant.update { it.copy(isSelected = !it.isSelected) }
            is BandLabModuleVariant.Screen -> {
                screenVariant.update { it.copy(isSelected = !it.isSelected) }
                // Select ui variant as well when the screen is selected
                val isScreenSelected = screenVariant.value.isSelected
                if (isScreenSelected) {
                    uiVariant.update { it.copy(isSelected = true) }
                }
            }
        }
    }

    private fun onModuleTypeClick(variant: BandLabModuleVariant, type: BandLabModuleType) {
        if (variant !is BandLabModuleVariant.Impl) {
            error("Not supported yet")
        }
        implVariant.update { it.copy(type = type) }
    }

    private fun onPluginClick(variant: BandLabModuleVariant, plugin: ModulePlugin) {
        val currentSelected = when (variant) {
            is BandLabModuleVariant.Api -> apiVariant.value.selectedPlugins
            is BandLabModuleVariant.Impl -> implVariant.value.selectedPlugins
            is BandLabModuleVariant.Ui -> uiVariant.value.selectedPlugins
            is BandLabModuleVariant.Screen -> screenVariant.value.selectedPlugins
        }
        val selectedPlugins = if (plugin in currentSelected) {
            currentSelected - plugin
        } else {
            currentSelected + plugin
        }
        when (variant) {
            is BandLabModuleVariant.Api -> apiVariant.update { it.copy(selectedPlugins = selectedPlugins) }
            is BandLabModuleVariant.Impl -> implVariant.update { it.copy(selectedPlugins = selectedPlugins) }
            is BandLabModuleVariant.Ui -> uiVariant.update { it.copy(selectedPlugins = selectedPlugins) }
            is BandLabModuleVariant.Screen -> screenVariant.update { it.copy(selectedPlugins = selectedPlugins) }
        }
    }

    private fun onExposureClick(variant: BandLabModuleVariant, exposure: ModuleExposure) {
        when (variant) {
            is BandLabModuleVariant.Impl -> implVariant.update { it.copy(exposure = exposure) }
            is BandLabModuleVariant.Screen -> screenVariant.update { it.copy(exposure = exposure) }
            is BandLabModuleVariant.Api, is BandLabModuleVariant.Ui -> error("Api and Ui module can't be exposed")
        }
    }

    private fun onGenerateActivityClick() {
        screenVariant.update { it.copy(generateActivityTemplate = !it.generateActivityTemplate) }
    }

    private fun onGeneratePageClick() {
        screenVariant.update { it.copy(generatePageTemplate = !it.generatePageTemplate) }
    }
}