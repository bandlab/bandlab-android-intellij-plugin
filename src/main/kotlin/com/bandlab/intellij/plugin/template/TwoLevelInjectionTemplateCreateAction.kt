package com.bandlab.intellij.plugin.template

import com.bandlab.intellij.plugin.utils.filePackage
import com.bandlab.intellij.plugin.utils.writeFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement

class TwoLevelInjectionTemplateCreateAction : CreateSimpleFileAction(
    text = "2-level Injection Template",
    description = "Create a 2-level Injection template with latest convention.",
    inputHint = "Interface Name (Ex: InvertedViewModel)",
    availability = Availability.MainOnly
) {
    override fun create(newName: String, directory: PsiDirectory): Array<PsiElement> {
        val templateBuilder = TwoLevelInjectionTemplateBuilder(
            name = newName,
            filePackage = directory.filePackage
        )
        return arrayOf(
            directory.writeFile(fileName = "${newName}.kt", content = templateBuilder.buildInterface()),
            directory.writeFile(fileName = "${newName}Impl.kt", content = templateBuilder.buildImpl())
        )
    }

    override fun hashCode(): Int = 9434

    override fun equals(other: Any?): Boolean = other is TwoLevelInjectionTemplateCreateAction
}