package com.bandlab.intellij.plugin.strings

import com.bandlab.intellij.plugin.BandLabIcons
import com.bandlab.intellij.plugin.utils.GradleProjectUtils
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.guessProjectDir
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

class UpdateStringsAction : DumbAwareAction(
    /* text = */ "Update Strings",
    /* description = */ "Update localized strings from Tolgee.",
    /* icon = */ BandLabIcons.logo
) {
    private val eligibleModules = setOf(
        ":audiostretch:common-strings",
        ":common:android:strings",
        ":edu:strings"
    )

    private val updateStringsCommand = "./localizer/bandlab-localizer update-strings"

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
        if (
            gradleProjectFolder != null &&
            GradleProjectUtils.getGradleProjectPath(project, gradleProjectFolder) in eligibleModules
        ) {
            e.presentation.isEnabledAndVisible = true
            return
        }

        e.presentation.isEnabledAndVisible = false
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val basePath = project.basePath ?: return
        val terminalManager = TerminalToolWindowManager.getInstance(project)

        // Try to find and reuse existing "Update Strings" tab
        val toolWindow = terminalManager.toolWindow
        if (toolWindow != null) {
            val contentManager = toolWindow.contentManager
            val existingContent = contentManager.contents.firstOrNull {
                it.displayName == "Update Strings"
            }
            if (existingContent != null) {
                contentManager.setSelectedContent(existingContent)
                toolWindow.activate {
                    val widget = TerminalToolWindowManager.findWidgetByContent(existingContent)
                    if (widget is ShellTerminalWidget) {
                        widget.executeCommand(updateStringsCommand)
                    } else {
                        widget?.sendCommandToExecute(updateStringsCommand)
                    }
                }
                return
            }
        }

        // Create new terminal tab
        val widget = terminalManager.createShellWidget(basePath, "Update Strings", true, false)
        widget.sendCommandToExecute(updateStringsCommand)
    }
}