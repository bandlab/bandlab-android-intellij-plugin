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

        val result = parseProjectPaths(content)

        assertThat(result)
            .containsExactly(
                ":platforms:gradle:foundry-gradle-plugin",
                ":platforms:intellij:skate",
                ":tools:cli",
                ":tools:foundry-common",
            )
    }

    @Test
    fun `parseProjectPaths trims whitespace and removes duplicate paths`() {
        val result =
            parseProjectPaths("  :features:profile:api  \n:features:profile:api\n  # comment\n")

        assertThat(result).containsExactly(":features:profile:api")
    }
}
