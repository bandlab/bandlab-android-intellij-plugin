package com.bandlab.intellij.plugin.module

import androidx.compose.runtime.Immutable
import com.bandlab.intellij.plugin.module.ModulePlugin.*

enum class ModulePlugin {
    Compose, Database, Metro, PreferenceConfig, RemoteConfig, RestApi, TestFixtures
}

enum class BandLabModuleType {
    Android, Kotlin
}

sealed interface ModuleTypeSelection {
    val type: BandLabModuleType?

    data class LockTo(override val type: BandLabModuleType) : ModuleTypeSelection
    data class RequireSelection(override val type: BandLabModuleType?) : ModuleTypeSelection
}

enum class ModuleExposure {
    AppGraph, MixEditorGraph, None
}

@Immutable
sealed interface BandLabModuleConfig {

    val isSelected: Boolean
    val typeSelection: ModuleTypeSelection

    val availablePlugins: Set<ModulePlugin>
    val selectedPlugins: Set<ModulePlugin>

    /**
     * null means module is not eligible for exposure
     */
    val exposure: ModuleExposure?

    data class Api(
        override val isSelected: Boolean = false,
        override val selectedPlugins: Set<ModulePlugin> = emptySet(),
    ) : BandLabModuleConfig {
        override val typeSelection: ModuleTypeSelection
            get() = ModuleTypeSelection.LockTo(BandLabModuleType.Kotlin)
        override val availablePlugins: Set<ModulePlugin>
            get() = ModulePlugin.entries.toSet() - Compose - Database
        override val exposure: ModuleExposure? = null
    }

    data class Impl(
        override val isSelected: Boolean = false,
        override val typeSelection: ModuleTypeSelection = ModuleTypeSelection.RequireSelection(null),
        override val selectedPlugins: Set<ModulePlugin> = setOf(Metro),
        override val exposure: ModuleExposure = ModuleExposure.AppGraph,
    ) : BandLabModuleConfig {
        override val availablePlugins: Set<ModulePlugin>
            get() = ModulePlugin.entries.toSet() - Compose
    }

    data class Ui(
        override val isSelected: Boolean = false,
        override val selectedPlugins: Set<ModulePlugin> = setOf(Compose),
    ) : BandLabModuleConfig {
        override val typeSelection: ModuleTypeSelection
            get() = ModuleTypeSelection.LockTo(BandLabModuleType.Android)
        override val availablePlugins: Set<ModulePlugin>
            get() = setOf(Compose, Metro)
        override val exposure: ModuleExposure? = null
    }

    data class Screen(
        override val isSelected: Boolean = false,
        override val selectedPlugins: Set<ModulePlugin> = setOf(Compose, Metro),
        override val exposure: ModuleExposure = ModuleExposure.AppGraph,
        val template: Template? = null,
    ) : BandLabModuleConfig {
        override val typeSelection: ModuleTypeSelection
            get() = ModuleTypeSelection.LockTo(BandLabModuleType.Android)
        override val availablePlugins: Set<ModulePlugin>
            get() = ModulePlugin.entries.toSet() - Database

        enum class Template {
            Activity, Page
        }
    }
}