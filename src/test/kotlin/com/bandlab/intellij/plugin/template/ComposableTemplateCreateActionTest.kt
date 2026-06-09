package com.bandlab.intellij.plugin.template

import com.bandlab.intellij.plugin.utils.readFile
import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement

class ComposableTemplateCreateActionTest : CreateTemplateActionTest() {

    fun testIsAvailableOnlyForMainDirectories() {
        val action = ComposableTemplateCreateAction()
        val mainDirectory = createProjectDirectory("src/main/kotlin/com/bandlab/injection")
        val androidTestDirectory = createProjectDirectory("src/androidTest/kotlin/com/bandlab/injection")
        val nonSourceDirectory = createProjectDirectory("docs")

        assertThat(action.invokeIsAvailable(createDataContext(mainDirectory))).isTrue()
        assertThat(action.invokeIsAvailable(createDataContext(androidTestDirectory))).isFalse()
        assertThat(action.invokeIsAvailable(createDataContext(nonSourceDirectory))).isFalse()
    }

    fun testCreateGeneratesFile() {
        val action = ComposableTemplateCreateAction()
        val targetDirectory = createProjectDirectory("src/main/kotlin/com/bandlab/injection")

        lateinit var createdElements: Array<PsiElement>
        WriteCommandAction.runWriteCommandAction(project) {
            createdElements = action.invokeCreate("ProjectList", targetDirectory)
        }

        assertThat(createdElements.map { it.containingFile.name })
            .containsExactly(
                "ProjectList.kt",
            )
            .inOrder()

        val builder = ComposableTemplateBuilder("ProjectList", "com.bandlab.injection")
        
        targetDirectory.virtualFile.refresh(false, true)
        assertThat(project.readFile(targetDirectory.virtualFile.findChild("ProjectList.kt")!!.path, isAbsolute = true))
            .isEqualTo(builder.buildFile())
    }

}
