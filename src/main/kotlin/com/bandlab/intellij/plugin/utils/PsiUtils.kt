package com.bandlab.intellij.plugin.utils

import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtClassOrObject

fun PsiElement.resolvePath(): String? {
    return when (this) {
        is PsiFile -> virtualFile.path
        is PsiDirectory -> virtualFile.path
        is KtClassOrObject -> containingFile.virtualFile.path
        else -> null
    }
}