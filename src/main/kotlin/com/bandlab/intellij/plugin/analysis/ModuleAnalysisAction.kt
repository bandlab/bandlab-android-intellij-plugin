package com.bandlab.intellij.plugin.analysis

import com.bandlab.intellij.plugin.BandLabIcons
import com.bandlab.intellij.plugin.utils.GradleProjectUtils
import com.bandlab.intellij.plugin.utils.isAndroidModule
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.roots.ModuleRootManager
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

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
        val basePath = project.basePath ?: return

        val terminalView = TerminalToolWindowManager.getInstance(project)
        val widget = terminalView.createShellWidget(basePath, "Module Analyzer", true, false)
        if (project.isAndroidModule(gradleProjectFolder.path)) {
            widget.sendCommandToExecute("./gradlew $gradlePath:analyzeModule")
        } else {
            widget.sendCommandToExecute("./gradlew $gradlePath:projectHealth")
        }
    }
}
