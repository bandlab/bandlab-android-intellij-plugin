package com.bandlab.intellij.plugin.module

import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.LocalFileSystem

class BandLabModuleSyncAction(
    private val projectSyncInvoker: ProjectSyncInvoker
) : AnAction("Sync Project") {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        projectSyncInvoker.syncProject(project)
    }
}

class BandLabModuleEditFileAction(
    buttonText: String,
    private val filePath: String
) : AnAction(buttonText) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val buildGradleFile = LocalFileSystem.getInstance()
            .refreshAndFindFileByPath(project.basePath + filePath)
            ?: return
        FileEditorManager.getInstance(project).openFile(buildGradleFile, true)
    }
}