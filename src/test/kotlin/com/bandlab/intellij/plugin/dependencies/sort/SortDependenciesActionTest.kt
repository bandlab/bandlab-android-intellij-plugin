package com.bandlab.intellij.plugin.dependencies.sort

import com.bandlab.intellij.plugin.utils.readFile
import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

class SortDependenciesActionTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = File(".").absolutePath

    fun testUpdateIsVisibleOnlyForBuildScripts() {
        val action = SortDependenciesAction()
        val kotlinBuildFile = createProjectFile("feature/build.gradle.kts", "plugins {}")
        val groovyBuildFile = createProjectFile("legacy/build.gradle", "plugins {}")
        val regularFile = createProjectFile("feature/README.md", "# docs")
        val kotlinEvent = createEvent(action, kotlinBuildFile)
        action.update(kotlinEvent)
        assertThat(kotlinEvent.presentation.isEnabledAndVisible).isTrue()
        val groovyEvent = createEvent(action, groovyBuildFile)
        action.update(groovyEvent)
        assertThat(groovyEvent.presentation.isEnabledAndVisible).isTrue()
        val regularEvent = createEvent(action, regularFile)
        action.update(regularEvent)
        assertThat(regularEvent.presentation.isEnabledAndVisible).isFalse()
    }

    fun testActionPerformedSortsPluginsAndDependenciesAndRemovesDuplicates() {
        val action = SortDependenciesAction()
        val buildFile = createProjectFile(
            "feature-profile/screen/build.gradle.kts",
            unsortedBuildGradleContent()
        )
        action.actionPerformed(createEvent(action, buildFile))
        assertThat(project.readFile(buildFile.path, isAbsolute = true))
            .isEqualTo(expectedBuildGradleContent().withTrailingNewline())
    }

    private fun createEvent(
        action: SortDependenciesAction,
        file: VirtualFile
    ): AnActionEvent {
        val psiFile = requireNotNull(PsiManager.getInstance(project).findFile(file))
        return TestActionEvent.createTestEvent(
            action,
            SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, project)
                .add(CommonDataKeys.PSI_ELEMENT, psiFile)
                .add(CommonDataKeys.PSI_FILE, psiFile)
                .add(CommonDataKeys.VIRTUAL_FILE, file)
                .build()
        )
    }

    private fun createProjectFile(relativePath: String, content: String): VirtualFile {
        val ioFile = File(requireNotNull(project.basePath), relativePath)
        ioFile.parentFile.mkdirs()
        ioFile.writeText(content)
        return requireNotNull(LocalFileSystem.getInstance().refreshAndFindFileByIoFile(ioFile))
    }

    private fun unsortedBuildGradleContent(): String =
        """
        plugins {
            alias(bandlab.plugins.compose)
            alias(bandlab.plugins.library.kotlin)
            alias(bandlab.plugins.metro)
            alias(bandlab.plugins.compose)
        }
        dependencies {
            implementation(project(":zeta"))
            api(project(":alpha"))
            implementation(project(":zeta"))
            compileOnly(project(":beta"))
        }
        """.trimIndent()

    private fun expectedBuildGradleContent(): String =
        """
        plugins {
            alias(bandlab.plugins.library.kotlin)
            alias(bandlab.plugins.compose)
            alias(bandlab.plugins.metro)
        }
        dependencies {
            api(project(":alpha"))
            compileOnly(project(":beta"))
            implementation(project(":zeta"))
        }
        """.trimIndent()

    private fun String.withTrailingNewline(): String = "$this\n"
}
