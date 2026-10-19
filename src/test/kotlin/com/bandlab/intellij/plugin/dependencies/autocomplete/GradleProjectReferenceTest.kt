// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.dependencies.autocomplete

import com.google.common.truth.Truth.assertThat
import com.intellij.util.io.delete
import kotlin.io.path.createFile
import kotlin.io.path.name
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GradleProjectReferenceTest {

    @get:Rule val tmpFolder = TemporaryFolder()

    @Test
    fun `findBuildFile prefers Kotlin then Groovy and returns null when neither exists`() {
        // Create a temporary directory for testing
        val tempDir = tmpFolder.newFolder("test-project").toPath()

        // Test when both files exist - should prefer .kts
        val buildFileKts = tempDir.resolve("build.gradle.kts")
        val buildFile = tempDir.resolve("build.gradle")
        buildFileKts.createFile()
        buildFile.createFile()

        val bothExistResult = findBuildFile(tempDir)
        assertThat(bothExistResult?.name).isEqualTo("build.gradle.kts")

        // Clean up for next test
        buildFileKts.delete()
        buildFile.delete()

        // Test when only Groovy exists
        buildFile.createFile()
        val onlyGroovyResult = findBuildFile(tempDir)
        assertThat(onlyGroovyResult?.name).isEqualTo("build.gradle")

        // Clean up for next test
        buildFile.delete()

        // Test when only Kotlin exists
        buildFileKts.createFile()
        val onlyKotlinResult = findBuildFile(tempDir)
        assertThat(onlyKotlinResult?.name).isEqualTo("build.gradle.kts")

        // Clean up for next test
        buildFileKts.delete()

        // Test when neither exists
        val neitherExistsResult = findBuildFile(tempDir)
        assertThat(neitherExistsResult).isNull()
    }

    @Test
    fun `reference text ranges exclude quotes correctly`() {
        // Test the core logic of text range calculation
        val doubleQuoted = "\":platforms:intellij:skate\""
        val singleQuoted = "':tools:cli'"
        val unquoted = ":raw:path"

        // Test that quoted strings exclude the quotes in the range
        val doubleQuotedRange = calculateReferenceTextRange(doubleQuoted)
        assertThat(doubleQuotedRange.startOffset).isEqualTo(1) // After opening quote
        assertThat(doubleQuotedRange.endOffset)
            .isEqualTo(doubleQuoted.length - 1) // Before closing quote

        val singleQuotedRange = calculateReferenceTextRange(singleQuoted)
        assertThat(singleQuotedRange.startOffset).isEqualTo(1) // After opening quote
        assertThat(singleQuotedRange.endOffset)
            .isEqualTo(singleQuoted.length - 1) // Before closing quote

        // Test that unquoted strings use the entire range
        val unquotedRange = calculateReferenceTextRange(unquoted)
        assertThat(unquotedRange.startOffset).isEqualTo(0) // From beginning
        assertThat(unquotedRange.endOffset).isEqualTo(unquoted.length) // To end
    }
}
