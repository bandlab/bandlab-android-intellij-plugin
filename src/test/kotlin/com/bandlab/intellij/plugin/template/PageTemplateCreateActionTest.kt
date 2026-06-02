package com.bandlab.intellij.plugin.template

import com.bandlab.intellij.plugin.utils.readFile
import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement

class PageTemplateCreateActionTest : CreateTemplateActionTest() {

    fun testIsAvailableOnlyForMainDirectories() {
        val action = PageTemplateCreateAction()
        val mainDirectory = createProjectDirectory("src/main/kotlin/com/bandlab/page")
        val androidTestDirectory = createProjectDirectory("src/androidTest/kotlin/com/bandlab/page")
        val nonSourceDirectory = createProjectDirectory("docs")

        assertThat(action.invokeIsAvailable(createDataContext(mainDirectory))).isTrue()
        assertThat(action.invokeIsAvailable(createDataContext(androidTestDirectory))).isFalse()
        assertThat(action.invokeIsAvailable(createDataContext(nonSourceDirectory))).isFalse()
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

        assertThat(targetDirectory.readFile("UserLibraryPage.kt"))
            .isEqualTo(builder.createPage())

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
                "UserLibraryWithNavNavEntry.kt",
            )
            .inOrder()

        val builder = PageTemplateBuilder("UserLibraryWithNav", "com.bandlab.page", includeNavKey = true)
        val navKeyBuilder = NavKeyTemplateBuilder("UserLibraryWithNav", "com.bandlab.page")

        targetDirectory.virtualFile.refresh(false, true)

        assertThat(targetDirectory.readFile("UserLibraryWithNavPage.kt"))
            .isEqualTo(builder.createPage())

        assertThat(targetDirectory.readFile("UserLibraryWithNavViewModel.kt"))
            .isEqualTo(builder.createViewModel())

        assertThat(targetDirectory.readFile("UserLibraryWithNavKey.kt"))
            .isEqualTo(navKeyBuilder.createNavKey())

        assertThat(targetDirectory.readFile("UserLibraryWithNavNavEntry.kt"))
            .isEqualTo(navKeyBuilder.createNavEntry())
    }

    private fun PageTemplateCreateAction.invokeCreateWithNav(
        newName: String,
        directory: PsiDirectory
    ): Array<PsiElement> {
        return create(newName, directory, includeNav = true)
    }

    private fun PsiDirectory.readFile(fileName: String): String? {
        val path = virtualFile.findChild(fileName)?.path ?: return null
        return project.readFile(path, isAbsolute = true)
    }
}