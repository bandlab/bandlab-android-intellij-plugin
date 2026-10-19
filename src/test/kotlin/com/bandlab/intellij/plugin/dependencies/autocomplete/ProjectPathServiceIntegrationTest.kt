// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.dependencies.autocomplete

import com.bandlab.intellij.plugin.utils.writeTestFile
import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

class ProjectPathServiceIntegrationTest : BasePlatformTestCase() {

    private val service
        get() = project.service<ProjectPathService>()

    override fun setUp() {
        super.setUp()
        // The light project can be reused; remove the previous test's manifest through the VFS.
        val manifest = File(requireNotNull(project.basePath), "gradle/all-projects.txt")
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(manifest)?.let { file ->
            runWriteCommandAction(project) { file.delete(this) }
        }
        service.invalidateCache()
    }

    fun testLoadsPathsFromManifestIgnoringCommentsAndBlankLines() {
        writeManifest("# Selected modules\n:features:profile:api\n\n:features:profile:ui\n")

        assertThat(service.getProjectPaths())
            .containsExactly(":features:profile:api", ":features:profile:ui")
    }

    fun testValidatesOnlyPathsDeclaredInManifest() {
        writeManifest(":features:profile:api\n")

        assertThat(service.isValidProjectPath(":features:profile:api")).isTrue()
        assertThat(service.isValidProjectPath(":features:profile:impl")).isFalse()
        assertThat(service.isValidProjectPath("")).isFalse()
    }

    fun testRepeatedReadsReuseCachedPaths() {
        writeManifest(":features:profile:api\n")
        val initialPaths = service.getProjectPaths()

        assertThat(service.getProjectPaths()).isSameInstanceAs(initialPaths)
    }

    fun testExplicitInvalidationReloadsPaths() {
        writeManifest(":features:profile:api\n")
        val initialPaths = service.getProjectPaths()

        service.invalidateCache()

        val reloadedPaths = service.getProjectPaths()
        assertThat(reloadedPaths).containsExactlyElementsIn(initialPaths)
        assertThat(reloadedPaths).isNotSameInstanceAs(initialPaths)
    }

    fun testVfsChangeInvalidatesCachedPaths() {
        val manifest = writeManifest(":features:profile:api\n")
        assertThat(service.getProjectPaths()).containsExactly(":features:profile:api")

        runWriteCommandAction(project) {
            VfsUtil.saveText(manifest, ":features:profile:ui\n")
        }

        assertThat(service.getProjectPaths()).containsExactly(":features:profile:ui")
    }

    fun testMissingManifestReturnsNoPaths() {
        assertThat(service.getProjectPaths()).isEmpty()
        assertThat(service.isValidProjectPath(":features:profile:api")).isFalse()
    }

    private fun writeManifest(content: String): VirtualFile {
        return project.writeTestFile("gradle/all-projects.txt", content)
    }
}
