package com.bandlab.intellij.plugin.module.dialog

import androidx.compose.runtime.Immutable
import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.bandlab.intellij.plugin.module.ModuleInfo
import com.bandlab.intellij.plugin.module.ui.WizardState
import com.bandlab.intellij.plugin.utils.Const.BUILD_GRADLE
import com.bandlab.intellij.plugin.utils.Const.NEW_LINE
import com.bandlab.intellij.plugin.utils.editFile
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.findOrCreateFile

@Immutable
internal data class FollowUpActionState(
    val text: String,
    val onClick: () -> Unit
)

internal class BandLabModuleFollowUpActionsViewModel(
    private val project: Project,
    private val modulePath: String,
    private val state: WizardState,
    private val projectSyncInvoker: ProjectSyncInvoker,
) {

    val actions: Set<FollowUpActionState> = buildSet {
        // Edit build.gradle
        if (state.apiConfig.value.isSelected) {
            FollowUpActionState(
                text = "Edit :api $BUILD_GRADLE",
                onClick = { editFile("$modulePath/api/$BUILD_GRADLE") }
            ).also(::add)
        }

        if (state.uiConfig.value.isSelected) {
            FollowUpActionState(
                text = "Edit :ui $BUILD_GRADLE",
                onClick = { editFile("$modulePath/ui/$BUILD_GRADLE") }
            ).also(::add)
        }

        if (state.implConfig.value.isSelected) {
            FollowUpActionState(
                text = "Edit :impl $BUILD_GRADLE",
                onClick = { editFile("$modulePath/impl/$BUILD_GRADLE") }
            ).also(::add)
        }

        if (state.screenConfig.value.isSelected) {
            FollowUpActionState(
                text = "Edit :screen $BUILD_GRADLE",
                onClick = { editFile("$modulePath/screen/$BUILD_GRADLE") }
            ).also(::add)
        }

        // Spotlight
        if (state.implConfig.value.isSelected) {
            FollowUpActionState(
                text = "Add :impl to spotlight",
                onClick = { addToSpotlight(ModuleInfo("$modulePath/impl")) }
            ).also(::add)
        }

        if (state.screenConfig.value.isSelected) {
            FollowUpActionState(
                text = "Add :screen to spotlight",
                onClick = { addToSpotlight(ModuleInfo("$modulePath/screen")) }
            ).also(::add)
        }

        add(
            FollowUpActionState(
                text = "Sync Project",
                onClick = ::syncProject
            )
        )
    }

    private fun syncProject() {
        projectSyncInvoker.syncProject(project)
    }

    private fun editFile(filePath: String) {
        val buildGradleFile = LocalFileSystem.getInstance()
            .refreshAndFindFileByPath(project.basePath + filePath)
            ?: return
        FileEditorManager.getInstance(project).openFile(buildGradleFile, true)
    }

    private fun addToSpotlight(moduleInfo: ModuleInfo) {
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