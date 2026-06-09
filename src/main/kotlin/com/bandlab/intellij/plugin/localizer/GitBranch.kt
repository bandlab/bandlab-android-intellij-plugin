package com.bandlab.intellij.plugin.localizer

import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Path

/**
 * Returns the current git branch for the project's working tree, or null when the HEAD is detached,
 * git is absent, or any IO error occurs.
 *
 * Works for both regular clones (`.git` is a directory) and git worktrees (`.git` is a file that
 * points to the real git directory).
 */
fun currentGitBranch(project: Project): String? {
    val root = project.basePath?.let(Path::of) ?: return null
    return currentGitBranchFromRoot(root)
}

/**
 * Resolves the git branch from an explicit working-tree root path. Exposed internally so tests can
 * call it directly with a temp directory without needing a real [Project].
 */
internal fun currentGitBranchFromRoot(root: Path): String? = runCatching {
    val headFile = resolveHeadFile(root)
    val firstLine = if (headFile != null) Files.readAllLines(headFile).firstOrNull() else null
    if (firstLine != null) parseRefLine(firstLine) else null
}.getOrNull()

private fun resolveHeadFile(root: Path): Path? {
    val dotGit = root.resolve(".git")
    return when {
        Files.isDirectory(dotGit) -> dotGit.resolve("HEAD")
        Files.isRegularFile(dotGit) -> {
            // Worktree: .git file contains "gitdir: <path>"
            val line = Files.readAllLines(dotGit).firstOrNull() ?: return null
            val prefix = "gitdir:"
            if (!line.startsWith(prefix)) return null
            val gitDirStr = line.removePrefix(prefix).trim()
            val gitDirPath = Path.of(gitDirStr).let { p ->
                if (p.isAbsolute) p else root.resolve(p).normalize()
            }
            gitDirPath.resolve("HEAD")
        }
        else -> null
    }
}

/** Parses `ref: refs/heads/<branch>` → `<branch>`, or null for a detached HEAD (raw SHA). */
private fun parseRefLine(line: String): String? {
    val refPrefix = "ref: refs/heads/"
    if (!line.startsWith(refPrefix)) return null
    return line.removePrefix(refPrefix).trim()
}
