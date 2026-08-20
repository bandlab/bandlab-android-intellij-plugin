// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.dependencies.autocomplete

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.util.IncorrectOperationException
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/**
 * Reference implementation for project paths in project(...) calls. Allows navigation to the
 * referenced project's build file.
 */
class GradleProjectReference(
    private val element: PsiElement,
    private val range: TextRange,
    private val projectPath: String,
) : PsiReference {

    override fun getElement(): PsiElement = element

    override fun getRangeInElement(): TextRange = range

    override fun resolve(): PsiElement? {
        val project = element.project
        val projectPathService = project.getService(ProjectPathService::class.java)

        if (!projectPathService.isValidProjectPath(projectPath)) {
            return null
        }

        // Convert a project path to a filesystem path
        val projectBasePath = project.basePath?.let(::Path) ?: return null
        val relativePath = projectPath.removePrefix(":").replace(":", "/")
        val projectDir = projectBasePath.resolve(relativePath)

        if (!projectDir.exists() || !projectDir.isDirectory()) {
            return null
        }

        // Look for build.gradle.kts first, then build.gradle
        val targetFile = findBuildFile(projectDir) ?: return null

        val virtualFile =
            VirtualFileManager.getInstance()
                .findFileByUrl("file://${targetFile.absolutePathString()}") ?: return null

        return PsiManager.getInstance(project).findFile(virtualFile)
    }

    override fun getCanonicalText(): String = projectPath

    override fun handleElementRename(newElementName: String): PsiElement {
        throw IncorrectOperationException("Cannot rename project path reference")
    }

    override fun bindToElement(element: PsiElement): PsiElement {
        throw IncorrectOperationException("Cannot bind project path reference")
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        val resolved = resolve()
        return resolved != null && resolved == element
    }

    override fun isSoft(): Boolean = false

    /** Navigate to the referenced project's build file. */
    fun navigate() {
        val resolved = resolve()
        if (resolved != null) {
            val virtualFile = resolved.containingFile?.virtualFile
            if (virtualFile != null) {
                FileEditorManager.getInstance(element.project).openFile(virtualFile, true)
            }
        }
    }
}

/**
 * Internal function to find the build file in a project directory. Prefers build.gradle.kts over
 * build.gradle. Exposed for testing.
 */
internal fun findBuildFile(projectDir: Path): Path? {
    val buildFileKts = projectDir.resolve("build.gradle.kts")
    val buildFile = projectDir.resolve("build.gradle")

    return when {
        buildFileKts.exists() -> buildFileKts
        buildFile.exists() -> buildFile
        else -> null
    }
}
