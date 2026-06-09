package com.bandlab.intellij.plugin.template

import com.bandlab.intellij.plugin.utils.filePackage
import com.bandlab.intellij.plugin.utils.writeFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement

class ComposableTemplateCreateAction : CreateSimpleFileAction(
    text = "Composable Template",
    description = "Create a Composable template with latest convention.",
    inputHint = "Composable Name (Ex: ProjectContent)",
    availability = Availability.ComposeOnly
) {
    override fun create(newName: String, directory: PsiDirectory): Array<PsiElement> {
        val templateBuilder = ComposableTemplateBuilder(
            name = newName,
            filePackage = directory.filePackage
        )
        return arrayOf(
            directory.writeFile(fileName = "${newName}.kt", content = templateBuilder.buildFile()),
        )
    }

    override fun hashCode(): Int = 9435

    override fun equals(other: Any?): Boolean = other is ComposableTemplateCreateAction
}