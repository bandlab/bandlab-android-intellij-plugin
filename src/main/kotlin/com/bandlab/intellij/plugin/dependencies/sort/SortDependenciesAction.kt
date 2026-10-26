// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.dependencies.sort

import com.bandlab.intellij.plugin.BandLabIcons
import com.bandlab.intellij.plugin.utils.isBuildScriptFile
import com.bandlab.intellij.plugin.utils.psiFileOrNull
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class SortDependenciesAction :
    DumbAwareAction(
        /* text = */ "Sort Dependencies",
        /* description = */ "Format build.gradle with the project's gradle-fmt.",
        /* icon = */ BandLabIcons.logo,
    ) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible =
            project != null &&
                isBuildScriptFile(e.psiFileOrNull()?.name) &&
                GradleFmtRunner.scriptOrNull(project) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val buildGradle = e.psiFileOrNull()?.virtualFile ?: return

        GradleFmtRunner.format(project, buildGradle)
    }
}
