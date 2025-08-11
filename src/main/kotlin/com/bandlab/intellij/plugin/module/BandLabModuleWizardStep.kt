@file:Suppress("DialogTitleCapitalization", "UnstableApiUsage")

package com.bandlab.intellij.plugin.module

import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.android.tools.idea.observable.core.ObservableBool
import com.android.tools.idea.wizard.model.SkippableWizardStep
import com.android.tools.idea.wizard.model.WizardModel
import com.bandlab.intellij.plugin.module.ui.BandLabModuleWizard
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
    private val moduleParent: String,
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
        val modulePath = state.moduleName.text.toString().replace(':', '/')
        val featureName = state.featureName.text.toString()

        val apiModuleInfo = ModuleInfo("$modulePath/api")
        val uiModuleInfo = ModuleInfo("$modulePath/ui")

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
                        add(Dependency("projects.common.android.screen"))
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

//        NotificationGroupManager.getInstance()
//            .getNotificationGroup("BandLab Notification Group")
//            .createNotification("Module ${moduleNameInput.text} is created", NotificationType.INFORMATION)
//            .addActions(
//                setOf(
//                    BandLabModuleSyncAction(projectSyncInvoker),
//                    BandLabModuleEditFileAction(
//                        buttonText = "Edit $BUILD_GRADLE",
//                        filePath = "$modulePath/screen/$BUILD_GRADLE"
//                    )
//                )
//            )
//            .notify(project)

        wizardScope.cancel("Wizard step is finished")
    }
}