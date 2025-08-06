package com.bandlab.intellij.plugin.module

import com.android.tools.idea.observable.core.BoolValueProperty
import com.bandlab.intellij.plugin.module.ui.WizardState
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules

internal class BandLabModuleWizardViewModel(
    project: Project
) {

    private val isModuleNameInvalid = AtomicBooleanProperty(false)
    private val isModuleAlreadyExists = AtomicBooleanProperty(false)

    val canCreate = BoolValueProperty(false)

    private val existingModuleNames = project.modules.map { module ->
        ':' + module.name
            .split('.')
            .drop(1) // drop the root folder
            .joinToString(":")
    }

    val state = WizardState(a = true)
}