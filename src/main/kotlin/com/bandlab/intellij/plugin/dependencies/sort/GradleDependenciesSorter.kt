// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.dependencies.sort

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.exists

/**
 * Sorts the `plugins {}` and `dependencies {}` blocks of a `build.gradle(.kts)` with the
 * [square/gradle-dependencies-sorter] CLI.
 *
 * The CLI is a self-contained fat jar shipped under `<plugin>/cli/`, deliberately outside `lib/` so
 * the plugin classloader never sees its bundled Kotlin stdlib, ANTLR, etc. It is launched as an
 * external process on the IDE's own JRE, rewrites the file on disk, and we then reload the editor.
 *
 * `dependencies {}` is grouped by configuration; `plugins {}` is sorted alphabetically via `--block
 * plugins`. Comments are preserved and travel with the declaration that follows them.
 *
 * [square/gradle-dependencies-sorter]: https://github.com/square/gradle-dependencies-sorter
 */
object GradleDependenciesSorter {

    const val NOTIFICATION_GROUP = "BandLab Sort Dependencies"

    private const val PLUGIN_ID = "com.bandlab.intellij.plugin"
    private const val CLI_JAR = "cli/sort-gradle-dependencies.jar"
    private const val PROCESS_TIMEOUT_MS = 60_000

    /**
     * Sorts [buildScript] in the background. Unsaved editor changes are flushed to disk first so
     * the CLI sees them; on success the file is reloaded from disk, on failure a balloon shows the
     * CLI's error and the file is left as it was.
     */
    fun sort(project: Project, buildScript: VirtualFile) {
        val documentManager = FileDocumentManager.getInstance()
        documentManager.getCachedDocument(buildScript)?.let(documentManager::saveDocument)

        object : Task.Backgroundable(project, "Sort dependencies", false) {
                private var result: Result<ProcessOutput>? = null

                override fun run(indicator: ProgressIndicator) {
                    result = runCatching { runSortingCli(buildScript.toNioPath()) }
                }

                override fun onSuccess() {
                    val output = result?.getOrNull()
                    when {
                        output == null -> notifyError(project, result?.exceptionOrNull()?.message)
                        output.exitCode != 0 -> notifyError(project, output.errorMessage())
                        else -> reload(buildScript)
                    }
                }

                override fun onThrowable(error: Throwable) {
                    notifyError(project, error.message)
                }
            }
            .queue()
    }

    /**
     * Runs `java -jar sort-gradle-dependencies.jar --no-blank-lines --block plugins <file>` and
     * waits for it.
     */
    private fun runSortingCli(buildScript: Path): ProcessOutput {
        val jar = cliJar()
        check(jar.exists()) { "Sorter CLI not found at $jar" }

        val commandLine =
            GeneralCommandLine(
                    javaExecutable().toString(),
                    "-jar",
                    jar.toString(),
                    "--no-blank-lines",
                    "--block",
                    "plugins",
                    buildScript.toString(),
                )
                .withWorkDirectory(buildScript.parent.toFile())
                .withCharset(StandardCharsets.UTF_8)

        return CapturingProcessHandler(commandLine).runProcess(PROCESS_TIMEOUT_MS)
    }

    private fun cliJar(): Path {
        val plugin =
            checkNotNull(PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID))) {
                "Plugin $PLUGIN_ID is not loaded"
            }
        return plugin.pluginPath.resolve(CLI_JAR)
    }

    /** The JRE the IDE itself runs on: always present, no PATH lookup needed. */
    private fun javaExecutable(): Path {
        val executable = if (SystemInfo.isWindows) "java.exe" else "java"
        return Path.of(System.getProperty("java.home"), "bin", executable)
    }

    /**
     * Re-reads the file from disk and forces open editors to reload it. A plain VFS refresh only
     * updates the cache; `reloadFiles` is what updates the editor's Document.
     */
    private fun reload(file: VirtualFile) {
        VfsUtil.markDirtyAndRefresh(false, false, false, file)
        FileDocumentManager.getInstance().reloadFiles(file)
    }

    private fun ProcessOutput.errorMessage(): String {
        if (isTimeout) return "Sorter timed out after ${PROCESS_TIMEOUT_MS / 1000}s"
        return stderr
            .trim()
            .ifEmpty { stdout.trim() }
            .ifEmpty { "Sorter exited with code $exitCode" }
    }

    private fun notifyError(project: Project, message: String?) {
        val content = message ?: "Unknown error"
        thisLogger().warn("Sort dependencies failed: $content")
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP)
            .createNotification("Sort dependencies", content, NotificationType.ERROR)
            .notify(project)
    }
}
