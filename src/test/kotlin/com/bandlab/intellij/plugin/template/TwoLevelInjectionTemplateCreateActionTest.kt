package com.bandlab.intellij.plugin.template

import com.bandlab.intellij.plugin.utils.readFile
import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement

class TwoLevelInjectionTemplateCreateActionTest : CreateTemplateActionTest() {

    fun testIsAvailableOnlyForMainDirectories() {
        val action = TwoLevelInjectionTemplateCreateAction()
        val mainDirectory = createProjectDirectory("src/main/kotlin/com/bandlab/injection")
        val androidTestDirectory = createProjectDirectory("src/androidTest/kotlin/com/bandlab/injection")
        val nonSourceDirectory = createProjectDirectory("docs")

        assertThat(action.invokeIsAvailable(createDataContext(mainDirectory))).isTrue()
        assertThat(action.invokeIsAvailable(createDataContext(androidTestDirectory))).isFalse()
        assertThat(action.invokeIsAvailable(createDataContext(nonSourceDirectory))).isFalse()
    }

    fun testCreateGeneratesInterfaceAndImplFiles() {
        val action = TwoLevelInjectionTemplateCreateAction()
        val targetDirectory = createProjectDirectory("src/main/kotlin/com/bandlab/injection")

        lateinit var createdElements: Array<PsiElement>
        WriteCommandAction.runWriteCommandAction(project) {
            createdElements = action.invokeCreate("InvertedViewModel", targetDirectory)
        }

        assertThat(createdElements.map { it.containingFile.name })
            .containsExactly(
                "InvertedViewModel.kt",
                "InvertedViewModelImpl.kt",
            )
            .inOrder()

        val builder = TwoLevelInjectionTemplateBuilder("InvertedViewModel", "com.bandlab.injection")
        
        targetDirectory.virtualFile.refresh(false, true)
        assertThat(project.readFile(targetDirectory.virtualFile.findChild("InvertedViewModel.kt")!!.path, isAbsolute = true))
            .isEqualTo(builder.buildInterface())
        assertThat(project.readFile(targetDirectory.virtualFile.findChild("InvertedViewModelImpl.kt")!!.path, isAbsolute = true))
            .isEqualTo(builder.buildImpl())
    }

}
