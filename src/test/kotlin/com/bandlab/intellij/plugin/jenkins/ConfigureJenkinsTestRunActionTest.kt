package com.bandlab.intellij.plugin.jenkins

import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ConfigureJenkinsTestRunActionTest : BasePlatformTestCase() {

    fun testVisibleForAndroidTestFileWithTests() {
        val file = addKotlin(
            "feature/src/androidTest/kotlin/com/bandlab/feature/FooTest.kt",
            """
            package com.bandlab.feature

            import org.junit.Test

            class FooTest {
                @Test
                fun doesThing() {}
            }
            """.trimIndent(),
        )

        assertThat(isActionVisible(file)).isTrue()
    }

    fun testHiddenForAndroidTestFileWithoutTests() {
        val file = addKotlin(
            "feature/src/androidTest/kotlin/com/bandlab/feature/Helpers.kt",
            """
            package com.bandlab.feature

            class Helpers {
                fun helper() {}
            }
            """.trimIndent(),
        )

        assertThat(isActionVisible(file)).isFalse()
    }

    fun testHiddenForMainSourceFileEvenWithTests() {
        val file = addKotlin(
            "feature/src/main/kotlin/com/bandlab/feature/Foo.kt",
            """
            package com.bandlab.feature

            import org.junit.Test

            class Foo {
                @Test
                fun doesThing() {}
            }
            """.trimIndent(),
        )

        assertThat(isActionVisible(file)).isFalse()
    }

    private fun addKotlin(path: String, content: String): VirtualFile =
        myFixture.addFileToProject(path, content).virtualFile

    private fun isActionVisible(file: VirtualFile): Boolean {
        val dataContext = SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, project)
            .add(CommonDataKeys.VIRTUAL_FILE, file)
            .build()
        val action = ConfigureJenkinsTestRunAction()
        val event = TestActionEvent.createTestEvent(action, dataContext)

        action.update(event)

        return event.presentation.isVisible && event.presentation.isEnabled
    }
}
