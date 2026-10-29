// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.dependencies.sort

import com.bandlab.intellij.plugin.utils.readFile
import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

/**
 * The action shells out to the project's `misc/scripts/gradle-fmt`; these tests replace it with a
 * bash stub that records how it was called and rewrites the build script. The real formatting is
 * covered by the gradle-fmt repository.
 */
class SortDependenciesActionTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = File(".").absolutePath

    private val projectRoot: File
        get() = File(requireNotNull(project.basePath))

    private val stubScript: File
        get() = File(projectRoot, "misc/scripts/gradle-fmt")

    private val invocationLog: File
        get() = File(projectRoot, "misc/scripts/gradle-fmt-invocation.log")

    override fun tearDown() {
        try {
            // The light project is shared between tests: never leak the stub into the next one.
            stubScript.delete()
            invocationLog.delete()
        } finally {
            super.tearDown()
        }
    }

    fun testUpdateIsHiddenWhenGradleFmtScriptIsMissing() {
        val action = SortDependenciesAction()
        val buildFile = createProjectFile("feature/build.gradle.kts", "plugins {}")
        val event = createEvent(action, buildFile)
        action.update(event)
        assertThat(event.presentation.isEnabledAndVisible).isFalse()
    }

    fun testUpdateIsVisibleOnlyForBuildScriptsWhenGradleFmtScriptExists() {
        installStub("exit 0")
        val action = SortDependenciesAction()
        val kotlinBuildFile = createProjectFile("feature/build.gradle.kts", "plugins {}")
        val groovyBuildFile = createProjectFile("legacy/build.gradle", "plugins {}")
        val regularFile = createProjectFile("feature/README.md", "# docs")
        val kotlinEvent = createEvent(action, kotlinBuildFile)
        action.update(kotlinEvent)
        assertThat(kotlinEvent.presentation.isEnabledAndVisible).isTrue()
        val groovyEvent = createEvent(action, groovyBuildFile)
        action.update(groovyEvent)
        assertThat(groovyEvent.presentation.isEnabledAndVisible).isTrue()
        val regularEvent = createEvent(action, regularFile)
        action.update(regularEvent)
        assertThat(regularEvent.presentation.isEnabledAndVisible).isFalse()
    }

    fun testActionPerformedRunsGradleFmtOnBuildScriptFromProjectRoot() {
        installStub(
            """
            printf '%s\n' "${'$'}PWD" "${'$'}@" > "${invocationLog.path}"
            printf '%s\n' 'dependencies {}' > "${'$'}1"
            """
                .trimIndent(),
        )
        val action = SortDependenciesAction()
        val buildFile = createProjectFile("feature/build.gradle.kts", "plugins {}\n")
        action.actionPerformed(createEvent(action, buildFile))
        assertThat(invocationLog.readLines())
            .containsExactly(projectRoot.canonicalPath, File(buildFile.path).canonicalPath)
            .inOrder()
        assertThat(project.readFile(buildFile.path, isAbsolute = true))
            .isEqualTo("dependencies {}\n")
    }

    fun testActionPerformedReloadsBuildScriptWhenGradleFmtWarns() {
        installStub(
            """
            echo "warning: unknown code left as written" >&2
            printf '%s\n' 'dependencies {}' > "${'$'}1"
            """
                .trimIndent(),
        )
        val action = SortDependenciesAction()
        val buildFile = createProjectFile("feature/build.gradle.kts", "plugins {}\n")
        action.actionPerformed(createEvent(action, buildFile))
        assertThat(project.readFile(buildFile.path, isAbsolute = true))
            .isEqualTo("dependencies {}\n")
    }

    fun testActionPerformedLeavesBuildScriptUntouchedWhenGradleFmtFails() {
        installStub(
            """
            echo "build.gradle.kts: could not be parsed" >&2
            exit 2
            """
                .trimIndent(),
        )
        val action = SortDependenciesAction()
        val content = "dependencies {\n    implementation(\n"
        val buildFile = createProjectFile("broken/build.gradle.kts", content)
        action.actionPerformed(createEvent(action, buildFile))
        assertThat(project.readFile(buildFile.path, isAbsolute = true)).isEqualTo(content)
    }

    /** Writes an executable `misc/scripts/gradle-fmt` whose body is [body]. */
    private fun installStub(body: String) {
        stubScript.parentFile.mkdirs()
        stubScript.writeText("#!/usr/bin/env bash\nset -eu\n$body\n")
        check(stubScript.setExecutable(true))
    }

    private fun createEvent(
        action: SortDependenciesAction,
        file: VirtualFile,
    ): AnActionEvent {
        val psiFile = requireNotNull(PsiManager.getInstance(project).findFile(file))
        return TestActionEvent.createTestEvent(
            action,
            SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, project)
                .add(CommonDataKeys.PSI_ELEMENT, psiFile)
                .add(CommonDataKeys.PSI_FILE, psiFile)
                .add(CommonDataKeys.VIRTUAL_FILE, file)
                .build(),
        )
    }

    private fun createProjectFile(relativePath: String, content: String): VirtualFile {
        val ioFile = File(projectRoot, relativePath)
        ioFile.parentFile.mkdirs()
        ioFile.writeText(content)
        val file = requireNotNull(LocalFileSystem.getInstance().refreshAndFindFileByIoFile(ioFile))
        // The light project is shared between tests, so the file (and its Document) may already be
        // cached from an earlier test: force the VFS to pick up what was just written to disk.
        VfsUtil.markDirtyAndRefresh(false, false, false, file)
        return file
    }
}
