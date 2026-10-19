// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.module

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import app.cash.turbine.test
import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.bandlab.intellij.plugin.module.ui.WizardState
import com.bandlab.intellij.plugin.utils.writeTestFile
import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class BandLabModuleWizardStepIntegrationTest : BasePlatformTestCase() {

    fun `test onWizardFinished creates expected modules and files`() = runTest {
        mockEnvironment()
        val wizardStep = createWizardStep(":features")
        val state = wizardStep.state

        state.setModuleRoot(":features:profile")
        state.featureName.setTextAndPlaceCursorAtEnd(name)

        // Select all module types
        (state.apiConfig as MutableStateFlow<BandLabModuleConfig.Api>).value =
            BandLabModuleConfig.Api(isSelected = true)
        (state.implConfig as MutableStateFlow<BandLabModuleConfig.Impl>).value =
            BandLabModuleConfig.Impl(
                isSelected = true,
                typeSelection = ModuleTypeSelection.RequireSelection(BandLabModuleType.Android),
            )
        (state.uiConfig as MutableStateFlow<BandLabModuleConfig.Ui>).value =
            BandLabModuleConfig.Ui(isSelected = true)
        (state.screenConfig as MutableStateFlow<BandLabModuleConfig.Screen>).value =
            BandLabModuleConfig.Screen(isSelected = true)

        wizardStep.onWizardFinished()

        val modulePath = "features/profile"
        assertExists("$modulePath/api/build.gradle")
        assertExists("$modulePath/impl/build.gradle")
        assertExists("$modulePath/ui/build.gradle")
        assertExists("$modulePath/screen/build.gradle")

        val screenGradle = File(project.basePath, "$modulePath/screen/build.gradle").readText()
        assertTrue(
            "Screen should depend on UI",
            screenGradle.contains("project(\":features:profile:ui\")"),
        )
        assertTrue(
            "Screen should depend on API",
            screenGradle.contains("api(project(\":features:profile:api\"))"),
        )
    }

    fun `test selecting screen module automatically selects ui module`() = runTest {
        val moduleParent = ":features"
        val wizardStep = createWizardStep(moduleParent)
        val state = wizardStep.state

        (state.uiConfig as MutableStateFlow<BandLabModuleConfig.Ui>).value =
            BandLabModuleConfig.Ui(isSelected = false)
        (state.screenConfig as MutableStateFlow<BandLabModuleConfig.Screen>).value =
            BandLabModuleConfig.Screen(isSelected = false)

        state.onConfigClick(BandLabModuleConfig.Screen(isSelected = false))

        assertTrue("UI module should be automatically selected", state.uiConfig.value.isSelected)
        assertTrue("Screen module should be selected", state.screenConfig.value.isSelected)
    }

    fun `test module name validation errors`() = runTest {
        mockEnvironment()
        val wizardStep = createWizardStep(":")
        val state = wizardStep.state

        state.validationErrors.test {
            assertThat(awaitItem()).isEmpty()
            assertThat(awaitItem()).containsExactly(ModuleValidationError.ModuleNameEmpty)

            state.setModuleRoot("no-colon")
            assertThat(awaitItem())
                .containsExactly(ModuleValidationError.ModuleNameShouldStartWithColon)

            state.setModuleRoot(":ends-with:")
            assertThat(awaitItem()).containsExactly(ModuleValidationError.ModuleNameEndsWithColon)

            state.setModuleRoot(":Invalid-Char")
            assertThat(awaitItem()).containsExactly(ModuleValidationError.ModuleNameInvalidChar)

            state.setModuleRoot(":features:profile")
            assertThat(awaitItem()).isEmpty()
        }
    }

    fun `test module existence validation`() = runTest {
        mockEnvironment()
        project.writeTestFile("gradle/all-projects.txt", "# All Modules\n:existing:module:api\n")

        val wizardStep = createWizardStep(":existing:module")
        val state = wizardStep.state

        state.validationErrors.test {
            awaitItem()
            assertThat(awaitItem()).isEqualTo(setOf(ModuleValidationError.ApiModuleExist))
        }
    }

    fun `test feature name derivation`() = runTest {
        mockEnvironment()
        val wizardStep = createWizardStep(":")
        val state = wizardStep.state

        snapshotFlow { state.featureName.text }
            .test {
                assertThat(awaitItem()).isEqualTo("")

                state.setModuleRoot(":features:user-profile")
                assertThat(awaitItem()).isEqualTo("FeaturesUserProfile")

                state.setModuleRoot(":auth")
                assertThat(awaitItem()).isEqualTo("Auth")
            }
    }

    fun `test onWizardFinished with Kotlin Impl and plugins`() = runTest {
        mockEnvironment()
        val wizardStep = createWizardStep(":features:auth")
        val state = wizardStep.state

        (state.apiConfig as MutableStateFlow<BandLabModuleConfig.Api>).value =
            BandLabModuleConfig.Api(
                isSelected = true,
                selectedPlugins = setOf(ModulePlugin.RestApi),
            )
        (state.implConfig as MutableStateFlow<BandLabModuleConfig.Impl>).value =
            BandLabModuleConfig.Impl(
                isSelected = true,
                typeSelection = ModuleTypeSelection.RequireSelection(BandLabModuleType.Kotlin),
                selectedPlugins = setOf(ModulePlugin.Database),
            )

        wizardStep.onWizardFinished()

        val modulePath = "features/auth"
        assertExists("$modulePath/api/build.gradle")
        assertExists("$modulePath/impl/build.gradle")

        val apiGradle = File(project.basePath, "$modulePath/api/build.gradle").readText()
        assertTrue(
            "API should have restApi plugin",
            apiGradle.contains("alias(bandlab.plugins.restApi)"),
        )
        assertTrue(
            "API should have library.kotlin plugin",
            apiGradle.contains("alias(bandlab.plugins.library.kotlin)"),
        )

        val implGradle = File(project.basePath, "$modulePath/impl/build.gradle").readText()
        assertTrue(
            "Impl should have database plugin",
            implGradle.contains("alias(bandlab.plugins.database)"),
        )
        assertTrue(
            "Impl should have library.kotlin plugin",
            implGradle.contains("alias(bandlab.plugins.library.kotlin)"),
        )
        assertTrue(
            "Impl should depend on API",
            implGradle.contains("api(project(\":features:auth:api\"))"),
        )
    }

    fun `test module plugin toggling`() = runTest {
        val wizardStep = createWizardStep(":")
        val state = wizardStep.state

        state.onConfigClick(BandLabModuleConfig.Api(isSelected = false))
        state.onPluginClick(state.apiConfig.value, ModulePlugin.RestApi)
        assertTrue(
            "RestApi plugin should be selected",
            state.apiConfig.value.selectedPlugins.contains(ModulePlugin.RestApi),
        )

        state.onPluginClick(state.apiConfig.value, ModulePlugin.RestApi)
        assertFalse(
            "RestApi plugin should be deselected",
            state.apiConfig.value.selectedPlugins.contains(ModulePlugin.RestApi),
        )
    }

    fun `test module exposure selection`() = runTest {
        val wizardStep = createWizardStep(":")
        val state = wizardStep.state

        state.onExposureClick(state.implConfig.value, ModuleExposure.MixEditorGraph)
        assertEquals(
            "Impl exposure should be MixEditorGraph",
            ModuleExposure.MixEditorGraph,
            state.implConfig.value.exposure,
        )

        state.onExposureClick(state.screenConfig.value, ModuleExposure.None)
        assertEquals(
            "Screen exposure should be None",
            ModuleExposure.None,
            state.screenConfig.value.exposure,
        )
    }

    fun `test screen template toggling`() = runTest {
        val wizardStep = createWizardStep(":")
        val state = wizardStep.state

        state.onTemplateSelection(BandLabModuleConfig.Screen.Template.Page)
        assertEquals(
            "Template should be Page",
            BandLabModuleConfig.Screen.Template.Page,
            state.screenConfig.value.template,
        )

        state.onTemplateSelection(BandLabModuleConfig.Screen.Template.PageWithNavKey)
        assertEquals(
            "Template should be change to PageWithNavKey",
            BandLabModuleConfig.Screen.Template.PageWithNavKey,
            state.screenConfig.value.template,
        )

        state.onTemplateSelection(BandLabModuleConfig.Screen.Template.PageWithNavKey)
        assertNull("Template should be null after second click", state.screenConfig.value.template)
    }

    fun `test module type selection`() = runTest {
        val wizardStep = createWizardStep(":")
        val state = wizardStep.state

        state.onModuleTypeClick(state.implConfig.value, BandLabModuleType.Kotlin)
        assertEquals(
            "Impl type should be Kotlin",
            BandLabModuleType.Kotlin,
            state.implConfig.value.typeSelection.type,
        )

        state.onModuleTypeClick(state.implConfig.value, BandLabModuleType.Android)
        assertEquals(
            "Impl type should be Android",
            BandLabModuleType.Android,
            state.implConfig.value.typeSelection.type,
        )
    }

    fun `test all module existence validation errors`() = runTest {
        mockEnvironment()
        project.writeTestFile(
            "gradle/all-projects.txt",
            "# All Modules\n:foo:impl\n:foo:ui\n:foo:screen\n",
        )

        val wizardStep = createWizardStep(":foo")
        val state = wizardStep.state

        state.validationErrors.test {
            awaitItem()
            assertThat(awaitItem())
                .containsExactly(
                    ModuleValidationError.ImplModuleExist,
                    ModuleValidationError.UiModuleExist,
                    ModuleValidationError.ScreenModuleExist,
                )
        }
    }

    private fun TestScope.createWizardStep(moduleParent: String): BandLabModuleWizardStep {
        return BandLabModuleWizardStep(
            project = project,
            moduleParent = moduleParent,
            projectSyncInvoker =
                object : ProjectSyncInvoker {
                    override fun syncProject(project: Project) {}
                },
            wizardScope = backgroundScope,
            ioDispatcher = testScheduler,
        )
    }

    private suspend fun WizardState.setModuleRoot(name: String) {
        moduleRoot.setTextAndPlaceCursorAtEnd(name)
        // Without this the text flow won't emit
        snapshotFlow { moduleRoot.text }.first()
    }

    private fun mockEnvironment() {
        project.writeTestFile("settings.gradle", "// settings.gradle\n")
        project.writeTestFile("gradle/all-projects.txt", "# All Modules\n:existing:module\n\n")
        project.writeTestFile(
            "app/build.gradle",
            "dependencies {\n    implementation(\"some-lib\")\n}\n",
        )
        project.writeTestFile("build.gradle", "// root build.gradle\n")
    }

    private fun assertExists(relativePath: String) {
        val file = File(project.basePath, relativePath)
        assertTrue("File $relativePath should exist", file.exists())
    }
}
