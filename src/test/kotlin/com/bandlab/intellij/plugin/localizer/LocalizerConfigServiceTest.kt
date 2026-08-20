// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.localizer

import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

class LocalizerConfigServiceTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        listOf("localizer", "common", "audiostretch", "app").forEach {
            File(requireNotNull(project.basePath), it).deleteRecursively()
        }
    }

    private val service
        get() = project.service<LocalizerConfigService>()

    fun testTargetsInManifestOrderFirstIsDefault() {
        writeManifest(TWO_TARGETS)
        assertThat(service.targets().map { it.addKeysToFile })
            .containsExactly(
                "common/android/strings/src/main/res/values/strings.xml",
                "audiostretch/common-strings/src/main/res/values/strings.xml",
            )
            .inOrder()
    }

    fun testTargetForBaseFile() {
        writeManifest(TWO_TARGETS)
        val base =
            createFile(
                "audiostretch/common-strings/src/main/res/values/strings.xml",
                "<resources/>",
            )
        assertThat(service.targetFor(base)?.addKeysToFile)
            .isEqualTo("audiostretch/common-strings/src/main/res/values/strings.xml")
    }

    fun testTargetForTranslationFileMapsToItsBase() {
        writeManifest(TWO_TARGETS)
        val fr =
            createFile("common/android/strings/src/main/res/values-fr/strings.xml", "<resources/>")
        assertThat(service.targetFor(fr)?.addKeysToFile)
            .isEqualTo("common/android/strings/src/main/res/values/strings.xml")
    }

    fun testUnrelatedFileHasNoTarget() {
        writeManifest(TWO_TARGETS)
        val other = createFile("app/src/main/kotlin/Foo.kt", "class Foo")
        assertThat(service.targetFor(other)).isNull()
        assertThat(service.isManagedStringFile(other)).isFalse()
    }

    fun testNotConfiguredWithoutManifest() {
        assertThat(service.isConfigured()).isFalse()
        assertThat(service.targets()).isEmpty()
    }

    fun testTargetsForRClassMapsToSingleModuleTarget() {
        writeManifest(TWO_TARGETS)
        assertThat(
                service.targetsForRClass("com.bandlab.audiostretch.common.strings.R").map {
                    it.addKeysToFile
                }
            )
            .containsExactly("audiostretch/common-strings/src/main/res/values/strings.xml")
    }

    fun testTargetsForRClassReturnsEveryTargetUnderTheModule() {
        writeManifest(COMMON_STRINGS_WITH_PLURALS)
        assertThat(
                service.targetsForRClass("com.bandlab.common.strings.R").map { it.addKeysToFile }
            )
            .containsExactly(
                "common/android/strings/src/main/res/values/strings.xml",
                "common/android/strings/src/main/res/values/strings-plurals.xml",
            )
            .inOrder()
    }

    fun testTargetsForUnmappedRClassIsEmpty() {
        writeManifest(TWO_TARGETS)
        assertThat(service.targetsForRClass("com.unknown.R")).isEmpty()
        assertThat(service.targetsForRClass("R")).isEmpty()
    }

    private fun writeManifest(content: String) {
        createFile("localizer/bandlab-localizer-config.toml", content)
    }

    private fun createFile(relativePath: String, content: String): VirtualFile {
        val file = File(requireNotNull(project.basePath), relativePath)
        file.parentFile.mkdirs()
        file.writeText(content)
        return requireNotNull(LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file))
    }

    private companion object {
        val TWO_TARGETS =
            """
            api-key = "k"
            project-id = "1"

            [[file]]
            locale = "en"
            base_path = "common/android/strings/src/main/res"
            path = "values/strings.xml"

            [file.translations]
            fr = "values-fr/strings.xml"

            [[file]]
            locale = "en"
            base_path = "audiostretch/common-strings/src/main/res"
            path = "values/strings.xml"

            [file.translations]
            fr = "values-fr/strings.xml"
            """
                .trimIndent()

        // Two base files under the same common/android/strings module: singulars + plurals.
        val COMMON_STRINGS_WITH_PLURALS =
            """
            api-key = "k"
            project-id = "1"

            [[file]]
            locale = "en"
            base_path = "common/android/strings/src/main/res"
            path = "values/strings.xml"

            [[file]]
            locale = "en"
            base_path = "common/android/strings/src/main/res"
            path = "values/strings-plurals.xml"
            """
                .trimIndent()
    }
}
