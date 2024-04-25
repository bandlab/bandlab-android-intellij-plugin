package com.bandlab.intellij.plugin.module

import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class BandLabModuleSyncAction(
    private val project: Project,
    private val projectSyncInvoker: ProjectSyncInvoker
) : AnAction("Sync Project") {

    override fun actionPerformed(e: AnActionEvent) {
        projectSyncInvoker.syncProject(project)
    }
}