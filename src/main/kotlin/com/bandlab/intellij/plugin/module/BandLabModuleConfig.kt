package com.bandlab.intellij.plugin.module

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
    val metro: Boolean = false,
    val restApi: Boolean = false,
    val remoteConfig: Boolean = false,
    val preferenceConfig: Boolean = false,
    val database: Boolean = false,
    val testFixtures: Boolean = false
)

enum class BandLabModuleType {
    Android, Kotlin
}

enum class ModuleExposure {
    AppGraph, MixEditorGraph, None
}