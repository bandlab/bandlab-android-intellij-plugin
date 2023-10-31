package com.bandlab.intellij.plugin.module

data class BandLabModuleConfig(
    val type: BandLabModuleType,
    val path: String,
    val name: String,
    val composeConvention: Boolean = false,
    val applyComposePlugin: Boolean = false,
    val applyDaggerPlugin: Boolean = false,
    val applyDatabasePlugin: Boolean = false,
    // Presented for generating dagger module, activity etc.
    val daggerModuleName: String? = null,
    val generateActivity: Boolean = false,
)

enum class BandLabModuleType {
    Android, Kotlin
}