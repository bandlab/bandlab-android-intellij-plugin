// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.template

import com.bandlab.intellij.plugin.utils.readFile
import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import java.io.File

class PageTemplateCreateActionTest : CreateTemplateActionTest() {

    fun testIsAvailableOnlyForMainDirectoriesWithComposePlugin() {
        val action = PageTemplateCreateAction()
        val mainDirectory =
            createProjectDirectory("compose-module/src/main/kotlin/com/bandlab/page")
        val androidTestDirectory =
            createProjectDirectory("compose-module/src/androidTest/kotlin/com/bandlab/page")
        val nonSourceDirectory = createProjectDirectory("compose-module/docs")
        createBuildGradle("compose-module", withCompose = true)

        assertThat(action.invokeIsAvailable(createDataContext(mainDirectory))).isTrue()
        assertThat(action.invokeIsAvailable(createDataContext(androidTestDirectory))).isFalse()
        assertThat(action.invokeIsAvailable(createDataContext(nonSourceDirectory))).isFalse()
    }

    fun testIsNotAvailableForModuleWithoutComposePlugin() {
        val action = PageTemplateCreateAction()
        val mainDirectory =
            createProjectDirectory("no-compose-module/src/main/kotlin/com/bandlab/page")
        createBuildGradle("no-compose-module", withCompose = false)

        assertThat(action.invokeIsAvailable(createDataContext(mainDirectory))).isFalse()
    }

    fun testIsNotAvailableWithoutBuildScript() {
        val action = PageTemplateCreateAction()
        val mainDirectory = createProjectDirectory("src/main/kotlin/com/bandlab/page")

        assertThat(action.invokeIsAvailable(createDataContext(mainDirectory))).isFalse()
    }

    fun testCreateGeneratesPageAndViewModelFiles() {
        val action = PageTemplateCreateAction()
        val targetDirectory = createProjectDirectory("src/main/kotlin/com/bandlab/page")

        lateinit var createdElements: Array<PsiElement>
        WriteCommandAction.runWriteCommandAction(project) {
            createdElements = action.invokeCreate("UserLibrary", targetDirectory)
        }

        assertThat(createdElements.map { it.containingFile.name })
            .containsExactly(
                "UserLibraryPage.kt",
                "UserLibraryViewModel.kt",
            )
            .inOrder()

        val builder = PageTemplateBuilder("UserLibrary", "com.bandlab.page", includeNavKey = false)

        targetDirectory.virtualFile.refresh(false, true)

        assertThat(targetDirectory.readFile("UserLibraryPage.kt")).isEqualTo(builder.createPage())

        assertThat(targetDirectory.readFile("UserLibraryViewModel.kt"))
            .isEqualTo(builder.createViewModel())
    }

    fun testCreateGeneratesNavKeyAndEntryFiles() {
        val action = PageTemplateCreateAction()
        val targetDirectory = createProjectDirectory("src/main/kotlin/com/bandlab/page")

        lateinit var createdElements: Array<PsiElement>
        WriteCommandAction.runWriteCommandAction(project) {
            createdElements = action.invokeCreateWithNav("UserLibraryWithNav", targetDirectory)
        }

        assertThat(createdElements.map { it.containingFile.name })
            .containsExactly(
                "UserLibraryWithNavPage.kt",
                "UserLibraryWithNavViewModel.kt",
                "UserLibraryWithNavKey.kt",
            )
            .inOrder()

        val builder =
            PageTemplateBuilder("UserLibraryWithNav", "com.bandlab.page", includeNavKey = true)
        val navKeyBuilder = NavKeyTemplateBuilder("UserLibraryWithNav", "com.bandlab.page")

        targetDirectory.virtualFile.refresh(false, true)

        assertThat(targetDirectory.readFile("UserLibraryWithNavPage.kt"))
            .isEqualTo(builder.createPage())

        assertThat(targetDirectory.readFile("UserLibraryWithNavViewModel.kt"))
            .isEqualTo(builder.createViewModel())

        assertThat(targetDirectory.readFile("UserLibraryWithNavKey.kt"))
            .isEqualTo(navKeyBuilder.createNavKey())
    }

    private fun PageTemplateCreateAction.invokeCreateWithNav(
        newName: String,
        directory: PsiDirectory,
    ): Array<PsiElement> {
        return create(newName, directory, includeNav = true)
    }

    private fun PsiDirectory.readFile(fileName: String): String? {
        val path = virtualFile.findChild(fileName)?.path ?: return null
        return project.readFile(path, isAbsolute = true)
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
