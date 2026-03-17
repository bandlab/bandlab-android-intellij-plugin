package com.bandlab.intellij.plugin.template

import com.bandlab.intellij.plugin.utils.readFile
import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.command.WriteCommandAction
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

        val builder = PageTemplateBuilder("UserLibrary", "com.bandlab.page")
        
        targetDirectory.virtualFile.refresh(false, true)
        assertThat(project.readFile(targetDirectory.virtualFile.findChild("UserLibraryPage.kt")!!.path, isAbsolute = true))
            .isEqualTo(builder.createPageWithContributesComponent())
        assertThat(project.readFile(targetDirectory.virtualFile.findChild("UserLibraryViewModel.kt")!!.path, isAbsolute = true))
            .isEqualTo(builder.createViewModel())
    }

}
