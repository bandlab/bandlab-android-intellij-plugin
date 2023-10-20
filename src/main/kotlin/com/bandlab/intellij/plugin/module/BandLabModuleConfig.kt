package com.bandlab.intellij.plugin.module

sealed class BandLabModuleConfig(
    open val path: String,
    open val name: String
) {

    class Kotlin(
        override val path: String,
        override val name: String,
    ) : BandLabModuleConfig(path, name)

    class Android(
        override val path: String,
        override val name: String,
        val composeConvention: Boolean = false,
        val applyComposePlugin: Boolean = false,
        val applyDaggerPlugin: Boolean = false,
        val applyDatabasePlugin: Boolean = false,
    ) : BandLabModuleConfig(path, name)

}