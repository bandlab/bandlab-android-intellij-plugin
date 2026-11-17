// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.strings.textres

import com.google.common.truth.Truth.assertThat
import java.nio.file.Path
import org.junit.Test

class TextResOptionsTest {

    @Test
    fun tablesPairTheTableWithItsFileAndIgnoreOtherPlugins() {
        val tables =
            parseTextResTables(
                listOf(
                    "plugin:com.bandlab.localizer.text:table=com.a;Strings;Plurals",
                    "plugin:com.bandlab.localizer.text:table=com.b;BStrings;BPlurals",
                    "plugin:com.bandlab.localizer.text:tableFile=com.a;/repo/a/strings.xml",
                    "plugin:com.bandlab.localizer.text:manifestDir=/repo/build/keys",
                    "plugin:dev.zacsweers.metro:enabled=true",
                ),
            )
        assertThat(tables)
            .containsExactly(
                TextResTable("com.a", "Strings", "Plurals", Path.of("/repo/a/strings.xml")),
            )
    }

    @Test
    fun exactCompilerBuildWins() {
        assertThat(pickCompilerPluginBuild("2.3.255-dev-255", listOf("2.3.20", "2.3.255-dev-255")))
            .isEqualTo("2.3.255-dev-255")
    }

    @Test
    fun intellijCompilerTakesTheNewestEarlierBuildOfItsBranch() {
        val builds = listOf("2.3.20", "2.3.20-ij253-87", "2.3.20-ij253-105", "2.3.20-ij253-119")
        assertThat(pickCompilerPluginBuild("2.3.20-ij253-110", builds))
            .isEqualTo("2.3.20-ij253-105")
    }

    @Test
    fun publicReleaseOfTheSameVersionIsNotCompatible() {
        assertThat(pickCompilerPluginBuild("2.3.20-ij253-105", listOf("2.3.20"))).isNull()
    }
}
