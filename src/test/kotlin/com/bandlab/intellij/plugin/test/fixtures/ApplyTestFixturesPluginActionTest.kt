package com.bandlab.intellij.plugin.test.fixtures

import com.bandlab.intellij.plugin.module.ModuleInfo
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

class ApplyTestFixturesPluginActionTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = File(".").absolutePath

    fun testUpdateIsVisibleOnlyForBuildScripts() {
        val action = ApplyTestFixturesPluginAction()
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

    fun testActionPerformedAddsPluginSortsEntriesAndCreatesTestFixturesFolder() {
        val action = ApplyTestFixturesPluginAction()
        val buildFile = createProjectFile(
            "feature-profile/edit-screen/build.gradle.kts",
            buildGradleContent()
        )

        action.actionPerformed(createEvent(action, buildFile))

        assertThat(project.readFile(buildFile.path, isAbsolute = true))
            .isEqualTo(expectedBuildGradleContent().withTrailingNewline())

        val moduleInfo = ModuleInfo(path = "/feature-profile/edit-screen")
        val testFixturesFolder = File(requireNotNull(project.basePath) + moduleInfo.testFixturesPath)
        assertThat(testFixturesFolder.isDirectory).isTrue()
    }

    fun testActionPerformedIsIdempotent() {
        val action = ApplyTestFixturesPluginAction()
        val buildFile = createProjectFile(
            "feature-profile/edit-screen/build.gradle.kts",
            buildGradleContent()
        )

        action.actionPerformed(createEvent(action, buildFile))
        val contentAfterFirstRun = project.readFile(buildFile.path, isAbsolute = true)

        action.actionPerformed(createEvent(action, buildFile))
        val contentAfterSecondRun = project.readFile(buildFile.path, isAbsolute = true)

        assertThat(contentAfterSecondRun).isEqualTo(contentAfterFirstRun)
        val pluginDeclaration = "alias(bandlab.plugins.testFixtures)"
        assertThat(Regex(Regex.escape(pluginDeclaration)).findAll(contentAfterSecondRun).count())
            .isEqualTo(1)
    }

    private fun createEvent(
        action: ApplyTestFixturesPluginAction,
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

    private fun buildGradleContent(): String =
        """
        plugins {
            alias(bandlab.plugins.library.kotlin)
            alias(bandlab.plugins.compose)
        }

        dependencies {
            implementation(project(":zeta"))
            api(project(":alpha"))
        }
        """.trimIndent()

    private fun expectedBuildGradleContent(): String =
        """
        plugins {
            alias(bandlab.plugins.library.kotlin)
            alias(bandlab.plugins.compose)
            alias(bandlab.plugins.testFixtures)
        }

        dependencies {
            api(project(":alpha"))
            implementation(project(":zeta"))
        }
        """.trimIndent()

    private fun String.withTrailingNewline(): String = "$this\n"
}
