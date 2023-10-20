package com.bandlab.intellij.plugin.module

import com.intellij.openapi.module.ModuleType
import javax.swing.Icon

class BandLabModuleType : ModuleType<BandLabModuleBuilder>("bandlab_module") {

    override fun createModuleBuilder(): BandLabModuleBuilder {
        return BandLabModuleBuilder()
    }

    override fun getName(): String {
        return "BandLab Convention"
    }

    override fun getDescription(): String {
        return "bla bla"
    }

    override fun getNodeIcon(isOpened: Boolean): Icon {
        return org.jdesktop.swingx.icon.EmptyIcon()
    }

    @Suppress("CompanionObjectInExtension")
    companion object {
        val INSTANCE = BandLabModuleType()
    }
}