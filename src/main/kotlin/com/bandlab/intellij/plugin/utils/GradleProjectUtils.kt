/*
 * Copyright (C) 2025 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Copied and modified from
// https://github.com/slackhq/foundry/blob/main/platforms/intellij/skate/src/main/kotlin/foundry/intellij/skate/gradle/GradleProjectUtils.kt
package com.bandlab.intellij.plugin.utils

import com.google.common.base.CaseFormat
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.Paths

/** Utility functions for working with Gradle projects. */
object GradleProjectUtils {

    /**
     * Checks if the given directory is a Gradle project by looking for build.gradle or
     * build.gradle.kts files.
     */
    fun isGradleProject(directory: VirtualFile): Boolean {
        if (!directory.isDirectory) return false
        return directory.children.any { it.name == "build.gradle" || it.name == "build.gradle.kts" }
    }

    /**
     * Finds the nearest parent Gradle project directory for the given file or directory. Returns the
     * file itself if it's a Gradle project directory, or null if no Gradle project is found.
     */
    fun findNearestGradleProject(root: VirtualFile, file: VirtualFile): VirtualFile? {
        var current: VirtualFile? = file
        while (current != null) {
            if (isGradleProject(current)) {
                return current
            } else if (current == root) {
                return null
            }
            current = current.parent
        }
        return null
    }

    /**
     * Gets the Gradle project path for the given directory in the format ":path:to:project". Returns
     * null if the directory is not part of a Gradle project.
     */
    fun getGradleProjectPath(project: Project, directory: VirtualFile): String? {
        val projectBasePath = project.basePath ?: return null
        val projectBaseDir = Paths.get(projectBasePath)
        val directoryPath = Paths.get(directory.path)

        // Check if the directory is within the project
        if (!directoryPath.startsWith(projectBaseDir)) {
            return null
        }

        // Get the relative path from the project base directory
        val relativePath = projectBaseDir.relativize(directoryPath)
        if (relativePath.toString().isEmpty()) {
            return ":"
        }

        // Convert the path to Gradle format
        return convertRelativePathToGradlePath(relativePath.toString())
    }

    /** Converts a relative filesystem path to a Gradle project path format. */
    internal fun convertRelativePathToGradlePath(relativePath: String): String {
        if (relativePath.isEmpty()) {
            return ":"
        }
        return ":" + relativePath.replace('/', ':')
    }

    /**
     * Gets the Gradle project accessor path for the given directory in the format
     * "projects.path.to.project". Returns null if the directory is not part of a Gradle project or
     * the root project
     */
    fun getGradleProjectAccessorPath(project: Project, directory: VirtualFile): String? {
        val gradlePath = getGradleProjectPath(project, directory) ?: return null
        if (gradlePath == ":") {
            return null
        }

        // Convert from ":path:to:project" to "projects.path.to.project"
        return "projects." + convertProjectPathToAccessor(gradlePath)
    }

    fun parseProjectPaths(content: String): Set<String> {
        return content
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .toSet()
    }

    private val moduleTypePluginIds = listOf(
        "bandlab.plugins.app",
        "bandlab.plugins.library",
        "bandlab.plugins.android.benchmark",
        "bandlab.plugins.android.baseline.generator",
        "bandlab.plugins.base",
    )

    fun Project.sortDependencies(fileAbsolutePath: String) {
        editFile(filePath = fileAbsolutePath, isAbsolute = true) {
            // Sort plugins
            val pluginsStartIndex = indexOf(Const.PLUGINS_START)
            if (pluginsStartIndex == -1) {
                throw RuntimeException("Can't find ${Const.PLUGINS_START} in $fileAbsolutePath.")
            }

            val pluginsToSortStartIndex = indexOf(Const.NEW_LINE, pluginsStartIndex) + 1
            val pluginsToSortEndIndex = indexOf(Const.PLUGINS_END, pluginsToSortStartIndex) - 1

            val sortedPlugins = substring(pluginsToSortStartIndex, pluginsToSortEndIndex)
                .split(Const.NEW_LINE)
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
            // Always append module type plugin at the beginning
            val moduleTypePlugin = sortedPlugins.first { plugin ->
                moduleTypePluginIds.any { id -> id in plugin }
            }
            val plugins = (listOf(moduleTypePlugin) + (sortedPlugins - moduleTypePlugin)).joinToString(Const.NEW_LINE)

            replace(pluginsToSortStartIndex, pluginsToSortEndIndex, plugins)

            // Sort dependencies
            val dependenciesStartIndex = indexOf(Const.DEPENDENCIES_START)
            if (dependenciesStartIndex == -1) {
                throw RuntimeException("Can't find ${Const.DEPENDENCIES_START} in $fileAbsolutePath.")
            }

            val dependenciesToSortStartIndex = indexOf(Const.NEW_LINE, dependenciesStartIndex) + 1
            val dependenciesToSortEndIndex = indexOf(Const.DEPENDENCIES_END, dependenciesToSortStartIndex) - 1

            val sortedDependencies = substring(dependenciesToSortStartIndex, dependenciesToSortEndIndex)
                .split(Const.NEW_LINE)
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
                .joinToString(Const.NEW_LINE)

            replace(dependenciesToSortStartIndex, dependenciesToSortEndIndex, sortedDependencies)
        }
    }
}

/**
 * Converts a kebab-case string (e.g., `some-module-name`) to lowerCamelCase (e.g.,
 * `someModuleName`).
 *
 * Used to transform Gradle module names into Kotlin DSL-friendly accessors.
 */
private fun kebabCaseToCamelCase(s: String): String {
    return CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, s)
}

/**
 * Returns a project accessor representation of the given [projectPath].
 *
 * Example: `:libraries:foundation` -> `libraries.foundation`.
 */
fun convertProjectPathToAccessor(projectPath: String): String {
    return projectPath.removePrefix(":").split(":").joinToString(separator = ".") { segment ->
        kebabCaseToCamelCase(segment)
    }
}