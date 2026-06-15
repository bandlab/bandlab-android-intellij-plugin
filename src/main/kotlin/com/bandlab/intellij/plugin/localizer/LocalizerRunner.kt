package com.bandlab.intellij.plugin.localizer

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.EnvironmentUtil
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/**
 * Runs the project's `bandlab-localizer` wrapper as a tracked process, streaming output into a Run
 * Console tab. The plugin shells out to the CLI for every string-file operation — it never edits
 * resources directly.
 *
 * Running the process ourselves (vs. handing a line to a terminal) buys two things: the captured
 * login-shell environment ([EnvironmentUtil]) so the wrapper finds `java` even when the IDE was
 * launched from the dock without a shell PATH; and a completion signal — when the process ends we
 * refresh the affected files' VFS so open editors reload the CLI's changes.
 */
object LocalizerRunner {

    /**
     * Runs `bandlab-localizer <args>`. On completion, refreshes [refresh] from disk so the editor
     * picks up what the CLI wrote.
     */
    fun run(project: Project, title: String, args: List<String>, refresh: List<Path> = emptyList()) {
        val basePath = project.basePath ?: return
        val wrapper = Path.of(basePath, "localizer", "bandlab-localizer").toString()

        val commandLine = GeneralCommandLine(listOf(wrapper) + args)
            .withWorkDirectory(basePath)
            .withCharset(StandardCharsets.UTF_8)
            .withEnvironment(EnvironmentUtil.getEnvironmentMap())

        val handler = try {
            KillableColoredProcessHandler(commandLine)
        } catch (e: ExecutionException) {
            Messages.showErrorDialog(project, e.message ?: "Failed to run bandlab-localizer", "Localizer: $title")
            return
        }

        val console = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
        console.attachToProcess(handler)

        if (refresh.isNotEmpty()) {
            handler.addProcessListener(object : ProcessListener {
                override fun processTerminated(event: ProcessEvent) {
                    ApplicationManager.getApplication().invokeLater {
                        if (project.isDisposed) return@invokeLater
                        val lfs = LocalFileSystem.getInstance()
                        val files = refresh.mapNotNull { runCatching { lfs.refreshAndFindFileByNioFile(it) }.getOrNull() }
                        if (files.isEmpty()) return@invokeLater
                        // Re-stat + re-read from disk, then force open editors to reload the new
                        // content (a plain VFS refresh updates the cache but doesn't reload the
                        // editor's Document — reloadFiles does).
                        VfsUtil.markDirtyAndRefresh(false, false, false, *files.toTypedArray())
                        FileDocumentManager.getInstance().reloadFiles(*files.toTypedArray())
                    }
                }
            })
        }

        val descriptor = RunContentDescriptor(console, handler, console.component, "Localizer: $title")
        RunContentManager.getInstance(project).showRunContent(DefaultRunExecutor.getRunExecutorInstance(), descriptor)
        handler.startNotify()
    }
}
