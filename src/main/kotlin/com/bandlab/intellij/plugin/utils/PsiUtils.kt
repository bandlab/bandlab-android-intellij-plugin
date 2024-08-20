package com.bandlab.intellij.plugin.utils

import com.bandlab.intellij.plugin.utils.Const.NEW_LINE
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.psi.KtClassOrObject

fun PsiElement.resolvePath(): String? {
    return when (this) {
        is PsiFile -> virtualFile.path
        is PsiDirectory -> virtualFile.path
        is KtClassOrObject -> containingFile.virtualFile.path
        else -> null
    }
}

fun Project.requireVirtualFile(path: String, isAbsolute: Boolean): VirtualFile {
    val absolutePath = if (isAbsolute) path else basePath + path
    return requireNotNull(LocalFileSystem.getInstance().refreshAndFindFileByPath(absolutePath))
}

/**
 *  Edit a given [filePath] in the project, [editBlock] provides you a callback with [StringBuilder]
 *  by helping you to fill in the existing content.
 */
fun Project.editFile(
    filePath: String,
    isAbsolute: Boolean,
    editBlock: StringBuilder.() -> Unit
) {
    val file = requireVirtualFile(filePath, isAbsolute)
    val filePsi = requireNotNull(file.toPsiFile(this))

    val document = requireNotNull(PsiDocumentManager.getInstance(this).getDocument(filePsi))
    val currentText = document.text

    val newText = buildString {
        appendLine(currentText)
        editBlock()
    }

    // Replace with the new text, follow up with an empty line at the end.
    document.replaceString(0, document.textLength, newText.trim() + NEW_LINE)

    // Refresh the VirtualFile to reflect the changes
    file.refresh(false, false)
}