package com.bandlab.intellij.plugin.analysis

import com.bandlab.intellij.plugin.BandLabIcons
import com.bandlab.intellij.plugin.utils.GradleProjectUtils
import com.bandlab.intellij.plugin.utils.isAndroidModule
import com.intellij.execution.ExecutionManager
import com.intellij.execution.ExecutionTargetManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.roots.ModuleRootManager

class ModuleAnalysisAction : DumbAwareAction(
    /* text = */ "Analyze Module",
    /* description = */ "Invoke DAGP and JVM module conversion analysis.",
    /* icon = */ BandLabIcons.logo
) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    /**
     *  Make the action available only when the menu is shown for the module root.
     */
    override fun update(e: AnActionEvent) {
        val selectedFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = selectedFile?.let(GradleProjectUtils::isGradleProject) == true
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val selectedFolder = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val module = e.getData(LangDataKeys.MODULE) ?: return
        val contentRoot = ModuleRootManager.getInstance(module).contentRoots.firstOrNull() ?: return
        val gradleProjectFolder = selectedFolder?.takeIf(GradleProjectUtils::isGradleProject) ?: contentRoot
        val gradlePath = GradleProjectUtils.getGradleProjectPath(project, gradleProjectFolder) ?: return

        val taskExecutionName = "Module Analysis ($gradlePath)"
        val systemId = ProjectSystemId("GRADLE")
        val settings = ExternalSystemTaskExecutionSettings().apply {
            executionName = taskExecutionName
            externalSystemIdString = systemId.id
            externalProjectPath = project.basePath
            taskNames = if (project.isAndroidModule(gradleProjectFolder.path)) {
                listOf("$gradlePath:analyzeModule")
            } else {
                listOf("$gradlePath:projectHealth")
            }
        }

        val runnerSettings = ExternalSystemUtil.createExternalSystemRunnerAndConfigurationSettings(
            settings,
            project,
            systemId
        ) ?: return

        val runConfig = runnerSettings.configuration
        if (runConfig is ExternalSystemRunConfiguration) {
            runConfig.isAllowRunningInParallel = true
        }

        runnerSettings.name = taskExecutionName

        val target = ExecutionTargetManager.getActiveTarget(project)
        ExecutionManager.getInstance(project).restartRunProfile(
            project,
            DefaultRunExecutor.getRunExecutorInstance(),
            target,
            runnerSettings,
            null
        )
    }
}
