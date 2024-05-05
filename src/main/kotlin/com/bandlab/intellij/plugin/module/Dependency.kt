package com.bandlab.intellij.plugin.module

sealed class Dependency {

    abstract val name: String

    data class Api(override val name: String): Dependency()

    data class Impl(override val name: String): Dependency()

}