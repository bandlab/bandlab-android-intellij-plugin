// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.dependencies.sort

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.EnvironmentUtil
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * Formats a `build.gradle(.kts)` with the project's own `misc/scripts/gradle-fmt` wrapper
 * ([bandlab/gradle-fmt]) with the rules from `gradle-fmt.toml` next to the script.
 */
object GradleFmtRunner {

    const val NOTIFICATION_GROUP = "BandLab Sort Dependencies"

    private const val SCRIPT_PATH = "misc/scripts/gradle-fmt"
    private const val PROCESS_TIMEOUT_MS = 5_000

    fun scriptOrNull(project: Project): Path? {
        val basePath = project.basePath ?: return null
        return Path.of(basePath, SCRIPT_PATH).takeIf(Files::isRegularFile)
    }

    /**
     * Formats [buildScript] in the background. Unsaved editor changes are flushed to disk first so
     * the formatter sees them. On success the file is reloaded from disk; warnings the formatter
     * printed are shown in a balloon. On failure a balloon shows the error and the file is left as
     * it was.
     */
    fun format(project: Project, buildScript: VirtualFile) {
        val script = scriptOrNull(project) ?: return
        val projectRoot = Path.of(requireNotNull(project.basePath))
        val documentManager = FileDocumentManager.getInstance()
        documentManager.getCachedDocument(buildScript)?.let(documentManager::saveDocument)

        object : Task.Backgroundable(project, "Sort dependencies", false) {
                private var result: Result<ProcessOutput>? = null

                override fun run(indicator: ProgressIndicator) {
                    result = runCatching { runScript(projectRoot, script, buildScript.toNioPath()) }
                }

                override fun onSuccess() {
                    val output = result?.getOrNull()
                    when {
                        output == null -> notifyError(project, result?.exceptionOrNull()?.message)
                        output.exitCode != 0 -> notifyError(project, output.errorMessage())
                        else -> {
                            reload(buildScript)
                            output.stderr
                                .trim()
                                .ifEmpty { null }
                                ?.let { notifyWarning(project, it) }
                        }
                    }
                }

                override fun onThrowable(error: Throwable) {
                    notifyError(project, error.message)
                }
            }
            .queue()
    }

    private fun runScript(projectRoot: Path, script: Path, buildScript: Path): ProcessOutput {
        val commandLine =
            GeneralCommandLine(script.toString(), buildScript.toString())
                .withWorkDirectory(projectRoot.toFile())
                .withCharset(StandardCharsets.UTF_8)
                .withEnvironment(EnvironmentUtil.getEnvironmentMap())

        return CapturingProcessHandler(commandLine).runProcess(PROCESS_TIMEOUT_MS)
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
        if (isTimeout) return "gradle-fmt timed out after ${PROCESS_TIMEOUT_MS / 1000}s"
        return stderr
            .trim()
            .ifEmpty { stdout.trim() }
            .ifEmpty { "gradle-fmt exited with code $exitCode" }
    }

    private fun notifyWarning(project: Project, message: String) {
        thisLogger().info("gradle-fmt warnings: $message")
        notify(project, message, NotificationType.WARNING)
    }

    private fun notifyError(project: Project, message: String?) {
        val content = message ?: "Unknown error"
        thisLogger().warn("Sort dependencies failed: $content")
        notify(project, content, NotificationType.ERROR)
    }

    private fun notify(project: Project, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP)
            .createNotification("Sort dependencies", content, type)
            .notify(project)
    }
}
