package com.bandlab.intellij.plugin.template

import com.bandlab.intellij.plugin.utils.readFile
import com.google.common.truth.Truth.assertThat
import com.intellij.ide.IdeView
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

class ActivityTemplateCreateActionTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = File(".").absolutePath

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

    private fun createDataContext(directory: PsiDirectory): DataContext {
        return com.intellij.openapi.actionSystem.impl.SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, project)
            .add(CommonDataKeys.PSI_ELEMENT, directory)
            .add(CommonDataKeys.VIRTUAL_FILE, directory.virtualFile)
            .add(LangDataKeys.IDE_VIEW, TestIdeView(directory))
            .build()
    }

    private fun createProjectDirectory(relativePath: String): PsiDirectory {
        val baseFile = File(requireNotNull(project.basePath))
        val targetFile = File(baseFile, relativePath)
        targetFile.mkdirs()

        // Important: refresh everything in the base to ensure VFS is fully aware of the hierarchy
        val virtualBase = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(baseFile)!!
        virtualBase.refresh(false, true)

        val virtualTarget = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(targetFile)!!
        
        // Ensure parent hierarchy in PSI is also aware
        var current: VirtualFile? = virtualTarget
        while (current != null && current.path != virtualBase.path) {
            PsiManager.getInstance(project).findDirectory(current)
            current = current.parent
        }

        return requireNotNull(PsiManager.getInstance(project).findDirectory(virtualTarget))
    }

    private class TestIdeView(
        private val directory: PsiDirectory
    ) : IdeView {

        override fun getDirectories(): Array<PsiDirectory> = arrayOf(directory)

        override fun getOrChooseDirectory(): PsiDirectory = directory

        override fun selectElement(element: PsiElement) = Unit
    }

    private fun ActivityTemplateCreateAction.invokeIsAvailable(dataContext: DataContext): Boolean {
        val method = CreateSimpleFileAction::class.java.getDeclaredMethod(
            "isAvailable",
            DataContext::class.java,
        )
        method.isAccessible = true
        return method.invoke(this, dataContext) as Boolean
    }

    @Suppress("UNCHECKED_CAST")
    private fun ActivityTemplateCreateAction.invokeCreate(
        newName: String,
        directory: PsiDirectory,
    ): Array<PsiElement> {
        val method = ActivityTemplateCreateAction::class.java.getDeclaredMethod(
            "create",
            String::class.java,
            PsiDirectory::class.java,
        )
        method.isAccessible = true
        return method.invoke(this, newName, directory) as Array<PsiElement>
    }
}
