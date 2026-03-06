package com.bandlab.intellij.plugin.test.fixtures

import com.bandlab.intellij.plugin.BandLabIcons
import com.bandlab.intellij.plugin.module.ModuleInfo
import com.bandlab.intellij.plugin.utils.Const.PLUGINS_END
import com.bandlab.intellij.plugin.utils.Const.PLUGINS_START
import com.bandlab.intellij.plugin.utils.GradleProjectUtils.sortDependencies
import com.bandlab.intellij.plugin.utils.editFile
import com.bandlab.intellij.plugin.utils.isBuildScriptFile
import com.bandlab.intellij.plugin.utils.psiFileOrNull
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

class ApplyTestFixturesPluginAction : DumbAwareAction(
    /* text = */ "Apply Test Fixtures Plugin",
    /* description = */ "Apply testFixtures plugin and create required folders.",
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
            /* commandName = */ "Apply Test Fixtures Plugin",
            /* groupID = */ null,
            /* runnable = */ {
                project.addTestFixturesPlugin(buildGradle.path)
                project.createTestFixturesFolder(buildGradle)
                project.sortDependencies(buildGradle.path)
            }
        )
    }

    private fun Project.addTestFixturesPlugin(fileAbsolutePath: String) {
        editFile(filePath = fileAbsolutePath, isAbsolute = true) {
            val pluginsIndex = indexOf(PLUGINS_START)
            if (pluginsIndex == -1) {
                throw RuntimeException("Can't find $PLUGINS_START in $fileAbsolutePath.")
            }

            val pluginDeclaration = "    alias(bandlab.plugins.testFixtures)"
            if (pluginDeclaration !in this) {
                val indexToInsert = indexOf(PLUGINS_END, pluginsIndex) - 1
                insert(indexToInsert, "\n$pluginDeclaration")
            }
        }
    }

    private fun Project.createTestFixturesFolder(buildGradle: VirtualFile) {
        val projectRootPath = basePath ?: return
        val moduleRelativePath = buildGradle.parent.path.removePrefix(projectRootPath)
        val moduleInfo = ModuleInfo(path = moduleRelativePath)

        val folder = File(basePath + moduleInfo.testFixturesPath)
        folder.mkdirs()
        LocalFileSystem.getInstance().refreshIoFiles(listOf(folder))
    }
}
