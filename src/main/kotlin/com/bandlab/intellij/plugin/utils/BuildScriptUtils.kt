// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.utils

import com.bandlab.intellij.plugin.utils.Const.ALL_PROJECTS_PATH
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import java.io.File

private const val BUILD_GRADLE = "build.gradle"
private const val BUILD_GRADLE_KTS = "build.gradle.kts"
private const val ANDROID_LIBRARY_PLUGIN_ID = "bandlab.plugins.library.android"
private const val COMPOSE_PLUGIN_ID = "bandlab.plugins.compose"

/**
 * @returns `true` if the project is using Kotlin DSL. Default to 'false' As of now this is only
 *   determined by checking if the root setting file is `settings.gradle.kts`.
 */
internal fun Project.isUsingKts(): Boolean {
    val basePath = basePath ?: return false
    return File(basePath, "settings.gradle.kts").exists()
}

internal fun Project.buildScriptName(): String {
    return if (isUsingKts()) BUILD_GRADLE_KTS else BUILD_GRADLE
}

internal fun isBuildScriptFile(fileName: String?): Boolean {
    if (fileName == null) return false
    return fileName == BUILD_GRADLE || fileName == BUILD_GRADLE_KTS
}

internal fun Project.hasAllProjectsFile(): Boolean {
    val basePath = basePath ?: return false
    return VirtualFileManager.getInstance().findFileByUrl("file://$basePath$ALL_PROJECTS_PATH") !=
        null
}

/**
 * @returns `true` if the project is an Android module, `false` otherwise. This is determined by
 *   checking if the build script contains the Android library plugin id.
 */
internal fun Project.isAndroidModule(projectFolderPath: String): Boolean {
    val projectFolder = resolveProjectFolder(projectFolderPath)
    val buildScript = File(projectFolder, buildScriptName())
    if (!buildScript.exists()) return false
    return buildScript.readText().contains(ANDROID_LIBRARY_PLUGIN_ID)
}

/**
 * @returns `true` if the nearest Gradle module containing [moduleDir] has the Compose plugin
 *   applied.
 */
internal fun Project.hasComposePlugin(moduleDir: VirtualFile): Boolean {
    val projectRoot =
        basePath?.let { LocalFileSystem.getInstance().findFileByPath(it) } ?: return false
    val gradleProjectDir =
        GradleProjectUtils.findNearestGradleProject(projectRoot, moduleDir) ?: return false
    val buildScript = File(gradleProjectDir.path, buildScriptName())
    if (!buildScript.exists()) return false
    return buildScript.readText().contains(COMPOSE_PLUGIN_ID)
}

private fun Project.resolveProjectFolder(projectFolderPath: String): File {
    val requestedFolder = File(projectFolderPath)
    if (requestedFolder.isAbsolute) return requestedFolder

    val basePath = basePath ?: return requestedFolder
    return File(basePath, projectFolderPath)
}
