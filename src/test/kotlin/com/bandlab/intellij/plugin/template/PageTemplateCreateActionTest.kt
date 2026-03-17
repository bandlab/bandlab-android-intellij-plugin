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

class PageTemplateCreateActionTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = File(".").absolutePath

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

        val virtualBase = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(baseFile)!!
        virtualBase.refresh(false, true)

        val virtualTarget = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(targetFile)!!
        
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

    private fun PageTemplateCreateAction.invokeIsAvailable(dataContext: DataContext): Boolean {
        val method = CreateSimpleFileAction::class.java.getDeclaredMethod(
            "isAvailable",
            DataContext::class.java,
        )
        method.isAccessible = true
        return method.invoke(this, dataContext) as Boolean
    }

    @Suppress("UNCHECKED_CAST")
    private fun PageTemplateCreateAction.invokeCreate(
        newName: String,
        directory: PsiDirectory,
    ): Array<PsiElement> {
        val method = PageTemplateCreateAction::class.java.getDeclaredMethod(
            "create",
            String::class.java,
            PsiDirectory::class.java,
        )
        method.isAccessible = true
        return method.invoke(this, newName, directory) as Array<PsiElement>
    }
}
