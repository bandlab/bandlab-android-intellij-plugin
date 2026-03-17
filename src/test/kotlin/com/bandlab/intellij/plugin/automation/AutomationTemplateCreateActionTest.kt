package com.bandlab.intellij.plugin.automation

import com.bandlab.intellij.plugin.template.CreateTemplateActionTest
import com.bandlab.intellij.plugin.utils.readFile
import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement

class AutomationTemplateCreateActionTest : CreateTemplateActionTest() {

    fun testIsAvailableOnlyForAndroidTestDirectories() {
        val action = AutomationTemplateCreateAction()
        val androidTestDirectory = createProjectDirectory("feature/src/androidTest/kotlin/com/bandlab/feature/profile")
        val mainDirectory = createProjectDirectory("feature/src/main/kotlin/com/bandlab/feature/profile")
        val nonSourceDirectory = createProjectDirectory("docs")

        assertThat(action.invokeIsAvailable(createDataContext(androidTestDirectory))).isTrue()
        assertThat(action.invokeIsAvailable(createDataContext(mainDirectory))).isFalse()
        assertThat(action.invokeIsAvailable(createDataContext(nonSourceDirectory))).isFalse()
    }

    fun testCreateGeneratesRobotSemanticsAndVerifierFilesWithExpectedContent() {
        val action = AutomationTemplateCreateAction()
        val targetDirectory = createProjectDirectory("feature/src/androidTest/kotlin/com/bandlab/feature/profile")

        lateinit var createdElements: Array<PsiElement>
        WriteCommandAction.runWriteCommandAction(project) {
            createdElements = action.invokeCreate("UserLibrary", targetDirectory)
        }

        assertThat(createdElements.map { it.containingFile.name })
            .containsExactly(
                "UserLibraryRobot.kt",
                "UserLibrarySemantics.kt",
                "UserLibraryVerifier.kt",
            )
            .inOrder()

        assertThat(project.readFile(targetDirectory.virtualFile.findChild("UserLibraryRobot.kt")!!.path, isAbsolute = true))
            .isEqualTo(expectedRobotFile().withTrailingNewline())
        assertThat(project.readFile(targetDirectory.virtualFile.findChild("UserLibrarySemantics.kt")!!.path, isAbsolute = true))
            .isEqualTo(expectedSemanticsFile().withTrailingNewline())
        assertThat(project.readFile(targetDirectory.virtualFile.findChild("UserLibraryVerifier.kt")!!.path, isAbsolute = true))
            .isEqualTo(expectedVerifierFile().withTrailingNewline())
    }


    private fun expectedRobotFile(): String =
        """
        package com.bandlab.feature.profile

        import com.bandlab.bandlab.screens.AnyAndroidComposeCompositeRule

        class UserLibraryRobot(
            private val rule: AnyAndroidComposeCompositeRule,
        ) {

            private val semantics = UserLibrarySemantics(rule)

            fun verify(block: UserLibraryVerifier.() -> Unit) = apply {
                UserLibraryVerifier(rule, semantics).block()
            }
        }
        """.trimIndent()

    private fun expectedSemanticsFile(): String =
        """
        package com.bandlab.feature.profile

        import com.bandlab.bandlab.screens.AnyAndroidComposeCompositeRule

        class UserLibrarySemantics(
            private val rule: AnyAndroidComposeCompositeRule,
        ) {
            
        }
        """.trimIndent()

    private fun expectedVerifierFile(): String =
        """
        package com.bandlab.feature.profile

        import com.bandlab.bandlab.screens.AnyAndroidComposeCompositeRule

        class UserLibraryVerifier(
            private val rule: AnyAndroidComposeCompositeRule,
            private val semantics: UserLibrarySemantics,
        ) {
            
        }
        """.trimIndent()


    private fun String.withTrailingNewline(): String = "$this\n"
}



