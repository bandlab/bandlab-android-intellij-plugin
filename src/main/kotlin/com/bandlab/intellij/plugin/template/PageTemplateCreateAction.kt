package com.bandlab.intellij.plugin.template

import com.bandlab.intellij.plugin.utils.filePackage
import com.bandlab.intellij.plugin.utils.writeFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement

class PageTemplateCreateAction : CreateSimpleFileAction(
    text = "Page Template",
    description = "Create a Page template with latest convention.",
    inputHint = "Feature Name (Ex: UserLibrary)",
    availability = Availability.MainOnly
) {
    override fun create(newName: String, directory: PsiDirectory): Array<PsiElement> {
        val pageBuilder = PageTemplateBuilder(
            name = newName,
            filePackage = directory.filePackage
        )
        return arrayOf(
            directory.writeFile(
                fileName = "${newName}Page.kt",
                content = pageBuilder.createPageWithContributesComponent(),
            ),
            directory.writeFile("${newName}ViewModel.kt", pageBuilder.createViewModel()),
        )
    }

    override fun hashCode(): Int = 9433

    override fun equals(other: Any?): Boolean = other is PageTemplateCreateAction
}