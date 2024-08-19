package com.bandlab.intellij.plugin.module

data class BandLabModuleConfig(
    val type: BandLabModuleType,
    val path: String,
    val name: String,
    val composeConvention: Boolean = false,
    val plugins: ModulePlugins,
    // Presented for generating dagger module, activity etc.
    val daggerConfig: DaggerModuleConfig? = null,
    val generateActivity: Boolean = false,
)

data class ModulePlugins(
    val compose: Boolean = false,
    val anvil: Boolean = false,
    val restApi: Boolean = false,
    val remoteConfig: Boolean = false,
    val preferenceConfig: Boolean = false,
    val database: Boolean = false,
    val testFixtures: Boolean = false
)

enum class BandLabModuleType {
    Android, Kotlin
}

data class DaggerModuleConfig(
    val name: String,
    val exposure: DaggerModuleExposure
)

enum class DaggerModuleExposure {
    None, AppComponent, MixEditorViewComponent
}