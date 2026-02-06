package com.bandlab.intellij.plugin.template

import com.bandlab.intellij.plugin.utils.filePackage
import com.bandlab.intellij.plugin.utils.resolvePath
import com.bandlab.intellij.plugin.utils.writeFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement

class ActivityTemplateCreateAction : CreateSimpleFileAction(
    text = "Activity Template",
    description = "Create an activity template with latest convention.",
    inputHint = "Feature Name (Ex: UserLibrary)",
    availability = Availability.MainOnly
) {
    override fun create(newName: String, directory: PsiDirectory): Array<PsiElement> {
        val activityBuilder = ActivityTemplateBuilder(
            name = newName,
            filePackage = directory.filePackage
        )
        return arrayOf(
            directory.writeFile("${newName}Activity.kt", activityBuilder.createActivity()),
            directory.writeFile("${newName}ViewModel.kt", activityBuilder.createViewModel()),
            // Known issue, if AndroidManifest exist already, the plugin cannot create a new instance, you'll see
            // the error and will need to add the declaration manually. I don't think we need to support this case,
            // having multiple activities in the same module is generally discouraged.
            directory.requireMainDir().writeFile("AndroidManifest.xml", activityBuilder.createManifest()),
        )
    }

    /**
     * Go through the parents of the [PsiDirectory] and find the main directory.
     */
    private fun PsiDirectory.requireMainDir(): PsiDirectory {
        var parentDir = parentDirectory
        while (parentDir != null) {
            if (parentDir.resolvePath()?.endsWith("src/main") == true) {
                return parentDir
            } else {
                parentDir = parentDir.parentDirectory
            }
        }
        error("Cannot find main dir under $this")
    }

    override fun hashCode(): Int = 9432

    override fun equals(other: Any?): Boolean = other is ActivityTemplateCreateAction

}