package com.bandlab.intellij.plugin.module

import androidx.compose.runtime.Immutable
import com.bandlab.intellij.plugin.module.ModulePlugin.*

data class BandLabModuleConfig(
    val type: BandLabModuleType,
    val path: String,
    val composeConvention: Boolean = false,
    val plugins: ModulePlugins,
    val exposure: ModuleExposure,
    val featureName: String,
    val generateActivity: Boolean = false,
)

data class ModulePlugins(
    val compose: Boolean = false,
    val database: Boolean = false,
    val metro: Boolean = false,
    val preferenceConfig: Boolean = false,
    val remoteConfig: Boolean = false,
    val restApi: Boolean = false,
    val testFixtures: Boolean = false,
)

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
sealed interface BandLabModuleVariant {

    val isSelected: Boolean
    val type: BandLabModuleType?

    val availablePlugins: List<ModulePlugin>
    val selectedPlugins: List<ModulePlugin>

    /**
     * null means module is not eligible for exposure
     */
    val exposure: ModuleExposure?

    data class Api(
        override val isSelected: Boolean = false,
        override val type: BandLabModuleType = BandLabModuleType.Kotlin,
        override val availablePlugins: List<ModulePlugin> = ModulePlugin.entries - Compose - Database,
        override val selectedPlugins: List<ModulePlugin> = emptyList(),
        override val exposure: ModuleExposure? = null
    ) : BandLabModuleVariant

    data class Impl(
        override val isSelected: Boolean = false,
        override val type: BandLabModuleType? = null, // Require explicit selection
        override val availablePlugins: List<ModulePlugin> = ModulePlugin.entries,
        override val selectedPlugins: List<ModulePlugin> = emptyList(),
        override val exposure: ModuleExposure = ModuleExposure.AppGraph,
    ) : BandLabModuleVariant

    data class Ui(
        override val isSelected: Boolean = false,
        override val type: BandLabModuleType = BandLabModuleType.Android,
        override val availablePlugins: List<ModulePlugin> = listOf(Compose, Metro),
        override val selectedPlugins: List<ModulePlugin> = listOf(Compose),
        override val exposure: ModuleExposure? = null
    ) : BandLabModuleVariant

    data class Screen(
        override val isSelected: Boolean = false,
        override val type: BandLabModuleType = BandLabModuleType.Android,
        override val availablePlugins: List<ModulePlugin> = ModulePlugin.entries - Database,
        override val selectedPlugins: List<ModulePlugin> = listOf(Compose, Metro),
        override val exposure: ModuleExposure = ModuleExposure.AppGraph,
        val generateActivityTemplate: Boolean = false,
        val generatePageTemplate: Boolean = false,
    ) : BandLabModuleVariant
}