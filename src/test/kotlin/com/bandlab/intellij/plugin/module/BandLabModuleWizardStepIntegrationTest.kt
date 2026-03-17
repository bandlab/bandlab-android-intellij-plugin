package com.bandlab.intellij.plugin.module

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.bandlab.intellij.plugin.utils.SnapshotFlowRule
import com.intellij.openapi.application.EDT
import com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.runInEdtAndWait
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import java.io.File

class BandLabModuleWizardStepIntegrationTest : BasePlatformTestCase() {

    @get:Rule
    val snapshotFlowRule = SnapshotFlowRule()

    fun `test onWizardFinished creates expected modules and files`() {
        mockEnvironment()
        val moduleParent = ":features"
        val wizardStep = createWizardStep(moduleParent)
        val viewModel = wizardStep.viewModel
        val state = viewModel.state

        setModuleName(viewModel, ":features:profile")
        state.featureName.setTextAndPlaceCursorAtEnd(name)

        // Select all module types
        (state.apiConfig as MutableStateFlow<BandLabModuleConfig.Api>).value =
            BandLabModuleConfig.Api(isSelected = true)
        (state.implConfig as MutableStateFlow<BandLabModuleConfig.Impl>).value = BandLabModuleConfig.Impl(
            isSelected = true,
            typeSelection = ModuleTypeSelection.RequireSelection(BandLabModuleType.Android)
        )
        (state.uiConfig as MutableStateFlow<BandLabModuleConfig.Ui>).value = BandLabModuleConfig.Ui(isSelected = true)
        (state.screenConfig as MutableStateFlow<BandLabModuleConfig.Screen>).value =
            BandLabModuleConfig.Screen(isSelected = true)

        wizardStep.onWizardFinished()

        val modulePath = "features/profile"
        assertExists("$modulePath/api/build.gradle.kts")
        assertExists("$modulePath/impl/build.gradle.kts")
        assertExists("$modulePath/ui/build.gradle.kts")
        assertExists("$modulePath/screen/build.gradle.kts")

        val screenGradle = File(project.basePath, "$modulePath/screen/build.gradle.kts").readText()
        assertTrue("Screen should depend on UI", screenGradle.contains("project(\":features:profile:ui\")"))
        assertTrue("Screen should depend on API", screenGradle.contains("api(project(\":features:profile:api\"))"))
    }

    fun `test selecting screen module automatically selects ui module`() {
        val moduleParent = ":features"
        val wizardStep = createWizardStep(moduleParent)
        val viewModel = wizardStep.viewModel
        val state = viewModel.state

        (state.uiConfig as MutableStateFlow<BandLabModuleConfig.Ui>).value = BandLabModuleConfig.Ui(isSelected = false)
        (state.screenConfig as MutableStateFlow<BandLabModuleConfig.Screen>).value =
            BandLabModuleConfig.Screen(isSelected = false)

        state.onConfigClick(BandLabModuleConfig.Screen(isSelected = false))

        assertTrue("UI module should be automatically selected", state.uiConfig.value.isSelected)
        assertTrue("Screen module should be selected", state.screenConfig.value.isSelected)
    }

    fun `test module name validation errors`() {
        mockEnvironment()
        val wizardStep = createWizardStep(":")
        val viewModel = wizardStep.viewModel
        val state = viewModel.state

        setModuleName(viewModel, "")
        assertTrue(
            "Empty name error expected, got ${state.validationErrors.value}",
            state.validationErrors.value.contains(ModuleValidationError.ModuleNameEmpty)
        )

        setModuleName(viewModel, "no-colon")
        assertTrue(
            "Should start with colon error expected",
            state.validationErrors.value.contains(ModuleValidationError.ModuleNameShouldStartWithColon)
        )

        setModuleName(viewModel, ":ends-with:")
        assertTrue(
            "Ends with colon error expected",
            state.validationErrors.value.contains(ModuleValidationError.ModuleNameEndsWithColon)
        )

        setModuleName(viewModel, ":Invalid-Char")
        assertTrue(
            "Invalid char error expected",
            state.validationErrors.value.contains(ModuleValidationError.ModuleNameInvalidChar)
        )

        setModuleName(viewModel, ":features:profile:api")
        assertTrue(
            "Ends with config error expected",
            state.validationErrors.value.contains(ModuleValidationError.ModuleNameEndsWithConfig)
        )
    }

    fun `test module existence validation`() {
        mockEnvironment() // adds :existing:module to all-projects.txt
        val basePath = project.basePath!!
        runInEdtAndWait {
            runWriteCommandAction(project) {
                val allProjectsFile = File(basePath, "gradle/all-projects.txt")
                allProjectsFile.appendText(":existing:module:api\n")
            }
            VfsUtil.findFileByIoFile(File(basePath, "gradle/all-projects.txt"), true)?.refresh(false, false)
        }

        val wizardStep = createWizardStep(":existing")
        val viewModel = wizardStep.viewModel
        val state = viewModel.state

        setModuleName(viewModel, ":existing:module")
        assertTrue(
            "API module exists error should be present, got ${state.validationErrors.value}",
            state.validationErrors.value.contains(ModuleValidationError.ApiModuleExist)
        )
    }

    //TODO: Start from this
    fun `test feature name derivation`() {
        mockEnvironment()
        val wizardStep = createWizardStep(":")
        val viewModel = wizardStep.viewModel
        val state = viewModel.state

        setModuleName(viewModel, ":features:user-profile")
        assertEquals("FeaturesUserProfile", state.featureName.text)

        setModuleName(viewModel, ":auth")
        assertEquals("Auth", state.featureName.text)
    }

    fun `test canCreate state logic`() {
        mockEnvironment()
        val wizardStep = createWizardStep(":features:profile")
        val viewModel = wizardStep.viewModel
        val state = viewModel.state

        // Initial state: feature name is "FeaturesProfile", but no modules selected
        (state.apiConfig as MutableStateFlow<BandLabModuleConfig.Api>).value =
            BandLabModuleConfig.Api(isSelected = false)
        (state.implConfig as MutableStateFlow<BandLabModuleConfig.Impl>).value =
            BandLabModuleConfig.Impl(isSelected = false)
        (state.uiConfig as MutableStateFlow<BandLabModuleConfig.Ui>).value =
            BandLabModuleConfig.Ui(isSelected = false)
        (state.screenConfig as MutableStateFlow<BandLabModuleConfig.Screen>).value =
            BandLabModuleConfig.Screen(isSelected = false)
        assertFalse("Should not be able to create when no modules selected", viewModel.canCreate.get())

        // Select API
        (state.apiConfig as MutableStateFlow<BandLabModuleConfig.Api>).value =
            BandLabModuleConfig.Api(isSelected = true)
        assertTrue("Should be able to create when API is selected", viewModel.canCreate.get())

        // Select Impl without type
        (state.apiConfig as MutableStateFlow<BandLabModuleConfig.Api>).value =
            BandLabModuleConfig.Api(isSelected = false)
        (state.implConfig as MutableStateFlow<BandLabModuleConfig.Impl>).value =
            BandLabModuleConfig.Impl(isSelected = true, typeSelection = ModuleTypeSelection.RequireSelection(null))
        assertFalse("Should not be able to create when Impl is selected without type", viewModel.canCreate.get())

        // Select Impl with type
        (state.implConfig as MutableStateFlow<BandLabModuleConfig.Impl>).value = BandLabModuleConfig.Impl(
            isSelected = true,
            typeSelection = ModuleTypeSelection.RequireSelection(BandLabModuleType.Kotlin)
        )
        assertTrue("Should be able to create when Impl is selected with type", viewModel.canCreate.get())

        setModuleName(viewModel, ":Invalid Name")
        assertFalse("Should not be able to create when module name is invalid", viewModel.canCreate.get())
    }

    fun `test onWizardFinished with Kotlin Impl and plugins`() {
        mockEnvironment()
        val wizardStep = createWizardStep(":features:auth")
        val viewModel = wizardStep.viewModel
        val state = viewModel.state

        setModuleName(viewModel, ":features:auth")

        (state.apiConfig as MutableStateFlow<BandLabModuleConfig.Api>).value = BandLabModuleConfig.Api(
            isSelected = true,
            selectedPlugins = setOf(ModulePlugin.RestApi)
        )
        (state.implConfig as MutableStateFlow<BandLabModuleConfig.Impl>).value = BandLabModuleConfig.Impl(
            isSelected = true,
            typeSelection = ModuleTypeSelection.RequireSelection(BandLabModuleType.Kotlin),
            selectedPlugins = setOf(ModulePlugin.Database)
        )

        wizardStep.onWizardFinished()

        val modulePath = "features/auth"
        assertExists("$modulePath/api/build.gradle.kts")
        assertExists("$modulePath/impl/build.gradle.kts")

        val apiGradle = File(project.basePath, "$modulePath/api/build.gradle.kts").readText()
        assertTrue("API should have restApi plugin", apiGradle.contains("alias(bandlab.plugins.restApi)"))
        assertTrue("API should have library.kotlin plugin", apiGradle.contains("alias(bandlab.plugins.library.kotlin)"))

        val implGradle = File(project.basePath, "$modulePath/impl/build.gradle.kts").readText()
        assertTrue("Impl should have database plugin", implGradle.contains("alias(bandlab.plugins.database)"))
        assertTrue(
            "Impl should have library.kotlin plugin",
            implGradle.contains("alias(bandlab.plugins.library.kotlin)")
        )
        assertTrue("Impl should depend on API", implGradle.contains("api(project(\":features:auth:api\"))"))
    }

    fun `test module plugin toggling`() {
        val wizardStep = createWizardStep(":")
        val state = wizardStep.viewModel.state

        state.onConfigClick(BandLabModuleConfig.Api(isSelected = false))
        state.onPluginClick(state.apiConfig.value, ModulePlugin.RestApi)
        assertTrue(
            "RestApi plugin should be selected",
            state.apiConfig.value.selectedPlugins.contains(ModulePlugin.RestApi)
        )

        state.onPluginClick(state.apiConfig.value, ModulePlugin.RestApi)
        assertFalse(
            "RestApi plugin should be deselected",
            state.apiConfig.value.selectedPlugins.contains(ModulePlugin.RestApi)
        )
    }

    fun `test module exposure selection`() {
        val wizardStep = createWizardStep(":")
        val state = wizardStep.viewModel.state

        state.onExposureClick(state.implConfig.value, ModuleExposure.MixEditorGraph)
        assertEquals(
            "Impl exposure should be MixEditorGraph",
            ModuleExposure.MixEditorGraph,
            state.implConfig.value.exposure
        )

        state.onExposureClick(state.screenConfig.value, ModuleExposure.None)
        assertEquals("Screen exposure should be None", ModuleExposure.None, state.screenConfig.value.exposure)
    }

    fun `test screen template toggling`() {
        val wizardStep = createWizardStep(":")
        val state = wizardStep.viewModel.state

        state.onGenerateActivityClick()
        assertEquals(
            "Template should be Activity",
            BandLabModuleConfig.Screen.Template.Activity,
            state.screenConfig.value.template
        )

        state.onGenerateActivityClick()
        assertNull("Template should be null after second click", state.screenConfig.value.template)

        state.onGeneratePageClick()
        assertEquals(
            "Template should be Page",
            BandLabModuleConfig.Screen.Template.Page,
            state.screenConfig.value.template
        )

        state.onGeneratePageClick()
        assertNull("Template should be null after second click", state.screenConfig.value.template)
    }

    fun `test module type selection`() {
        val wizardStep = createWizardStep(":")
        val state = wizardStep.viewModel.state

        state.onModuleTypeClick(state.implConfig.value, BandLabModuleType.Kotlin)
        assertEquals("Impl type should be Kotlin", BandLabModuleType.Kotlin, state.implConfig.value.typeSelection.type)

        state.onModuleTypeClick(state.implConfig.value, BandLabModuleType.Android)
        assertEquals(
            "Impl type should be Android",
            BandLabModuleType.Android,
            state.implConfig.value.typeSelection.type
        )
    }

    fun `test all module existence validation errors`() {
        mockEnvironment()
        val basePath = project.basePath!!
        runInEdtAndWait {
            runWriteCommandAction(project) {
                val allProjectsFile = File(basePath, "gradle/all-projects.txt")
                allProjectsFile.appendText(":existing:impl-module:impl\n")
                allProjectsFile.appendText(":existing:ui-module:ui\n")
                allProjectsFile.appendText(":existing:screen-module:screen\n")
            }
            VfsUtil.findFileByIoFile(File(basePath, "gradle/all-projects.txt"), true)?.refresh(false, false)
        }

        val wizardStep = createWizardStep(":")
        val viewModel = wizardStep.viewModel
        val state = viewModel.state

        setModuleName(viewModel, ":existing:impl-module")
        assertTrue(
            "Impl module exists error expected, got ${state.validationErrors.value}",
            state.validationErrors.value.contains(ModuleValidationError.ImplModuleExist)
        )

        setModuleName(viewModel, ":existing:ui-module")
        assertTrue(
            "UI module exists error expected, got ${state.validationErrors.value}",
            state.validationErrors.value.contains(ModuleValidationError.UiModuleExist)
        )

        setModuleName(viewModel, ":existing:screen-module")
        assertTrue(
            "Screen module exists error expected, got ${state.validationErrors.value}",
            state.validationErrors.value.contains(ModuleValidationError.ScreenModuleExist)
        )
    }

    private fun createWizardStep(moduleParent: String): BandLabModuleWizardStep {
        return BandLabModuleWizardStep(
            project = project,
            moduleParent = moduleParent,
            projectSyncInvoker = object : ProjectSyncInvoker {
                override fun syncProject(project: Project) {}
            },
            wizardScope = CoroutineScope(Dispatchers.EDT)
        )
    }

    private fun setModuleName(viewModel: BandLabModuleWizardViewModel, name: String) {
        viewModel.state.moduleRoot.setTextAndPlaceCursorAtEnd(name)
    }

    private fun mockEnvironment() {
        val basePath = project.basePath ?: return
        val settingsFile = File(basePath, "settings.gradle.kts")
        val allProjectsFile = File(basePath, "gradle/all-projects.txt")
        val appBuildGradle = File(basePath, "app/build.gradle.kts")
        val rootBuildGradle = File(basePath, "build.gradle.kts")

        runInEdtAndWait {
            runWriteCommandAction(project) {
                if (!settingsFile.exists()) {
                    settingsFile.parentFile.mkdirs()
                    settingsFile.writeText("// settings.gradle.kts\n")
                }
                if (!allProjectsFile.exists()) {
                    allProjectsFile.parentFile.mkdirs()
                    allProjectsFile.writeText("# All Modules\n:existing:module\n\n")
                }
                if (!appBuildGradle.exists()) {
                    appBuildGradle.parentFile.mkdirs()
                    appBuildGradle.writeText("dependencies {\n    implementation(\"some-lib\")\n}\n")
                }
                if (!rootBuildGradle.exists()) {
                    rootBuildGradle.parentFile.mkdirs()
                    rootBuildGradle.writeText("// root build.gradle.kts\n")
                }
            }
        }
    }

    private fun assertExists(relativePath: String) {
        val file = File(project.basePath, relativePath)
        assertTrue("File $relativePath should exist", file.exists())
    }
}
