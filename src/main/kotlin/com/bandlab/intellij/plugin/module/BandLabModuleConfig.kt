package com.bandlab.intellij.plugin.module

import androidx.compose.runtime.Immutable
import com.bandlab.intellij.plugin.module.ModulePlugin.*

enum class ModulePlugin {
    Compose, Database, Metro, PreferenceConfig, RemoteConfig, RestApi, TestFixtures
}

enum class BandLabModuleType {
    Android, Kotlin
}

enum class ModuleExposure {
    AppGraph, MixEditorGraph, None
}

@Immutable
sealed interface BandLabModuleConfig {

    val isSelected: Boolean
    val type: BandLabModuleType?
    val requireTypeSelection: Boolean
        get() = false

    val availablePlugins: Set<ModulePlugin>
    val selectedPlugins: Set<ModulePlugin>

    /**
     * null means module is not eligible for exposure
     */
    val exposure: ModuleExposure?

    data class Api(
        override val isSelected: Boolean = false,
        override val type: BandLabModuleType = BandLabModuleType.Kotlin,
        override val availablePlugins: Set<ModulePlugin> = ModulePlugin.entries.toSet() - Compose - Database,
        override val selectedPlugins: Set<ModulePlugin> = emptySet(),
        override val exposure: ModuleExposure? = null
    ) : BandLabModuleConfig

    data class Impl(
        override val isSelected: Boolean = false,
        override val type: BandLabModuleType? = null,
        override val requireTypeSelection: Boolean = true,
        override val availablePlugins: Set<ModulePlugin> = ModulePlugin.entries.toSet(),
        override val selectedPlugins: Set<ModulePlugin> = emptySet(),
        override val exposure: ModuleExposure = ModuleExposure.AppGraph,
    ) : BandLabModuleConfig

    data class Ui(
        override val isSelected: Boolean = false,
        override val type: BandLabModuleType = BandLabModuleType.Android,
        override val availablePlugins: Set<ModulePlugin> = setOf(Compose, Metro),
        override val selectedPlugins: Set<ModulePlugin> = setOf(Compose),
        override val exposure: ModuleExposure? = null
    ) : BandLabModuleConfig

    data class Screen(
        override val isSelected: Boolean = false,
        override val type: BandLabModuleType = BandLabModuleType.Android,
        override val availablePlugins: Set<ModulePlugin> = ModulePlugin.entries.toSet() - Database,
        override val selectedPlugins: Set<ModulePlugin> = setOf(Compose, Metro),
        override val exposure: ModuleExposure = ModuleExposure.AppGraph,
        val generateActivityTemplate: Boolean = false,
        val generatePageTemplate: Boolean = false,
    ) : BandLabModuleConfig
}