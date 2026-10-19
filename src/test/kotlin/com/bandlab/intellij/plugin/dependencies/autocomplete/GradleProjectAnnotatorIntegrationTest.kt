// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.dependencies.autocomplete

import com.bandlab.intellij.plugin.utils.writeTestFile
import com.google.common.truth.Truth.assertThat
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class GradleProjectAnnotatorIntegrationTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        // Validation is enabled by the Android project's generated module list, not Gradle sync.
        project.writeTestFile("gradle/all-projects.txt", ":features:profile:api\n")
        project.service<ProjectPathService>().invalidateCache()
    }

    fun testUnknownPathIsHighlightedOnceAndKnownPathIsAccepted() {
        // Groovy supports highlighting without a synced Gradle model in this light fixture.
        myFixture.configureByText(
            "build.gradle",
            """
            dependencies {
                implementation(project(":features:profile:api"))
                implementation(project(":features:missing:api"))
            }
            """
                .trimIndent(),
        )

        // Other Gradle/Groovy inspections may report unresolved DSL symbols in this minimal
        // fixture.
        val pathErrors =
            myFixture.doHighlighting(HighlightSeverity.ERROR).filter {
                it.description?.startsWith("Unknown project path:") == true
            }

        assertThat(pathErrors.map { it.description })
            .containsExactly("Unknown project path: ':features:missing:api'")
        val error = pathErrors.single()
        assertThat(myFixture.file.text.substring(error.startOffset, error.endOffset))
            .isEqualTo(":features:missing:api")
    }
}
