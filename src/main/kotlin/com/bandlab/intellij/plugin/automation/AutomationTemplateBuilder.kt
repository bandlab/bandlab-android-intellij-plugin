package com.bandlab.intellij.plugin.automation

import com.bandlab.intellij.plugin.automation.AutomationTemplateCreateAction.Companion.ANDROID_TEST_DIR
import com.bandlab.intellij.plugin.utils.resolvePath
import com.intellij.ide.actions.CreateFileAction.MkDirs
import com.intellij.internal.statistic.collectors.fus.fileTypes.FileTypeUsageCounterCollector
import com.intellij.openapi.application.WriteAction
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class AutomationTemplateBuilder(
    private val name: String,
    private val directory: PsiDirectory
) {

    private val filePackage = requireNotNull(directory.resolvePath()) { "Directory path is null" }
        .substringAfter("$ANDROID_TEST_DIR/kotlin/")
        .replace('/', '.')

    fun createRobot(): PsiElement {
        val robotPattern = """
            package $filePackage

            import com.bandlab.bandlab.screens.AnyAndroidComposeCompositeRule

            context(AnyAndroidComposeCompositeRule)
            class ${name}Robot {

                private val semantics = ${name}Semantics()

                fun verify(block: ${name}Verifier.() -> Unit) = apply {
                    ${name}Verifier(semantics).block()
                }
            }

        """.trimIndent()

        return writeFile("${name}Robot.kt", robotPattern)
    }

    fun createSemantics(): PsiElement {
        val semanticsPattern = """
            package $filePackage

            import com.bandlab.bandlab.screens.AnyAndroidComposeCompositeRule

            context(AnyAndroidComposeCompositeRule)
            class ${name}Semantics {
                
            }

        """.trimIndent()

        return writeFile("${name}Semantics.kt", semanticsPattern)
    }

    fun createVerifier(): PsiElement {
        val verifierPattern = """
            package $filePackage

            import com.bandlab.bandlab.screens.AnyAndroidComposeCompositeRule

            context(AnyAndroidComposeCompositeRule)
            class ${name}Verifier(
                private val semantics: ${name}Semantics,
            ) {
                
            }

        """.trimIndent()

        return writeFile("${name}Verifier.kt", verifierPattern)
    }

    private fun writeFile(
        fileName: String,
        content: String
    ): PsiElement {
        val mkdirs = MkDirs(name, directory)
        val filePsi = WriteAction.compute<PsiFile, RuntimeException> {
            mkdirs.directory.createFile(fileName)
        }

        val psiDocumentManager = PsiDocumentManager.getInstance(directory.project)
        psiDocumentManager.getDocument(filePsi)?.setText(content)

        FileTypeUsageCounterCollector.triggerCreate(filePsi.project, filePsi.virtualFile)
        return filePsi
    }
}