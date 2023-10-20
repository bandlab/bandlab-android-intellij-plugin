package com.bandlab.intellij.plugin.module

import com.bandlab.intellij.plugin.BandLabIcons
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
        return "Create modules easily with BandLab Android convention."
    }

    override fun getNodeIcon(isOpened: Boolean): Icon {
        return BandLabIcons.logo
    }

    @Suppress("CompanionObjectInExtension")
    companion object {
        val INSTANCE = BandLabModuleType()
    }
}