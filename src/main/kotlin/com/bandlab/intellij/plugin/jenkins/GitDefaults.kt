package com.bandlab.intellij.plugin.jenkins

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.project.Project
import com.intellij.util.EnvironmentUtil
import java.nio.charset.StandardCharsets

/**
 * The GitHub owner of the remote the current branch tracks — i.e. the fork account that holds the
 * branch.
 */
fun currentGitForkOwner(project: Project): String? {
    val remote = currentBranchRemote(project) ?: return null
    val url = runGit(project, "remote", "get-url", remote) ?: return null
    return gitHubOwner(url)
}

/** The project's git `user.email`, or null when unset. */
fun currentGitEmail(project: Project): String? = runGit(project, "config", "--get", "user.email")

/** Remote the current branch tracks (e.g. `origin` out of `origin/master`), or null when unset. */
private fun currentBranchRemote(project: Project): String? =
    runGit(project, "rev-parse", "--abbrev-ref", "--symbolic-full-name", "@{u}")
        ?.substringBefore('/')
        ?.takeIf { it.isNotEmpty() }

/** The owner segment of a GitHub remote URL (SSH or HTTPS), or null when it isn't a GitHub URL. */
private fun gitHubOwner(remoteUrl: String): String? {
    if ("github.com" !in remoteUrl) return null
    return remoteUrl.substringAfter("github.com")
        .trimStart(':', '/')
        .substringBefore('/')
        .takeIf { it.isNotEmpty() }
}

/**
 * Runs `git <args>` in the project root with the captured login-shell environment (mirroring how the
 * rest of the plugin runs external tools — see LocalizerRunner). Returns trimmed stdout, or null on
 * any error / empty output. Synchronous with a short timeout — keep it off latency-sensitive paths.
 */
private fun runGit(project: Project, vararg args: String): String? {
    val basePath = project.basePath ?: return null
    return runCatching {
        // List constructor (as in LocalizerRunner) — the proven form in this codebase.
        val commandLine = GeneralCommandLine(listOf("git", *args))
            .withWorkDirectory(basePath)
            .withCharset(StandardCharsets.UTF_8)
            .withEnvironment(EnvironmentUtil.getEnvironmentMap())
        val output = CapturingProcessHandler(commandLine).runProcess(5_000)
        output.stdout.trim().takeIf { it.isNotEmpty() }
    }.getOrNull()
}
