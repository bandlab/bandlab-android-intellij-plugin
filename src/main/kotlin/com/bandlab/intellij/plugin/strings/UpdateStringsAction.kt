package com.bandlab.intellij.plugin.strings

import com.bandlab.intellij.plugin.BandLabIcons
import com.bandlab.intellij.plugin.utils.GradleProjectUtils
import com.bandlab.intellij.plugin.utils.buildScriptName
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtilCore

class UpdateStringsAction : DumbAwareAction(
    /* text = */ "Update Strings",
    /* description = */ "Update localized strings from Tolgee.",
    /* icon = */ BandLabIcons.logo
) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val selectedFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        if (selectedFile == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val name = selectedFile.name
        if (name == "strings.xml" || name == "strings-plurals.xml") {
            e.presentation.isEnabledAndVisible = true
            return
        }

        val project = e.project
        val projectDir = project?.guessProjectDir()
        if (projectDir == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val gradleProjectFolder = GradleProjectUtils.findNearestGradleProject(projectDir, selectedFile)
        if (gradleProjectFolder != null) {
            val buildGradle = gradleProjectFolder.findChild(project.buildScriptName())
            if (buildGradle != null && !buildGradle.isDirectory) {
                val buildFileContent = VfsUtilCore.loadText(buildGradle)
                if (buildFileContent.contains("libs.plugins.localizer")) {
                    e.presentation.isEnabledAndVisible = true
                    return
                }
            }
        }

        e.presentation.isEnabledAndVisible = false
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val projectDir = project.guessProjectDir() ?: return
        val selectedFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val gradleProjectFolder = GradleProjectUtils.findNearestGradleProject(projectDir, selectedFile) ?: return
        val gradlePath = GradleProjectUtils.getGradleProjectPath(project, gradleProjectFolder) ?: return

        val systemId = ProjectSystemId("GRADLE")
        val settings = ExternalSystemTaskExecutionSettings().apply {
            executionName = "Update Strings ($gradlePath)"
            externalSystemIdString = systemId.id
            externalProjectPath = project.basePath
            taskNames = listOf("$gradlePath:updateStrings")
        }

        ExternalSystemUtil.runTask(
            /* taskSettings = */ settings,
            /* executorId = */ DefaultRunExecutor.EXECUTOR_ID,
            /* project = */ project,
            /* externalSystemId = */ systemId,
            /* callback = */ null,
            /* progressExecutionMode = */ ProgressExecutionMode.IN_BACKGROUND_ASYNC,
            /* activateToolWindowBeforeRun = */ true
        )
    }
}