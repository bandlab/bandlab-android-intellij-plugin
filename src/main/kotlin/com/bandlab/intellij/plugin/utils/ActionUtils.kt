// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.utils

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

fun AnActionEvent.psiFileOrNull(): PsiFile? {
    val containingFile = CommonDataKeys.PSI_ELEMENT.getData(dataContext)?.containingFile
    if (containingFile != null) {
        return containingFile
    }

    val project = this.project
    val doc = dataContext.getData(DataKey.create<Editor>("editor"))?.document
    return if (project == null || doc == null) {
        null
    } else {
        PsiDocumentManager.getInstance(project).getPsiFile(doc)
    }
}
