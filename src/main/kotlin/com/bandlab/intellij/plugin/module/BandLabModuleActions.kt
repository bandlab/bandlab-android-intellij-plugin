package com.bandlab.intellij.plugin.module

import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.bandlab.intellij.plugin.utils.Const.NEW_LINE
import com.bandlab.intellij.plugin.utils.editFile
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.findOrCreateFile

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

class BandLabModuleAddToSpotlightAction(
    buttonText: String,
    private val moduleInfo: ModuleInfo
) : AnAction(buttonText) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        WriteCommandAction.runWriteCommandAction(
            project,
            "Edit Spotlight Config",
            null,
            {
                val fileSystem = LocalFileSystem.getInstance()
                val gradleDir = fileSystem.findFileByPath("${project.basePath}/gradle")!!
                val ideProjects = gradleDir.findOrCreateFile("ide-projects.txt")

                project.editFile(ideProjects) {
                    appendLine(moduleInfo.reference)
                    val modules = toString()
                        .split(NEW_LINE)
                        .filter { it.isNotBlank() }
                        .distinct()
                        .joinToString(NEW_LINE)
                    replace(0, length, modules)
                }
            }
        )
    }
}