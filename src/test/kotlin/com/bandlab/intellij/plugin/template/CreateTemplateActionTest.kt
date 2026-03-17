package com.bandlab.intellij.plugin.template

import com.intellij.ide.IdeView
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

abstract class CreateTemplateActionTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = File(".").absolutePath

    protected fun createDataContext(directory: PsiDirectory): DataContext {
        return com.intellij.openapi.actionSystem.impl.SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, project)
            .add(CommonDataKeys.PSI_ELEMENT, directory)
            .add(CommonDataKeys.VIRTUAL_FILE, directory.virtualFile)
            .add(LangDataKeys.IDE_VIEW, TestIdeView(directory))
            .build()
    }

    protected fun createProjectDirectory(relativePath: String): PsiDirectory {
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

    protected fun Any.invokeIsAvailable(dataContext: DataContext): Boolean {
        val method = try {
            this::class.java.getDeclaredMethod("isAvailable", DataContext::class.java)
        } catch (e: NoSuchMethodException) {
            this::class.java.superclass.getDeclaredMethod("isAvailable", DataContext::class.java)
        }
        method.isAccessible = true
        return method.invoke(this, dataContext) as Boolean
    }

    @Suppress("UNCHECKED_CAST")
    protected fun Any.invokeCreate(newName: String, directory: PsiDirectory): Array<PsiElement> {
        val method = this::class.java.getDeclaredMethod("create", String::class.java, PsiDirectory::class.java)
        method.isAccessible = true
        return method.invoke(this, newName, directory) as Array<PsiElement>
    }

    protected class TestIdeView(private val directory: PsiDirectory) : IdeView {
        override fun getDirectories(): Array<PsiDirectory> = arrayOf(directory)
        override fun getOrChooseDirectory(): PsiDirectory = directory
        override fun selectElement(element: PsiElement) = Unit
    }
}
