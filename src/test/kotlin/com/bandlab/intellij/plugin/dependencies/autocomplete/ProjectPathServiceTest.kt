// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.dependencies.autocomplete

import com.bandlab.intellij.plugin.utils.GradleProjectUtils.parseProjectPaths
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProjectPathServiceTest {

    @Test
    fun `parseProjectPaths filters comments and blank lines`() {
        val content =
            """
            # This is a comment
            :platforms:gradle:foundry-gradle-plugin
            :platforms:intellij:skate

            # Another comment
            :tools:cli
            :tools:foundry-common
            """
                .trimIndent()

        @Suppress("UNCHECKED_CAST") val result = parseProjectPaths(content)

        assertThat(result)
            .containsExactly(
                ":platforms:gradle:foundry-gradle-plugin",
                ":platforms:intellij:skate",
                ":tools:cli",
                ":tools:foundry-common",
            )
    }

    @Test
    fun `getMatchingProjectPaths returns filtered results`() {
        val testPaths =
            setOf(
                ":platforms:gradle:foundry-gradle-plugin",
                ":platforms:intellij:skate",
                ":platforms:intellij:compose",
                ":tools:cli",
                ":tools:foundry-common",
            )

        val filtered = testPaths.filter { it.startsWith(":platforms:") }.sorted()

        assertThat(filtered).hasSize(3)
        assertThat(filtered).contains(":platforms:gradle:foundry-gradle-plugin")
        assertThat(filtered).contains(":platforms:intellij:compose")
        assertThat(filtered).contains(":platforms:intellij:skate")
    }
}
