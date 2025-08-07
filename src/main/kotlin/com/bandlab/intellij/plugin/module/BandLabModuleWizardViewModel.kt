package com.bandlab.intellij.plugin.module

import androidx.compose.foundation.text.input.TextFieldState
import com.android.tools.idea.observable.core.BoolValueProperty
import com.bandlab.intellij.plugin.module.ui.WizardState
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class BandLabModuleWizardViewModel(
    project: Project,
    moduleParent: String,
) {

    private val isModuleNameInvalid = AtomicBooleanProperty(false)
    private val isModuleAlreadyExists = AtomicBooleanProperty(false)

    private val moduleName = TextFieldState(moduleParent)
    private val apiVariant = MutableStateFlow(BandLabModuleVariant.Api())
    private val implVariant = MutableStateFlow(BandLabModuleVariant.Impl())
    private val uiVariant = MutableStateFlow(BandLabModuleVariant.Ui())
    private val screenVariant = MutableStateFlow(BandLabModuleVariant.Screen())

    val canCreate = BoolValueProperty(false)

    private val existingModuleNames = project.modules.map { module ->
        ':' + module.name
            .split('.')
            .drop(1) // drop the root folder
            .joinToString(":")
    }

    val state = WizardState(
        moduleName = moduleName,
        apiVariant = apiVariant,
        implVariant = implVariant,
        uiVariant = uiVariant,
        screenVariant = screenVariant,
        onVariantClick = ::onVariantClick
    )

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
}