package com.bandlab.intellij.plugin.precommit

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.EnvironmentUtil
import com.intellij.util.system.OS
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * Installs the project's pre-commit.com hook, silently and in the background. No dialog is ever
 * shown; the developer only sees a balloon on completion or failure.
 *
 * The check is cheap enough to run on every project open: it reads `.git/hooks/pre-commit` and
 * returns immediately if the pre-commit.com hook is already there. The install path only runs on
 * the first open of a fresh checkout (or after someone wipes the hook).
 *
 * Installing the `pre-commit` binary is OS-specific — Homebrew on macOS, pipx/pip on
 * Windows/Linux — so this picks the toolchain per [OS]. Every process runs with the captured
 * login-shell environment ([EnvironmentUtil]) so `brew`/`pipx` resolve even when the IDE was
 * launched from the dock/Start menu without a shell PATH.
 */
object PreCommitHookInstaller {

    private const val NOTIFICATION_GROUP = "BandLab Pre-commit"

    // pre-commit.com writes this marker into the hook it generates; its presence means the hook is
    // already wired up and we can skip everything.
    private const val PRE_COMMIT_MARKER = "pre-commit.com"

    fun installIfMissing(project: Project) {
        val basePath = project.basePath ?: return
        val projectRoot = Path.of(basePath)

        // Only act on a repo that opts into this hook — the config file is our signal that this is
        // the bandlab-android checkout and not some unrelated project the plugin also loads in.
        if (!Files.isRegularFile(projectRoot.resolve(".pre-commit-config.yaml"))) return

        val hookFile = projectRoot.resolve(".git").resolve("hooks").resolve("pre-commit")
        if (isHookInstalled(hookFile)) return

        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "Installing git pre-commit hook", false) {
                override fun run(indicator: ProgressIndicator) {
                    runInstall(project, projectRoot, indicator)
                }
            },
        )
    }

    private fun isHookInstalled(hookFile: Path): Boolean =
        Files.isRegularFile(hookFile) &&
            runCatching { Files.readString(hookFile).contains(PRE_COMMIT_MARKER) }.getOrDefault(false)

    private fun runInstall(project: Project, projectRoot: Path, indicator: ProgressIndicator) {
        // If `pre-commit` isn't on PATH yet, install it with the OS-appropriate package manager.
        if (!isOnPath("pre-commit")) {
            val installer = binaryInstallCommand()
            if (installer == null) {
                thisLogger().warn("No known package manager to install pre-commit on ${OS.CURRENT}; skipping.")
                return
            }
            indicator.text = "Installing pre-commit (${installer.first()})"
            val installed = exec(projectRoot, installer)
            if (!installed || !isOnPath("pre-commit")) {
                notify(
                    project,
                    "Couldn't install pre-commit automatically",
                    "Run `${installer.joinToString(" ")}` manually, then reopen the project.",
                    NotificationType.WARNING,
                )
                return
            }
        }

        // Wire the hook into .git/hooks/pre-commit for this checkout.
        indicator.text = "Registering pre-commit hook"
        val wired = exec(projectRoot, listOf("pre-commit", "install"))
        if (wired) {
            notify(
                project,
                "Pre-commit hook installed",
                "Configured git hooks now run automatically on commit.",
                NotificationType.INFORMATION,
            )
        } else {
            notify(
                project,
                "Couldn't register the pre-commit hook",
                "Run `pre-commit install` in the project root to finish setup.",
                NotificationType.WARNING,
            )
        }
    }

    /** The command that installs the `pre-commit` binary itself, or null if we can't on this OS. */
    private fun binaryInstallCommand(): List<String>? = when (OS.CURRENT) {
        OS.macOS -> listOf("brew", "install", "pre-commit")
        // pipx keeps pre-commit isolated and puts it on PATH; it's the recommended install per
        // pre-commit.com. Requires Python, which Windows/Linux dev machines have here.
        OS.Windows, OS.Linux -> listOf("pipx", "install", "pre-commit")
        else -> null
    }

    private fun isOnPath(binary: String): Boolean {
        val probe = if (OS.CURRENT == OS.Windows) listOf("where", binary) else listOf("which", binary)
        return exec(workDir = null, command = probe)
    }

    /** Runs [command], returning true on exit code 0. Uses the captured login-shell environment. */
    private fun exec(workDir: Path?, command: List<String>): Boolean = runCatching {
        val commandLine = GeneralCommandLine(command)
            .withCharset(StandardCharsets.UTF_8)
            .withEnvironment(EnvironmentUtil.getEnvironmentMap())
        if (workDir != null) commandLine.withWorkDirectory(workDir.toFile())

        val output = CapturingProcessHandler(commandLine).runProcess(PROCESS_TIMEOUT_MS)
        if (output.exitCode != 0) {
            thisLogger().warn("`${command.joinToString(" ")}` exited ${output.exitCode}: ${output.stderr.trim()}")
        }
        output.exitCode == 0
    }.getOrElse { e ->
        thisLogger().warn("Failed to run `${command.joinToString(" ")}`", e)
        false
    }

    private fun notify(project: Project, title: String, content: String, type: NotificationType) {
        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) return@invokeLater
            NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP)
                .createNotification(title, content, type)
                .notify(project)
        }
    }

    private const val PROCESS_TIMEOUT_MS = 120_000
}
