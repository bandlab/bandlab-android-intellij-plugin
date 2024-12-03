package com.bandlab.intellij.plugin.automation

import com.bandlab.intellij.plugin.automation.AutomationTemplateCreateAction.Companion.ANDROID_TEST_DIR
import com.bandlab.intellij.plugin.utils.resolvePath
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.idea.KotlinFileType

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

            class ${name}Robot(
                private val rule: AnyAndroidComposeCompositeRule,
            ) {

                private val semantics = ${name}Semantics(rule)

                fun verify(block: ${name}Verifier.() -> Unit) = apply {
                    ${name}Verifier(rule, semantics).block()
                }
            }

        """.trimIndent()

        return writeFile("${name}Robot.kt", robotPattern)
    }

    fun createSemantics(): PsiElement {
        val semanticsPattern = """
            package $filePackage

            import com.bandlab.bandlab.screens.AnyAndroidComposeCompositeRule

            class ${name}Semantics(
                private val rule: AnyAndroidComposeCompositeRule,
            ) {
                
            }

        """.trimIndent()

        return writeFile("${name}Semantics.kt", semanticsPattern)
    }

    fun createVerifier(): PsiElement {
        val verifierPattern = """
            package $filePackage

            import com.bandlab.bandlab.screens.AnyAndroidComposeCompositeRule

            class ${name}Verifier(
                private val rule: AnyAndroidComposeCompositeRule,
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
        val psiFile = PsiFileFactory
            .getInstance(directory.project)
            .createFileFromText(fileName, KotlinFileType.INSTANCE, content)

        return directory.add(psiFile)
    }
}