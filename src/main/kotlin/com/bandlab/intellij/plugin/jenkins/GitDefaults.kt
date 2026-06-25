package com.bandlab.intellij.plugin.jenkins

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.project.Project
import com.intellij.util.EnvironmentUtil
import java.nio.charset.StandardCharsets

/**
 * Resolves the current git `user.name` for the project, or null when git is unavailable / unset.
 *
 * Shells out to `git config user.name` (so it honors global + local config, unlike reading
 * `.git/config` directly) with the captured login-shell environment, mirroring how the rest of the
 * plugin runs external tools (see LocalizerRunner). Runs synchronously with a short timeout — call
 * it off any latency-sensitive path.
 *
 * The current git branch already has a dependency-free reader: `currentGitBranch` in the localizer
 * package.
 */
fun currentGitUser(project: Project): String? = gitConfig(project, "user.name")

/** The project's git `user.email`, or null when unset. */
fun currentGitEmail(project: Project): String? = gitConfig(project, "user.email")

private fun gitConfig(project: Project, key: String): String? {
    val basePath = project.basePath ?: return null
    return runCatching {
        val commandLine = GeneralCommandLine("git", "config", "--get", key)
            .withWorkDirectory(basePath)
            .withCharset(StandardCharsets.UTF_8)
            .withEnvironment(EnvironmentUtil.getEnvironmentMap())
        CapturingProcessHandler(commandLine).runProcess(5_000).stdout
            .trim()
            .takeIf { it.isNotEmpty() }
    }.getOrNull()
}
