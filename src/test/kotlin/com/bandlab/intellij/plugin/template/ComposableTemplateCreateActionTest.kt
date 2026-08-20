// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.template

import com.bandlab.intellij.plugin.utils.readFile
import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import java.io.File

class ComposableTemplateCreateActionTest : CreateTemplateActionTest() {

    fun testIsAvailableOnlyForMainDirectoriesWithComposePlugin() {
        val action = ComposableTemplateCreateAction()
        val mainDirectory =
            createProjectDirectory("compose-module/src/main/kotlin/com/bandlab/injection")
        val androidTestDirectory =
            createProjectDirectory("compose-module/src/androidTest/kotlin/com/bandlab/injection")
        val nonSourceDirectory = createProjectDirectory("compose-module/docs")
        createBuildGradle("compose-module", withCompose = true)

        assertThat(action.invokeIsAvailable(createDataContext(mainDirectory))).isTrue()
        assertThat(action.invokeIsAvailable(createDataContext(androidTestDirectory))).isFalse()
        assertThat(action.invokeIsAvailable(createDataContext(nonSourceDirectory))).isFalse()
    }

    fun testIsNotAvailableForModuleWithoutComposePlugin() {
        val action = ComposableTemplateCreateAction()
        val mainDirectory =
            createProjectDirectory("no-compose-module/src/main/kotlin/com/bandlab/injection")
        createBuildGradle("no-compose-module", withCompose = false)

        assertThat(action.invokeIsAvailable(createDataContext(mainDirectory))).isFalse()
    }

    fun testIsNotAvailableWithoutBuildScript() {
        val action = ComposableTemplateCreateAction()
        val mainDirectory = createProjectDirectory("src/main/kotlin/com/bandlab/injection")

        assertThat(action.invokeIsAvailable(createDataContext(mainDirectory))).isFalse()
    }

    fun testCreateGeneratesFile() {
        val action = ComposableTemplateCreateAction()
        val targetDirectory =
            createProjectDirectory("compose-module/src/main/kotlin/com/bandlab/injection")

        lateinit var createdElements: Array<PsiElement>
        WriteCommandAction.runWriteCommandAction(project) {
            createdElements = action.invokeCreate("ProjectList", targetDirectory)
        }

        assertThat(createdElements.map { it.containingFile.name })
            .containsExactly("ProjectList.kt")
            .inOrder()

        val builder = ComposableTemplateBuilder("ProjectList", "com.bandlab.injection")

        targetDirectory.virtualFile.refresh(false, true)
        assertThat(
                project.readFile(
                    targetDirectory.virtualFile.findChild("ProjectList.kt")!!.path,
                    isAbsolute = true,
                )
            )
            .isEqualTo(builder.buildFile())
    }

    private fun createBuildGradle(moduleDir: String, withCompose: Boolean) {
        val baseFile = File(requireNotNull(project.basePath))
        val buildGradle = File(baseFile, "$moduleDir/build.gradle")
        val content =
            if (withCompose) {
                """
                plugins {
                    alias(bandlab.plugins.library.android)
                    alias(bandlab.plugins.compose)
                }
                """
                    .trimIndent()
            } else {
                """
                plugins {
                    alias(bandlab.plugins.library.android)
                }
                """
                    .trimIndent()
            }
        buildGradle.writeText(content)

        val moduleVirtualDir =
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(baseFile, moduleDir))
        moduleVirtualDir?.refresh(false, true)
    }
}
