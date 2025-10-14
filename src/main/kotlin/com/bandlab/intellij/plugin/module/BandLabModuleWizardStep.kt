package com.bandlab.intellij.plugin.module

import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.android.tools.idea.observable.core.ObservableBool
import com.android.tools.idea.wizard.model.SkippableWizardStep
import com.android.tools.idea.wizard.model.WizardModel
import com.bandlab.intellij.plugin.module.dialog.BandLabModuleFollowUpActionsDialog
import com.bandlab.intellij.plugin.module.dialog.BandLabModuleFollowUpActionsViewModel
import com.bandlab.intellij.plugin.module.ui.BandLabModuleWizard
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.enableNewSwingCompositing
import javax.swing.JComponent

class EmptyModel : WizardModel() {
    override fun handleFinished() = Unit
}

class BandLabModuleWizardStep(
    private val project: Project,
    moduleParent: String,
    private val projectSyncInvoker: ProjectSyncInvoker,
) : SkippableWizardStep<EmptyModel>(EmptyModel(), "BandLab Convention") {

    private val wizardScope = CoroutineScope(Dispatchers.Main)
    private val viewModel = BandLabModuleWizardViewModel(wizardScope, project, moduleParent)

    override fun getComponent(): JComponent {
        @OptIn(ExperimentalJewelApi::class)
        enableNewSwingCompositing()

        return JewelComposePanel {
            BandLabModuleWizard(viewModel.state)
        }
    }

    override fun canGoForward(): ObservableBool {
        return viewModel.canCreate
    }

    override fun onWizardFinished() {
        val state = viewModel.state
        val moduleRoot = state.moduleRoot.text
        val modulePath = moduleRoot.toString().replace(':', '/')
        val featureName = state.featureName.text.toString()

        val apiModuleInfo = ModuleInfo("$modulePath/api")
        val uiModuleInfo = ModuleInfo("$modulePath/ui")

        runWriteCommandAction {
            listOf(
                state.apiConfig,
                state.implConfig,
                state.screenConfig,
                state.uiConfig,
            )
                .filter { it.value.isSelected }
                .forEach { configState ->
                    val config = configState.value
                    val moduleInfo = when (config) {
                        is BandLabModuleConfig.Api -> apiModuleInfo
                        is BandLabModuleConfig.Impl -> ModuleInfo("$modulePath/impl")
                        is BandLabModuleConfig.Screen -> ModuleInfo("$modulePath/screen")
                        is BandLabModuleConfig.Ui -> uiModuleInfo
                    }
                    val dependsOn = buildList {
                        if (config is BandLabModuleConfig.Screen) {
                            add(Dependency("project(\":common:android:screen\")"))
                            if (state.uiConfig.value.isSelected) {
                                // Depends on the ui module where the composables located
                                add(Dependency(uiModuleInfo.projectAccessorReference))
                            }
                        }
                        if (config !is BandLabModuleConfig.Api) {
                            if (state.apiConfig.value.isSelected) {
                                // Expose api transitively from ui, impl and screen module
                                add(
                                    Dependency(
                                        name = apiModuleInfo.projectAccessorReference,
                                        config = DependencyConfiguration.Api
                                    )
                                )
                            }
                        }
                    }

                    val template = BandLabModuleTemplate(
                        project = project,
                        moduleInfo = moduleInfo,
                        config = config,
                        featureName = featureName,
                        dependsOn = dependsOn
                    )
                    template.create()
                }
        }

        ApplicationManager.getApplication().invokeLater {
            BandLabModuleFollowUpActionsDialog(
                project = project,
                viewModel = BandLabModuleFollowUpActionsViewModel(
                    project = project,
                    modulePath = modulePath,
                    state = state,
                    projectSyncInvoker = projectSyncInvoker
                )
            ).show()
        }

        wizardScope.cancel("Wizard step is finished")
    }

    private fun runWriteCommandAction(block: () -> Unit) {
        WriteCommandAction.runWriteCommandAction(
            project,
            "Create Template",
            null,
            block
        )
    }
}