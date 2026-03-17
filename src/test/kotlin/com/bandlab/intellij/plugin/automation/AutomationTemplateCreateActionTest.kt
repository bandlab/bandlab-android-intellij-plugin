package com.bandlab.intellij.plugin.automation

import com.bandlab.intellij.plugin.utils.readFile
import com.google.common.truth.Truth.assertThat
import com.intellij.ide.IdeView
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

class AutomationTemplateCreateActionTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = File(".").absolutePath

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

    private fun createDataContext(directory: PsiDirectory): DataContext {
        return com.intellij.openapi.actionSystem.impl.SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, project)
            .add(CommonDataKeys.PSI_ELEMENT, directory)
            .add(CommonDataKeys.VIRTUAL_FILE, directory.virtualFile)
            .add(LangDataKeys.IDE_VIEW, TestIdeView(directory))
            .build()
    }

    private fun createProjectDirectory(relativePath: String): PsiDirectory {
        val ioDirectory = File(requireNotNull(project.basePath), relativePath)
        ioDirectory.mkdirs()

        val virtualDirectory = requireNotNull(
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(ioDirectory)
        )
        return requireNotNull(PsiManager.getInstance(project).findDirectory(virtualDirectory))
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

    private class TestIdeView(
        private val directory: PsiDirectory
    ) : IdeView {

        override fun getDirectories(): Array<PsiDirectory> = arrayOf(directory)

        override fun getOrChooseDirectory(): PsiDirectory = directory

        override fun selectElement(element: PsiElement) = Unit
    }

    private fun AutomationTemplateCreateAction.invokeIsAvailable(dataContext: DataContext): Boolean {
        val method = AutomationTemplateCreateAction::class.java.getDeclaredMethod(
            "isAvailable",
            DataContext::class.java,
        )
        method.isAccessible = true
        return method.invoke(this, dataContext) as Boolean
    }

    @Suppress("UNCHECKED_CAST")
    private fun AutomationTemplateCreateAction.invokeCreate(
        newName: String,
        directory: PsiDirectory,
    ): Array<PsiElement> {
        val method = AutomationTemplateCreateAction::class.java.getDeclaredMethod(
            "create",
            String::class.java,
            PsiDirectory::class.java,
        )
        method.isAccessible = true
        return method.invoke(this, newName, directory) as Array<PsiElement>
    }

    private fun String.withTrailingNewline(): String = "$this\n"
}



