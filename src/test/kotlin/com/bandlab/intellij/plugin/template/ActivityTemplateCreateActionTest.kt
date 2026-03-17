package com.bandlab.intellij.plugin.template

import com.bandlab.intellij.plugin.utils.readFile
import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement

class ActivityTemplateCreateActionTest : CreateTemplateActionTest() {

    fun testIsAvailableOnlyForMainDirectories() {
        val action = ActivityTemplateCreateAction()
        val mainDirectory = createProjectDirectory("feature/src/main/kotlin/com/bandlab/activity")
        val androidTestDirectory = createProjectDirectory("feature/src/androidTest/kotlin/com/bandlab/activity")
        val nonSourceDirectory = createProjectDirectory("docs")

        assertThat(action.invokeIsAvailable(createDataContext(mainDirectory))).isTrue()
        assertThat(action.invokeIsAvailable(createDataContext(androidTestDirectory))).isFalse()
        assertThat(action.invokeIsAvailable(createDataContext(nonSourceDirectory))).isFalse()
    }

    private fun PsiDirectory.requireMainDir(): PsiDirectory {
        var parentDir = parentDirectory
        while (parentDir != null) {
            if (parentDir.virtualFile.path.endsWith("src/main")) {
                return parentDir
            } else {
                parentDir = parentDir.parentDirectory
            }
        }
        error("Cannot find main dir under $this")
    }

    fun testCreateGeneratesActivityPageViewModelAndManifestFiles() {
        val action = ActivityTemplateCreateAction()
        val targetDirectory = createProjectDirectory("src/main/kotlin/com/bandlab/activity")

        lateinit var createdElements: Array<PsiElement>
        WriteCommandAction.runWriteCommandAction(project) {
            createdElements = action.invokeCreate("UserLibrary", targetDirectory)
        }

        assertThat(createdElements.map { it.containingFile.name })
            .containsExactly(
                "UserLibraryActivity.kt",
                "UserLibraryPage.kt",
                "UserLibraryViewModel.kt",
                "AndroidManifest.xml",
            )
            .inOrder()

        val builder = ActivityTemplateBuilder("UserLibrary", "com.bandlab.activity")
        
        targetDirectory.virtualFile.refresh(false, true)
        assertThat(project.readFile(targetDirectory.virtualFile.findChild("UserLibraryActivity.kt")!!.path, isAbsolute = true))
            .isEqualTo(builder.createActivity())
        assertThat(project.readFile(targetDirectory.virtualFile.findChild("UserLibraryPage.kt")!!.path, isAbsolute = true))
            .isEqualTo(builder.createPage())
        assertThat(project.readFile(targetDirectory.virtualFile.findChild("UserLibraryViewModel.kt")!!.path, isAbsolute = true))
            .isEqualTo(builder.createViewModel())
        
        val mainDir = targetDirectory.requireMainDir()
        val manifestFile = mainDir.virtualFile.findChild("AndroidManifest.xml")
        assertThat(manifestFile).isNotNull()
        manifestFile!!.refresh(false, false)
        assertThat(project.readFile(manifestFile.path, isAbsolute = true))
            .isEqualTo(builder.createManifest())
    }

}
