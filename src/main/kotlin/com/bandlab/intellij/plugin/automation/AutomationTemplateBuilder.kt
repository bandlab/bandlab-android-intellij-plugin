package com.bandlab.intellij.plugin.automation

import com.bandlab.intellij.plugin.utils.filePackage
import com.bandlab.intellij.plugin.utils.writeFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement

class AutomationTemplateBuilder(
    private val name: String,
    private val directory: PsiDirectory
) {

    private val filePackage = directory.filePackage

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

        return directory.writeFile("${name}Robot.kt", robotPattern)
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

        return directory.writeFile("${name}Semantics.kt", semanticsPattern)
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

        return directory.writeFile("${name}Verifier.kt", verifierPattern)
    }
}