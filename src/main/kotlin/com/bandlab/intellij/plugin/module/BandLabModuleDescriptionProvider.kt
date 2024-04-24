package com.bandlab.intellij.plugin.module

import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.android.tools.idea.npw.module.ModuleDescriptionProvider
import com.android.tools.idea.npw.module.ModuleGalleryEntry
import com.android.tools.idea.wizard.model.SkippableWizardStep
import com.bandlab.intellij.plugin.BandLabIcons
import com.intellij.openapi.project.Project
import javax.swing.Icon

class BandLabModuleDescriptionProvider : ModuleDescriptionProvider {

    override fun getDescriptions(project: Project): Collection<ModuleGalleryEntry> {
        return listOf(BandLabModuleEntry())
    }
}

private class BandLabModuleEntry : ModuleGalleryEntry {

    override val name: String = "BandLab Convention"

    override val description: String = "Create modules easily with BandLab Android convention."

    override val icon: Icon = BandLabIcons.logo

    override fun createStep(
        project: Project,
        moduleParent: String,
        projectSyncInvoker: ProjectSyncInvoker
    ): SkippableWizardStep<*> {
        return BandLabModuleWizardStep(project)
    }
}