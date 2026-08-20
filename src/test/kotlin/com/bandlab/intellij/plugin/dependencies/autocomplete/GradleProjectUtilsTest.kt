// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.dependencies.autocomplete

import com.bandlab.intellij.plugin.utils.GradleProjectUtils
import com.google.common.truth.Truth.assertThat
import com.intellij.mock.MockProject
import com.intellij.mock.MockVirtualFile
import com.intellij.openapi.util.Disposer
import org.junit.Test

class GradleProjectUtilsTest {
    @Test
    fun `findNearestGradleProject returns current directory if it is a Gradle project`() {
        val root = MockVirtualFile(true, "root")
        val gradleDir = MockVirtualFile(true, "gradleProject")
        gradleDir.addChild(MockVirtualFile(false, "build.gradle"))
        root.addChild(gradleDir)

        assertThat(GradleProjectUtils.findNearestGradleProject(root, gradleDir))
            .isEqualTo(gradleDir)
    }

    @Test
    fun `findNearestGradleProject returns nearest parent Gradle project`() {
        val root = MockVirtualFile(true, "root")
        val gradleDir = MockVirtualFile(true, "gradleProject")
        gradleDir.addChild(MockVirtualFile(false, "build.gradle"))
        val subDir = MockVirtualFile(true, "subDir")
        gradleDir.addChild(subDir)
        root.addChild(gradleDir)

        assertThat(GradleProjectUtils.findNearestGradleProject(root, subDir)).isEqualTo(gradleDir)
    }

    @Test
    fun `findNearestGradleProject returns null if no Gradle project is found`() {
        val root = MockVirtualFile(true, "root")
        val subDir = MockVirtualFile(true, "subDir")
        subDir.addChild(MockVirtualFile(false, "someFile.txt"))
        root.addChild(subDir)

        assertThat(GradleProjectUtils.findNearestGradleProject(root, subDir)).isNull()
    }

    @Test
    fun `findNearestGradleProject returns root if it is a Gradle project`() {
        val root = MockVirtualFile(true, "root")
        root.addChild(MockVirtualFile(false, "build.gradle"))
        val subDir = MockVirtualFile(true, "subDir")
        root.addChild(subDir)

        assertThat(GradleProjectUtils.findNearestGradleProject(root, subDir)).isEqualTo(root)
    }

    @Test
    fun `isGradleProject returns true when directory contains build_gradle`() {
        val exampleDir = MockVirtualFile(true, "exampleDir")
        exampleDir.addChild(MockVirtualFile(false, "build.gradle"))
        exampleDir.addChild(MockVirtualFile(false, "other_file.txt"))

        assertThat(GradleProjectUtils.isGradleProject(exampleDir)).isTrue()
    }

    @Test
    fun `isGradleProject returns true when directory contains build_gradle_kts`() {
        val exampleDir = MockVirtualFile(true, "exampleDir")
        exampleDir.addChild(MockVirtualFile(false, "build.gradle.kts"))
        exampleDir.addChild(MockVirtualFile(false, "some_file.txt"))

        assertThat(GradleProjectUtils.isGradleProject(exampleDir)).isTrue()
    }

    @Test
    fun `isGradleProject returns false for directory with no gradle build files`() {
        val exampleDir = MockVirtualFile(true, "exampleDir")
        exampleDir.addChild(MockVirtualFile(false, "file1.txt"))
        exampleDir.addChild(MockVirtualFile(false, "file2.doc"))

        assertThat(GradleProjectUtils.isGradleProject(exampleDir)).isFalse()
    }

    @Test
    fun `isGradleProject returns false when VirtualFile is not a directory`() {
        val exampleFile = MockVirtualFile(false, "exampleFile")

        assertThat(GradleProjectUtils.isGradleProject(exampleFile)).isFalse()
    }

    @Test
    fun `isGradleProject returns false when directory has no children`() {
        val exampleDir = MockVirtualFile(true, "exampleDir")

        assertThat(GradleProjectUtils.isGradleProject(exampleDir)).isFalse()
    }

    @Test
    fun `getGradleProjectPath returns root path when base path matches directory`() {
        val rootDir = MockVirtualFile(true, "rootDir")
        val project = createProject(rootDir)

        val directory = MockVirtualFile(true, "rootDir")

        assertThat(GradleProjectUtils.getGradleProjectPath(project, directory)).isEqualTo(":")
    }

    @Test
    fun `getGradleProjectPath returns correct path for nested directory`() {
        val rootDir = MockVirtualFile(true, "rootDir")
        val project = createProject(rootDir)

        val nestedDir = MockVirtualFile(true, "nested")
        val projectDir = MockVirtualFile(true, "project")
        nestedDir.addChild(projectDir)
        rootDir.addChild(nestedDir)

        assertThat(GradleProjectUtils.getGradleProjectPath(project, projectDir))
            .isEqualTo(":nested:project")
    }

    @Test
    fun `getGradleProjectPath returns null for directory outside project base path`() {
        val rootDir = MockVirtualFile(true, "rootDir")
        val project = createProject(rootDir)

        val externalDirectory = MockVirtualFile(true, "externalDirectory")

        assertThat(GradleProjectUtils.getGradleProjectPath(project, externalDirectory)).isNull()
    }

    @Test
    fun `getGradleProjectAccessorPath returns camelCase accessor for hyphenated module`() {
        val rootDir = MockVirtualFile(true, "rootDir")
        val project = createProject(rootDir)

        // create a sample mock gradle project: libraries/sample-result/test
        val librariesDir = MockVirtualFile(true, "libraries")
        val sampleResultDir = MockVirtualFile(true, "sample-result")
        val testDir = MockVirtualFile(true, "test")

        testDir.addChild(MockVirtualFile(false, "build.gradle.kts"))
        sampleResultDir.addChild(testDir)
        librariesDir.addChild(sampleResultDir)
        rootDir.addChild(librariesDir)

        val result = GradleProjectUtils.getGradleProjectAccessorPath(project, testDir)

        assertThat(result).isEqualTo("projects.libraries.sampleResult.test")
    }

    @Suppress("UnstableApiUsage")
    private fun createProject(baseDir: MockVirtualFile): MockProject {
        return object : MockProject(null, Disposer.newDisposable()) {
            override fun getBasePath(): String = baseDir.path
        }
    }
}
