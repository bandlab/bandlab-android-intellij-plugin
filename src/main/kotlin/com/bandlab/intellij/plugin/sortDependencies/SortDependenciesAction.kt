package com.bandlab.intellij.plugin.sortDependencies

import com.bandlab.intellij.plugin.BandLabIcons
import com.bandlab.intellij.plugin.utils.Const.BUILD_GRADLE
import com.bandlab.intellij.plugin.utils.Const.DEPENDENCIES_END
import com.bandlab.intellij.plugin.utils.Const.DEPENDENCIES_START
import com.bandlab.intellij.plugin.utils.Const.NEW_LINE
import com.bandlab.intellij.plugin.utils.Const.PLUGINS_END
import com.bandlab.intellij.plugin.utils.Const.PLUGINS_START
import com.bandlab.intellij.plugin.utils.editFile
import com.bandlab.intellij.plugin.utils.psiFileOrNull
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project

class SortDependenciesAction : DumbAwareAction(
    /* text = */ "Sort Dependencies",
    /* description = */ "Sort dependencies in build.gradle.",
    /* icon = */ BandLabIcons.logo
) {

    private val moduleTypePluginIds = listOf(
        "bandlab.plugins.app",
        "bandlab.plugins.library",
        "bandlab.plugins.android.benchmark",
        "bandlab.plugins.android.baseline.generator",
        "bandlab.plugins.base",
    )

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    /**
     *  Make the action available only when the menu is shown for the build.gradle.kts
     */
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.psiFileOrNull()?.name == BUILD_GRADLE
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

    private fun Project.sortDependencies(fileAbsolutePath: String) {
        editFile(filePath = fileAbsolutePath, isAbsolute = true) {
            // Sort plugins
            val pluginsStartIndex = indexOf(PLUGINS_START)
            if (pluginsStartIndex == -1) {
                throw RuntimeException("Can't find $PLUGINS_START in $fileAbsolutePath.")
            }

            val pluginsToSortStartIndex = indexOf(NEW_LINE, pluginsStartIndex) + 1
            val pluginsToSortEndIndex = indexOf(PLUGINS_END, pluginsToSortStartIndex) - 1

            val sortedPlugins = substring(pluginsToSortStartIndex, pluginsToSortEndIndex)
                .split(NEW_LINE)
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
            // Always append module type plugin at the beginning
            val moduleTypePlugin = sortedPlugins.first { plugin ->
                moduleTypePluginIds.any { id -> id in plugin }
            }
            val plugins = (listOf(moduleTypePlugin) + (sortedPlugins - moduleTypePlugin)).joinToString(NEW_LINE)

            replace(pluginsToSortStartIndex, pluginsToSortEndIndex, plugins)

            // Sort dependencies
            val dependenciesStartIndex = indexOf(DEPENDENCIES_START)
            if (dependenciesStartIndex == -1) {
                throw RuntimeException("Can't find $DEPENDENCIES_START in $fileAbsolutePath.")
            }

            val dependenciesToSortStartIndex = indexOf(NEW_LINE, dependenciesStartIndex) + 1
            val dependenciesToSortEndIndex = indexOf(DEPENDENCIES_END, dependenciesToSortStartIndex) - 1

            val sortedDependencies = substring(dependenciesToSortStartIndex, dependenciesToSortEndIndex)
                .split(NEW_LINE)
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
                .joinToString(NEW_LINE)

            replace(dependenciesToSortStartIndex, dependenciesToSortEndIndex, sortedDependencies)
        }
    }
}
