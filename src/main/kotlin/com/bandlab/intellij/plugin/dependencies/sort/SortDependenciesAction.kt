package com.bandlab.intellij.plugin.dependencies.sort

import com.bandlab.intellij.plugin.BandLabIcons
import com.bandlab.intellij.plugin.utils.GradleProjectUtils.sortDependencies
import com.bandlab.intellij.plugin.utils.isBuildScriptFile
import com.bandlab.intellij.plugin.utils.psiFileOrNull
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.DumbAwareAction

class SortDependenciesAction : DumbAwareAction(
    /* text = */ "Sort Dependencies",
    /* description = */ "Sort dependencies in build.gradle.",
    /* icon = */ BandLabIcons.logo
) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    /**
     *  Make the action available only when the menu is shown for the build.gradle.kts
     */
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isBuildScriptFile(e.psiFileOrNull()?.name)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val buildGradle = e.psiFileOrNull()?.virtualFile ?: return

        WriteCommandAction.runWriteCommandAction(
            /* project = */ project,
            /* commandName = */ "Sort Dependencies",
            /* groupID = */ null,
            /* runnable = */ {
                project.sortDependencies(buildGradle.path)
            }
        )
    }
}
