package com.bandlab.intellij.plugin.utils

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.refactoring.psiElement

fun AnActionEvent.psiFileOrNull(): PsiFile? {
    val containingFile = dataContext.psiElement?.containingFile
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