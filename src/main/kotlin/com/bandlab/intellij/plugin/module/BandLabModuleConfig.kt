package com.bandlab.intellij.plugin.module

data class BandLabModuleConfig(
    val type: BandLabModuleType,
    val path: String,
    val name: String,
    val composeConvention: Boolean = false,
    val applyComposePlugin: Boolean = false,
    val applyAnvilPlugin: Boolean = false,
    val applyRestApiPlugin: Boolean = false,
    val applyRemoteConfigPlugin: Boolean = false,
    val applyDatabasePlugin: Boolean = false,
    // Presented for generating dagger module, activity etc.
    val daggerConfig: DaggerModuleConfig? = null,
    val generateActivity: Boolean = false,
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