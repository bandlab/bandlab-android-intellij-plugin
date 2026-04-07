package com.bandlab.intellij.plugin.strings

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

class UpdateStringsActionTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = File(".").absolutePath

    fun testUpdateIsVisibleForStringsXml() {
        val action = UpdateStringsAction()
        val stringsXmlFile = createProjectFile("feature/src/main/res/values/strings.xml", "<resources></resources>")
        val event = createEvent(action, stringsXmlFile)
        
        action.update(event)
        assertThat(event.presentation.isEnabledAndVisible).isTrue()
    }

    fun testUpdateIsVisibleForStringsPluralsXml() {
        val action = UpdateStringsAction()
        val stringsPluralsXmlFile = createProjectFile("feature/src/main/res/values/strings-plurals.xml", "<resources></resources>")
        val event = createEvent(action, stringsPluralsXmlFile)
        
        action.update(event)
        assertThat(event.presentation.isEnabledAndVisible).isTrue()
    }

    fun testUpdateIsVisibleForFileInModuleWithLocalizerPlugin() {
        val action = UpdateStringsAction()
        
        createProjectFile("settings.gradle.kts", "")
        createProjectFile("feature1/build.gradle.kts", "plugins {\n    alias(libs.plugins.localizer)\n}")
        val someFile = createProjectFile("feature1/src/main/java/com/example/SomeFile.kt", "class SomeFile")
        
        val event = createEvent(action, someFile)
        action.update(event)
        assertThat(event.presentation.isEnabledAndVisible).isTrue()
    }

    fun testUpdateIsNotVisibleForFileInModuleWithoutLocalizerPlugin() {
        val action = UpdateStringsAction()
        
        createProjectFile("settings.gradle.kts", "")
        createProjectFile("feature2/build.gradle.kts", "plugins {\n    alias(libs.plugins.compose)\n}")
        val someFile = createProjectFile("feature2/src/main/java/com/example/SomeFile.kt", "class SomeFile")
        
        val event = createEvent(action, someFile)
        action.update(event)
        assertThat(event.presentation.isEnabledAndVisible).isFalse()
    }

    fun testUpdateIsNotVisibleForRegularFileOutsideModule() {
        val action = UpdateStringsAction()
        val regularFile = createProjectFile("README.md", "# Docs")
        
        val event = createEvent(action, regularFile)
        action.update(event)
        assertThat(event.presentation.isEnabledAndVisible).isFalse()
    }

    fun testUpdateIsNotVisibleWhenNoVirtualFile() {
        val action = UpdateStringsAction()
        val event = TestActionEvent.createTestEvent(
            action,
            SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, project)
                .build()
        )
        action.update(event)
        assertThat(event.presentation.isEnabledAndVisible).isFalse()
    }

    fun testActionPerformedExitsWhenNoProject() {
        val action = UpdateStringsAction()
        val event = TestActionEvent.createTestEvent(action)
        // Should not throw and exit early
        action.actionPerformed(event)
    }

    fun testActionPerformedExitsWhenNoVirtualFile() {
        val action = UpdateStringsAction()
        val event = TestActionEvent.createTestEvent(
            action,
            SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, project)
                .build()
        )
        // Should not throw and exit early
        action.actionPerformed(event)
    }

    fun testActionPerformedExitsWhenNoGradleProject() {
        val action = UpdateStringsAction()
        val regularFile = createProjectFile("README.md", "# Docs")
        val event = createEvent(action, regularFile)
        
        // Should not throw and exit early since there's no build.gradle
        action.actionPerformed(event)
    }

    private fun createEvent(
        action: UpdateStringsAction,
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
}