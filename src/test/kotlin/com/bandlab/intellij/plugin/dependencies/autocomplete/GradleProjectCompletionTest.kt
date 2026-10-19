// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.dependencies.autocomplete

import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

class GradleProjectCompletionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        val manifest = File(requireNotNull(project.basePath), "gradle/all-projects.txt")
        manifest.parentFile.mkdirs()
        manifest.writeText(":\n:features:profile:api\n:features:profile:ui\n")
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(manifest)
        project.service<ProjectPathService>().invalidateCache()
    }

    fun testCompletionSuggestsDeclaredPathsAndExcludesCurrentProject() {
        // Put the build file in the project root so its own Gradle path is ":".
        val buildFile = File(requireNotNull(project.basePath), "build.gradle.kts")
        buildFile.writeText("dependencies { implementation(project(\"<caret>\")) }")
        val virtualFile =
            requireNotNull(LocalFileSystem.getInstance().refreshAndFindFileByIoFile(buildFile))
        myFixture.configureFromExistingVirtualFile(virtualFile)

        val suggestions = myFixture.completeBasic().orEmpty().map { it.lookupString }

        assertThat(suggestions).containsExactly(":features:profile:api", ":features:profile:ui")
    }

    fun testCompletionDoesNotSuggestProjectPathsOutsideProjectCall() {
        myFixture.configureByText(
            "build.gradle.kts",
            "dependencies { implementation(\"<caret>\") }",
        )

        val suggestions = myFixture.completeBasic().orEmpty().map { it.lookupString }

        assertThat(suggestions).doesNotContain(":features:profile:api")
        assertThat(suggestions).doesNotContain(":features:profile:ui")
    }
}
