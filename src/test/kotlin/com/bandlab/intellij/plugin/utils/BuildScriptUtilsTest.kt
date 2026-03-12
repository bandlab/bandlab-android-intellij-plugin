package com.bandlab.intellij.plugin.utils

import com.google.common.truth.Truth.assertThat
import com.intellij.mock.MockProject
import com.intellij.openapi.util.Disposer
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BuildScriptUtilsTest {

    @get:Rule val tmpFolder = TemporaryFolder()

    @Test
    fun `isAndroidModule returns true when groovy build script contains Android plugin`() {
        val projectDir = tmpFolder.newFolder("groovy-module")
        File(projectDir, "build.gradle").writeText("plugins { id(\"bandlab.plugins.library.android\") }")

        val project = createProject(projectDir.absolutePath)

        assertThat(project.isAndroidModule()).isTrue()
    }

    @Test
    fun `isAndroidModule returns true when kotlin build script contains Android plugin`() {
        val projectDir = tmpFolder.newFolder("kts-module")
        File(projectDir, "settings.gradle.kts").writeText("rootProject.name = \"kts-module\"")
        File(projectDir, "build.gradle.kts").writeText("plugins { id(\"bandlab.plugins.library.android\") }")

        val project = createProject(projectDir.absolutePath)

        assertThat(project.isAndroidModule()).isTrue()
    }

    @Test
    fun `isAndroidModule returns false when matching build script is missing`() {
        val projectDir = tmpFolder.newFolder("missing-build-script")

        val project = createProject(projectDir.absolutePath)

        assertThat(project.isAndroidModule()).isFalse()
    }

    @Test
    fun `isAndroidModule returns false when build script does not contain Android plugin`() {
        val projectDir = tmpFolder.newFolder("non-android-module")
        File(projectDir, "build.gradle").writeText("plugins { id(\"bandlab.plugins.library\") }")

        val project = createProject(projectDir.absolutePath)

        assertThat(project.isAndroidModule()).isFalse()
    }

    @Suppress("UnstableApiUsage")
    private fun createProject(basePath: String): MockProject {
        return object : MockProject(null, Disposer.newDisposable()) {
            override fun getBasePath(): String = basePath
        }
    }
}
