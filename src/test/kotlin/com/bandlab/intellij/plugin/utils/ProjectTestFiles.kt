// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.utils

import com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager

/** Resets a fixture file on disk and in the reused light project's VFS and document cache. */
fun Project.writeTestFile(relativePath: String, content: String): VirtualFile {
    lateinit var file: VirtualFile
    runWriteCommandAction(this) {
        val directory =
            requireNotNull(
                VfsUtil.createDirectoryIfMissing(
                    "${requireNotNull(basePath)}/${relativePath.substringBeforeLast('/', "")}",
                ),
            )
        file = directory.findOrCreateChildData(this, relativePath.substringAfterLast('/'))
        val documentManager = FileDocumentManager.getInstance()
        val document = requireNotNull(documentManager.getDocument(file))
        document.setText(content)
        PsiDocumentManager.getInstance(this).commitDocument(document)
        documentManager.saveDocument(document)
    }
    return file
}
