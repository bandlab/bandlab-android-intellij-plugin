// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.dependencies.autocomplete

import com.bandlab.intellij.plugin.utils.writeTestFile
import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class GradleProjectCompletionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        project.writeTestFile(
            "gradle/all-projects.txt",
            ":\n:features:profile:api\n:features:profile:ui\n",
        )
        project.service<ProjectPathService>().invalidateCache()
    }

    fun testCompletionSuggestsDeclaredPathsAndExcludesCurrentProject() {
        // Put the build file in the project root so its own Gradle path is ":".
        val virtualFile =
            project.writeTestFile(
                "build.gradle.kts",
                "dependencies { implementation(project(\"<caret>\")) }",
            )
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
