package com.bandlab.intellij.plugin.module

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import com.android.tools.idea.observable.core.BoolValueProperty
import com.bandlab.intellij.plugin.module.ui.WizardState
import com.bandlab.intellij.plugin.utils.Const.ALL_PROJECTS_PATH
import com.bandlab.intellij.plugin.utils.Const.NEW_LINE
import com.bandlab.intellij.plugin.utils.combine
import com.bandlab.intellij.plugin.utils.readFile
import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

internal class BandLabModuleWizardViewModel(
    wizardScope: CoroutineScope,
    private val project: Project,
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

    private val existingModuleNames = ::retrieveExistingModules
        .asFlow()
        .shareIn(wizardScope, SharingStarted.WhileSubscribed(), replay = 1)

    private val validationErrors: StateFlow<Set<ModuleValidationError>> = combine(
        apiConfig.map { it.isSelected }.distinctUntilChanged(),
        implConfig.map { it.isSelected }.distinctUntilChanged(),
        uiConfig.map { it.isSelected }.distinctUntilChanged(),
        screenConfig.map { it.isSelected }.distinctUntilChanged(),
        moduleNameTextFlow,
        existingModuleNames
    ) { isApiSelected, isImplSelected, isUiSelected, isScreenSelected, name, existingModuleNames ->
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

    val canCreate = BoolValueProperty(false)

    val state = WizardState(
        moduleRoot = moduleName,
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
        existingModuleNames = existingModuleNames,
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

        combine(
            apiConfig.map { it.isSelected }.distinctUntilChanged(),
            implConfig,
            uiConfig.map { it.isSelected }.distinctUntilChanged(),
            screenConfig.map { it.isSelected }.distinctUntilChanged(),
            validationErrors
        ) { isApiSelected, impl, isUiSelected, isScreenSelected, errors ->
            val isAnyModuleSelected = isApiSelected || impl.isSelected || isUiSelected || isScreenSelected
            val isImplModuleTypeValid = !impl.isSelected || impl.typeSelection.type != null
            val isValid = isAnyModuleSelected && isImplModuleTypeValid && errors.isEmpty()
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
        implConfig.update { it.copy(typeSelection = ModuleTypeSelection.RequireSelection(type)) }
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
        screenConfig.update {
            if (it.template == BandLabModuleConfig.Screen.Template.Activity) {
                it.copy(template = null)
            } else {
                it.copy(template = BandLabModuleConfig.Screen.Template.Activity)
            }
        }
    }

    private fun onGeneratePageClick() {
        screenConfig.update {
            if (it.template == BandLabModuleConfig.Screen.Template.Page) {
                it.copy(template = null)
            } else {
                it.copy(template = BandLabModuleConfig.Screen.Template.Page)
            }
        }
    }

    private suspend fun retrieveExistingModules(): Set<String> = withContext(Dispatchers.IO) {
        readAction {
            project.readFile(
                filePath = ALL_PROJECTS_PATH,
                isAbsolute = false
            )
                .split(NEW_LINE)
                .filter { it.isNotBlank() }
                .toSet()
        }
    }
}