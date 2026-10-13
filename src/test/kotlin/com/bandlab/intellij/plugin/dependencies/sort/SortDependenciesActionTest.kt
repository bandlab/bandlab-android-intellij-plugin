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

class SortDependenciesActionTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = File(".").absolutePath

    fun testUpdateIsVisibleOnlyForBuildScripts() {
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

    fun testActionPerformedSortsPluginsAndDependenciesAndKeepsComments() {
        val action = SortDependenciesAction()
        val buildFile =
            createProjectFile(
                "feature-profile/screen/build.gradle.kts",
                unsortedBuildGradleContent(),
            )
        action.actionPerformed(createEvent(action, buildFile))
        assertThat(project.readFile(buildFile.path, isAbsolute = true))
            .isEqualTo(expectedBuildGradleContent())
    }

    fun testActionPerformedSortsGroovyBuildScript() {
        val action = SortDependenciesAction()
        val buildFile =
            createProjectFile(
                "legacy/build.gradle",
                """
                dependencies {
                    implementation project(':zeta')
                    api project(':alpha')
                }
                """
                    .trimIndent() + "\n",
            )
        action.actionPerformed(createEvent(action, buildFile))
        assertThat(project.readFile(buildFile.path, isAbsolute = true))
            .isEqualTo(
                """
                dependencies {
                    api project(':alpha')
                    implementation project(':zeta')
                }
                """
                    .trimIndent() + "\n"
            )
    }

    fun testActionPerformedSortsPluginsWithoutModuleTypePlugin() {
        val action = SortDependenciesAction()
        val buildFile =
            createProjectFile(
                "tooling/build.gradle.kts",
                """
                plugins {
                    alias(libs.plugins.kover)
                    `java-library`
                    alias(libs.plugins.changelog)
                }
                """
                    .trimIndent() + "\n",
            )
        action.actionPerformed(createEvent(action, buildFile))
        assertThat(project.readFile(buildFile.path, isAbsolute = true))
            .isEqualTo(
                """
                plugins {
                    `java-library`
                    alias(libs.plugins.changelog)
                    alias(libs.plugins.kover)
                }
                """
                    .trimIndent() + "\n"
            )
    }

    fun testActionPerformedKeepsAlreadySortedFileUntouched() {
        val action = SortDependenciesAction()
        val buildFile =
            createProjectFile(
                "feature-profile/screen/build.gradle.kts",
                expectedBuildGradleContent(),
            )
        action.actionPerformed(createEvent(action, buildFile))
        assertThat(project.readFile(buildFile.path, isAbsolute = true))
            .isEqualTo(expectedBuildGradleContent())
    }

    fun testActionPerformedLeavesUnparsableFileUntouched() {
        val action = SortDependenciesAction()
        val content = "dependencies {\n    implementation(\n"
        val buildFile = createProjectFile("broken/build.gradle.kts", content)
        action.actionPerformed(createEvent(action, buildFile))
        assertThat(project.readFile(buildFile.path, isAbsolute = true)).isEqualTo(content)
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
        val ioFile = File(requireNotNull(project.basePath), relativePath)
        ioFile.parentFile.mkdirs()
        ioFile.writeText(content)
        val file = requireNotNull(LocalFileSystem.getInstance().refreshAndFindFileByIoFile(ioFile))
        // The light project is shared between tests, so the file (and its Document) may already be
        // cached from an earlier test: force the VFS to pick up what was just written to disk.
        VfsUtil.markDirtyAndRefresh(false, false, false, file)
        return file
    }

    private fun unsortedBuildGradleContent(): String =
        """
        plugins {
            alias(bandlab.plugins.metro)
            alias(bandlab.plugins.compose)
            alias(bandlab.plugins.library.kotlin)
            alias(bandlab.plugins.compose)
        }

        dependencies {
            // zeta is needed for X
            implementation(project(":zeta"))
            api(project(":alpha"))
            compileOnly(project(":beta"))
            testImplementation(libs.junit)
        }
        """
            .trimIndent() + "\n"

    private fun expectedBuildGradleContent(): String =
        """
        plugins {
            alias(bandlab.plugins.library.kotlin)
            alias(bandlab.plugins.compose)
            alias(bandlab.plugins.metro)
        }

        dependencies {
            api(project(":alpha"))
            // zeta is needed for X
            implementation(project(":zeta"))
            compileOnly(project(":beta"))
            testImplementation(libs.junit)
        }
        """
            .trimIndent() + "\n"
}
