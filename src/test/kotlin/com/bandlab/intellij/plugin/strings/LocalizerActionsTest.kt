package com.bandlab.intellij.plugin.strings

import com.bandlab.intellij.plugin.localizer.LocalizerAction
import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

class LocalizerActionsTest : BasePlatformTestCase() {

    // BasePlatformTestCase reuses the project basePath across methods and we write a real manifest
    // file, so wipe it for per-method isolation.
    override fun setUp() {
        super.setUp()
        File(requireNotNull(project.basePath), "localizer").deleteRecursively()
    }

    fun testActionsVisibleWhenConfigured() {
        writeManifest(VALID_MANIFEST)
        actions().forEach { assertThat(visibilityOf(it)).isTrue() }
    }

    fun testActionsHiddenWithoutManifest() {
        actions().forEach { assertThat(visibilityOf(it)).isFalse() }
    }

    fun testActionsHiddenWhenManifestInvalid() {
        writeManifest(
            """
            api-key = "k"
            timeout = "5 parsecs"
            """.trimIndent(),
        )
        actions().forEach { assertThat(visibilityOf(it)).isFalse() }
    }

    private fun actions(): List<LocalizerAction> =
        listOf(UpdateStringsAction(), AddStringsAction(), DeleteStringsAction())

    private fun visibilityOf(action: LocalizerAction): Boolean {
        val event = TestActionEvent.createTestEvent(
            action,
            SimpleDataContext.builder().add(CommonDataKeys.PROJECT, project).build(),
        )
        action.update(event)
        return event.presentation.isEnabledAndVisible
    }

    private fun writeManifest(content: String) {
        val file = File(requireNotNull(project.basePath), "localizer/bandlab-localizer-config.toml")
        file.parentFile.mkdirs()
        file.writeText(content)
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
    }

    private companion object {
        val VALID_MANIFEST = """
            api-key = "k"
            project-id = "1"

            [[file]]
            locale = "en"
            path = "app/src/main/res/values/strings.xml"
        """.trimIndent()
    }
}
