// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.localizer

import com.google.common.truth.Truth.assertThat
import java.nio.file.Files
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GitBranchTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun regularGitDir_featureBranch() {
        val root = tmp.newFolder("repo").toPath()
        val gitDir = Files.createDirectory(root.resolve(".git"))
        Files.writeString(gitDir.resolve("HEAD"), "ref: refs/heads/feature/my-feature\n")

        assertThat(currentGitBranchFromRoot(root)).isEqualTo("feature/my-feature")
    }

    @Test
    fun regularGitDir_mainBranch() {
        val root = tmp.newFolder("main-repo").toPath()
        val gitDir = Files.createDirectory(root.resolve(".git"))
        Files.writeString(gitDir.resolve("HEAD"), "ref: refs/heads/master\n")

        assertThat(currentGitBranchFromRoot(root)).isEqualTo("master")
    }

    @Test
    fun regularGitDir_detachedHead_returnsNull() {
        val root = tmp.newFolder("detached").toPath()
        val gitDir = Files.createDirectory(root.resolve(".git"))
        Files.writeString(gitDir.resolve("HEAD"), "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2\n")

        assertThat(currentGitBranchFromRoot(root)).isNull()
    }

    @Test
    fun worktreeGitFile_absoluteGitdir() {
        // Simulate a git worktree: .git is a file containing "gitdir: <absolute path>"
        val mainGitDir = tmp.newFolder("main-git").toPath()
        Files.writeString(mainGitDir.resolve("HEAD"), "ref: refs/heads/feature/wt-branch\n")

        val worktreeRoot = tmp.newFolder("worktree").toPath()
        Files.writeString(worktreeRoot.resolve(".git"), "gitdir: ${mainGitDir.toAbsolutePath()}\n")

        assertThat(currentGitBranchFromRoot(worktreeRoot)).isEqualTo("feature/wt-branch")
    }

    @Test
    fun worktreeGitFile_relativeGitdir() {
        // "gitdir: real-git" — path relative to the worktree root
        val root = tmp.newFolder("relative-wt").toPath()
        val gitWorktreesDir = Files.createDirectories(root.resolve("real-git"))
        Files.writeString(gitWorktreesDir.resolve("HEAD"), "ref: refs/heads/relative-branch\n")

        Files.writeString(root.resolve(".git"), "gitdir: real-git\n")

        assertThat(currentGitBranchFromRoot(root)).isEqualTo("relative-branch")
    }

    @Test
    fun missingDotGit_returnsNull() {
        val root = tmp.newFolder("no-git").toPath()
        assertThat(currentGitBranchFromRoot(root)).isNull()
    }
}
