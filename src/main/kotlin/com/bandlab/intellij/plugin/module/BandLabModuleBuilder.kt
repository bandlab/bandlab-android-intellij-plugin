package com.bandlab.intellij.plugin.module

import com.intellij.ide.projectWizard.ProjectSettingsStep
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.Disposable
import com.intellij.openapi.module.ModifiableModuleModel
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.configuration.ModulesProvider

class BandLabModuleBuilder : ModuleBuilder() {

    private lateinit var moduleConfig: BandLabModuleConfig

    override fun getModuleType(): ModuleType<*> = BandLabModuleType.INSTANCE

    override fun createWizardSteps(
        wizardContext: WizardContext,
        modulesProvider: ModulesProvider
    ): Array<ModuleWizardStep> {
        return ModuleWizardStep.EMPTY_ARRAY
    }

    override fun getCustomOptionsStep(
        context: WizardContext,
        parentDisposable: Disposable
    ): ModuleWizardStep {
        return BandLabModuleWizardStep(context) { moduleConfig = it }
    }

    override fun commitModule(project: Project, model: ModifiableModuleModel?): Module? {
        BandLabModuleTemplate(project, moduleConfig).create()
        return null
    }

    // Ignore the default project settings step, we need only our custom step.
    override fun getIgnoredSteps() = listOf(ProjectSettingsStep::class.java)

}