package com.bandlab.intellij.plugin.module

import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.android.tools.idea.observable.core.ObservableBool
import com.android.tools.idea.wizard.model.SkippableWizardStep
import com.android.tools.idea.wizard.model.WizardModel
import com.bandlab.intellij.plugin.module.ui.BandLabModuleWizard
import com.bandlab.intellij.plugin.utils.Const.BUILD_GRADLE
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
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
        val moduleName = state.moduleName.text
        val modulePath = moduleName.toString().replace(':', '/')
        val featureName = state.featureName.text.toString()

        val apiModuleInfo = ModuleInfo("$modulePath/api")
        val implModuleInfo = ModuleInfo("$modulePath/impl")
        val screenModuleInfo = ModuleInfo("$modulePath/screen")
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
                        is BandLabModuleConfig.Impl -> implModuleInfo
                        is BandLabModuleConfig.Screen -> screenModuleInfo
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
        }

        NotificationGroupManager.getInstance()
            .getNotificationGroup("BandLab Notification Group")
            .createNotification("Your $moduleName modules are ready!", NotificationType.INFORMATION)
            .addActions(
                buildSet {
                    if (state.apiConfig.value.isSelected) {
                        BandLabModuleEditFileAction(
                            buttonText = "Edit :api $BUILD_GRADLE",
                            filePath = "$modulePath/api/$BUILD_GRADLE"
                        ).also(::add)
                    }
                    if (state.uiConfig.value.isSelected) {
                        BandLabModuleEditFileAction(
                            buttonText = "Edit :ui $BUILD_GRADLE",
                            filePath = "$modulePath/ui/$BUILD_GRADLE"
                        ).also(::add)
                    }
                    if (state.implConfig.value.isSelected) {
                        BandLabModuleEditFileAction(
                            buttonText = "Edit :impl $BUILD_GRADLE",
                            filePath = "$modulePath/impl/$BUILD_GRADLE"
                        ).also(::add)

                        BandLabModuleAddToSpotlightAction(
                            buttonText = "Add :impl to spotlight",
                            moduleInfo = implModuleInfo
                        ).also(::add)
                    }
                    if (state.screenConfig.value.isSelected) {
                        BandLabModuleEditFileAction(
                            buttonText = "Edit :screen $BUILD_GRADLE",
                            filePath = "$modulePath/screen/$BUILD_GRADLE"
                        ).also(::add)

                        BandLabModuleAddToSpotlightAction(
                            buttonText = "Add :screen to spotlight",
                            moduleInfo = screenModuleInfo
                        ).also(::add)
                    }
                    add(BandLabModuleSyncAction(projectSyncInvoker))
                }
            )
            .notify(project)

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