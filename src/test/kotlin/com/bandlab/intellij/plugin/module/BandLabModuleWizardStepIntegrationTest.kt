package com.bandlab.intellij.plugin.module

import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.bandlab.intellij.plugin.utils.Const.ALL_PROJECTS_PATH
import com.bandlab.intellij.plugin.utils.Const.DEPENDENCIES_END
import com.bandlab.intellij.plugin.utils.Const.DEPENDENCIES_START
import com.bandlab.intellij.plugin.utils.Const.PLUGINS_END
import com.bandlab.intellij.plugin.utils.Const.PLUGINS_START
import com.google.common.truth.Truth.assertThat
import com.intellij.mock.MockProject
import com.intellij.openapi.util.Disposer
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.io.path.createFile
import kotlin.io.path.writeText

class BandLabModuleWizardStepIntegrationTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    @Test
    fun `MockProject basePath override works correctly`() {
        val projectDir = tmpFolder.newFolder("mock-test")
        val project = createMockProject(projectDir)

        assertThat(project.basePath).isNotNull()
        assertThat(project.basePath).isEqualTo(projectDir.absolutePath)
    }

    @Test
    fun `creates single API module with correct structure`() {
        val projectDir = setupTestProject()
        val project = createMockProject(projectDir)

        val template = BandLabModuleTemplate(
            project = project,
            moduleInfo = ModuleInfo("/user/profile/api"),
            config = BandLabModuleConfig.Api(isSelected = true),
            featureName = "UserProfile",
            dependsOn = emptyList()
        )

        template.create()

        // Verify module directory structure created
        assertThat(File(projectDir, "user/profile/api/src/main/kotlin/com/bandlab/user/profile/api").exists()).isTrue()

        // Verify build.gradle.kts created
        val buildFile = File(projectDir, "user/profile/api/build.gradle.kts")
        assertThat(buildFile.exists()).isTrue()

        val buildContent = buildFile.readText()
        assertThat(buildContent).contains("alias(bandlab.plugins.library.kotlin)")
        assertThat(buildContent).contains(PLUGINS_START)
        assertThat(buildContent).contains(PLUGINS_END)
        assertThat(buildContent).contains(DEPENDENCIES_START)
        assertThat(buildContent).contains(DEPENDENCIES_END)
        assertThat(buildContent).doesNotContain("alias(bandlab.plugins.library.android)")

        // Verify module added to all-projects.txt
        val allProjectsFile = File(projectDir, "gradle/all-projects.txt")
        val allProjectsContent = allProjectsFile.readText()
        assertThat(allProjectsContent).contains(":user:profile:api")
    }

    @Test
    fun `creates single Impl module with Android type`() {
        val projectDir = setupTestProject()
        val project = createMockProject(projectDir)

        val template = BandLabModuleTemplate(
            project = project,
            moduleInfo = ModuleInfo("/user/profile/impl"),
            config = BandLabModuleConfig.Impl(
                isSelected = true,
                typeSelection = ModuleTypeSelection.RequireSelection(BandLabModuleType.Android),
                selectedPlugins = setOf(ModulePlugin.Metro, ModulePlugin.Database)
            ),
            featureName = "UserProfile",
            dependsOn = emptyList()
        )

        template.create()

        // Verify module directory created
        assertThat(File(projectDir, "user/profile/impl/src/main/kotlin/com/bandlab/user/profile/impl").exists()).isTrue()

        // Verify build.gradle.kts content
        val buildFile = File(projectDir, "user/profile/impl/build.gradle.kts")
        val buildContent = buildFile.readText()
        assertThat(buildContent).contains("alias(bandlab.plugins.library.android)")
        assertThat(buildContent).contains("alias(bandlab.plugins.metro)")
        assertThat(buildContent).contains("alias(bandlab.plugins.database)")
        assertThat(buildContent).doesNotContain("alias(bandlab.plugins.library.kotlin)")

        // Verify module added to all-projects.txt
        val allProjectsFile = File(projectDir, "gradle/all-projects.txt")
        assertThat(allProjectsFile.readText()).contains(":user:profile:impl")
    }

    @Test
    fun `creates single Impl module with Kotlin type`() {
        val projectDir = setupTestProject()
        val project = createMockProject(projectDir)

        val template = BandLabModuleTemplate(
            project = project,
            moduleInfo = ModuleInfo("/user/profile/impl"),
            config = BandLabModuleConfig.Impl(
                isSelected = true,
                typeSelection = ModuleTypeSelection.RequireSelection(BandLabModuleType.Kotlin),
                selectedPlugins = setOf(ModulePlugin.Metro)
            ),
            featureName = "UserProfile",
            dependsOn = emptyList()
        )

        template.create()

        // Verify build.gradle.kts content
        val buildFile = File(projectDir, "user/profile/impl/build.gradle.kts")
        val buildContent = buildFile.readText()
        assertThat(buildContent).contains("alias(bandlab.plugins.library.kotlin)")
        assertThat(buildContent).contains("alias(bandlab.plugins.metro)")
        assertThat(buildContent).doesNotContain("alias(bandlab.plugins.library.android)")
    }

    @Test
    fun `creates single UI module with Compose plugin`() {
        val projectDir = setupTestProject()
        val project = createMockProject(projectDir)

        val template = BandLabModuleTemplate(
            project = project,
            moduleInfo = ModuleInfo("/user/profile/ui"),
            config = BandLabModuleConfig.Ui(
                isSelected = true,
                selectedPlugins = setOf(ModulePlugin.Compose, ModulePlugin.Metro)
            ),
            featureName = "UserProfile",
            dependsOn = emptyList()
        )

        template.create()

        // Verify module directory created
        assertThat(File(projectDir, "user/profile/ui/src/main/kotlin/com/bandlab/user/profile/ui").exists()).isTrue()

        // Verify build.gradle.kts content
        val buildFile = File(projectDir, "user/profile/ui/build.gradle.kts")
        val buildContent = buildFile.readText()
        assertThat(buildContent).contains("alias(bandlab.plugins.library.android)")
        assertThat(buildContent).contains("alias(bandlab.plugins.compose)")
        assertThat(buildContent).contains("alias(bandlab.plugins.metro)")
    }

    @Test
    fun `creates single Screen module with all default plugins`() {
        val projectDir = setupTestProject()
        val project = createMockProject(projectDir)

        val template = BandLabModuleTemplate(
            project = project,
            moduleInfo = ModuleInfo("/user/profile/screen"),
            config = BandLabModuleConfig.Screen(
                isSelected = true,
                selectedPlugins = setOf(ModulePlugin.Compose, ModulePlugin.Metro, ModulePlugin.Screen)
            ),
            featureName = "UserProfile",
            dependsOn = emptyList()
        )

        template.create()

        // Verify module directory created
        assertThat(File(projectDir, "user/profile/screen/src/main/kotlin/com/bandlab/user/profile/screen").exists()).isTrue()

        // Verify build.gradle.kts content
        val buildFile = File(projectDir, "user/profile/screen/build.gradle.kts")
        val buildContent = buildFile.readText()
        assertThat(buildContent).contains("alias(bandlab.plugins.library.android)")
        assertThat(buildContent).contains("alias(bandlab.plugins.compose)")
        assertThat(buildContent).contains("alias(bandlab.plugins.metro)")
        assertThat(buildContent).contains("alias(bandlab.plugins.screen)")
    }

    @Test
    fun `creates multiple modules (API + Impl) with dependencies`() {
        val projectDir = setupTestProject()
        val project = createMockProject(projectDir)

        val apiModuleInfo = ModuleInfo("/user/profile/api")
        val implModuleInfo = ModuleInfo("/user/profile/impl")

        // Create API module first
        val apiTemplate = BandLabModuleTemplate(
            project = project,
            moduleInfo = apiModuleInfo,
            config = BandLabModuleConfig.Api(isSelected = true),
            featureName = "UserProfile",
            dependsOn = emptyList()
        )
        apiTemplate.create()

        // Create Impl module with API dependency
        val implTemplate = BandLabModuleTemplate(
            project = project,
            moduleInfo = implModuleInfo,
            config = BandLabModuleConfig.Impl(
                isSelected = true,
                typeSelection = ModuleTypeSelection.RequireSelection(BandLabModuleType.Android)
            ),
            featureName = "UserProfile",
            dependsOn = listOf(
                Dependency(
                    name = apiModuleInfo.projectAccessorReference,
                    config = DependencyConfiguration.Api
                )
            )
        )
        implTemplate.create()

        // Verify both modules created
        assertThat(File(projectDir, "user/profile/api/build.gradle.kts").exists()).isTrue()
        assertThat(File(projectDir, "user/profile/impl/build.gradle.kts").exists()).isTrue()

        // Verify dependency in impl module
        val implBuildFile = File(projectDir, "user/profile/impl/build.gradle.kts")
        val implBuildContent = implBuildFile.readText()
        assertThat(implBuildContent).contains("api(project(\":user:profile:api\"))")

        // Verify both modules in all-projects.txt
        val allProjectsContent = File(projectDir, "gradle/all-projects.txt").readText()
        assertThat(allProjectsContent).contains(":user:profile:api")
        assertThat(allProjectsContent).contains(":user:profile:impl")
    }

    @Test
    fun `creates all modules (API + Impl + UI + Screen) with correct dependencies`() {
        val projectDir = setupTestProject()
        val project = createMockProject(projectDir)

        val apiModuleInfo = ModuleInfo("/user/profile/api")
        val uiModuleInfo = ModuleInfo("/user/profile/ui")
        val implModuleInfo = ModuleInfo("/user/profile/impl")
        val screenModuleInfo = ModuleInfo("/user/profile/screen")

        // Create API module
        BandLabModuleTemplate(
            project = project,
            moduleInfo = apiModuleInfo,
            config = BandLabModuleConfig.Api(isSelected = true),
            featureName = "UserProfile",
            dependsOn = emptyList()
        ).create()

        // Create UI module with API dependency
        BandLabModuleTemplate(
            project = project,
            moduleInfo = uiModuleInfo,
            config = BandLabModuleConfig.Ui(isSelected = true),
            featureName = "UserProfile",
            dependsOn = listOf(
                Dependency(
                    name = apiModuleInfo.projectAccessorReference,
                    config = DependencyConfiguration.Api
                )
            )
        ).create()

        // Create Impl module with API dependency
        BandLabModuleTemplate(
            project = project,
            moduleInfo = implModuleInfo,
            config = BandLabModuleConfig.Impl(
                isSelected = true,
                typeSelection = ModuleTypeSelection.RequireSelection(BandLabModuleType.Android)
            ),
            featureName = "UserProfile",
            dependsOn = listOf(
                Dependency(
                    name = apiModuleInfo.projectAccessorReference,
                    config = DependencyConfiguration.Api
                )
            )
        ).create()

        // Create Screen module with UI and API dependencies
        BandLabModuleTemplate(
            project = project,
            moduleInfo = screenModuleInfo,
            config = BandLabModuleConfig.Screen(isSelected = true),
            featureName = "UserProfile",
            dependsOn = listOf(
                Dependency(name = uiModuleInfo.projectAccessorReference),
                Dependency(
                    name = apiModuleInfo.projectAccessorReference,
                    config = DependencyConfiguration.Api
                )
            )
        ).create()

        // Verify all modules created
        assertThat(File(projectDir, "user/profile/api/build.gradle.kts").exists()).isTrue()
        assertThat(File(projectDir, "user/profile/ui/build.gradle.kts").exists()).isTrue()
        assertThat(File(projectDir, "user/profile/impl/build.gradle.kts").exists()).isTrue()
        assertThat(File(projectDir, "user/profile/screen/build.gradle.kts").exists()).isTrue()

        // Verify Screen module has correct dependencies
        val screenBuildContent = File(projectDir, "user/profile/screen/build.gradle.kts").readText()
        assertThat(screenBuildContent).contains("implementation(project(\":user:profile:ui\"))")
        assertThat(screenBuildContent).contains("api(project(\":user:profile:api\"))")

        // Verify all modules in all-projects.txt
        val allProjectsContent = File(projectDir, "gradle/all-projects.txt").readText()
        assertThat(allProjectsContent).contains(":user:profile:api")
        assertThat(allProjectsContent).contains(":user:profile:ui")
        assertThat(allProjectsContent).contains(":user:profile:impl")
        assertThat(allProjectsContent).contains(":user:profile:screen")
    }

    @Test
    fun `creates module with custom plugin selection`() {
        val projectDir = setupTestProject()
        val project = createMockProject(projectDir)

        val template = BandLabModuleTemplate(
            project = project,
            moduleInfo = ModuleInfo("/user/settings/api"),
            config = BandLabModuleConfig.Api(
                isSelected = true,
                selectedPlugins = setOf(
                    ModulePlugin.Metro,
                    ModulePlugin.PreferenceConfig,
                    ModulePlugin.RemoteConfig,
                    ModulePlugin.RestApi
                )
            ),
            featureName = "UserSettings",
            dependsOn = emptyList()
        )

        template.create()

        val buildFile = File(projectDir, "user/settings/api/build.gradle.kts")
        val buildContent = buildFile.readText()
        assertThat(buildContent).contains("alias(bandlab.plugins.metro)")
        assertThat(buildContent).contains("alias(bandlab.plugins.preferenceConfig)")
        assertThat(buildContent).contains("alias(bandlab.plugins.remoteConfig)")
        assertThat(buildContent).contains("alias(bandlab.plugins.restApi)")
    }

    @Test
    fun `exposes module to AppGraph`() {
        val projectDir = setupTestProject()
        val project = createMockProject(projectDir)

        // Create app module build file
        val appDir = File(projectDir, "app")
        appDir.mkdirs()
        val appBuildFile = File(appDir, "build.gradle.kts")
        appBuildFile.writeText("""
            $PLUGINS_START
            $PLUGINS_END

            $DEPENDENCIES_START
            $DEPENDENCIES_END
        """.trimIndent())

        val template = BandLabModuleTemplate(
            project = project,
            moduleInfo = ModuleInfo("/user/profile/impl"),
            config = BandLabModuleConfig.Impl(
                isSelected = true,
                typeSelection = ModuleTypeSelection.RequireSelection(BandLabModuleType.Android),
                exposure = ModuleExposure.AppGraph
            ),
            featureName = "UserProfile",
            dependsOn = emptyList()
        )

        template.create()

        // Verify module exposed to app
        val appBuildContent = appBuildFile.readText()
        assertThat(appBuildContent).contains("implementation(project(\":user:profile:impl\"))")
    }

    @Test
    fun `exposes module to MixEditorGraph`() {
        val projectDir = setupTestProject()
        val project = createMockProject(projectDir)

        // Create mixeditor/legacy module build file
        val mixEditorDir = File(projectDir, "mixeditor/legacy")
        mixEditorDir.mkdirs()
        val mixEditorBuildFile = File(mixEditorDir, "build.gradle.kts")
        mixEditorBuildFile.writeText("""
            $PLUGINS_START
            $PLUGINS_END

            $DEPENDENCIES_START
            $DEPENDENCIES_END
        """.trimIndent())

        val template = BandLabModuleTemplate(
            project = project,
            moduleInfo = ModuleInfo("/audio/mixer/screen"),
            config = BandLabModuleConfig.Screen(
                isSelected = true,
                exposure = ModuleExposure.MixEditorGraph
            ),
            featureName = "AudioMixer",
            dependsOn = emptyList()
        )

        template.create()

        // Verify module exposed to mixeditor/legacy
        val mixEditorBuildContent = mixEditorBuildFile.readText()
        assertThat(mixEditorBuildContent).contains("implementation(project(\":audio:mixer:screen\"))")
    }

    @Test
    fun `creates Screen module with Activity template`() {
        val projectDir = setupTestProject()
        val project = createMockProject(projectDir)

        val template = BandLabModuleTemplate(
            project = project,
            moduleInfo = ModuleInfo("/user/profile/screen"),
            config = BandLabModuleConfig.Screen(
                isSelected = true,
                template = BandLabModuleConfig.Screen.Template.Activity
            ),
            featureName = "UserProfile",
            dependsOn = emptyList()
        )

        template.create()

        val screenSrcPath = "user/profile/screen/src/main/kotlin/com/bandlab/user/profile/screen"

        // Verify Activity file created
        val activityFile = File(projectDir, "$screenSrcPath/UserProfileActivity.kt")
        assertThat(activityFile.exists()).isTrue()
        val activityContent = activityFile.readText()
        assertThat(activityContent).contains("class UserProfileActivity")
        assertThat(activityContent).contains("package com.bandlab.user.profile.screen")

        // Verify Page file created
        val pageFile = File(projectDir, "$screenSrcPath/UserProfilePage.kt")
        assertThat(pageFile.exists()).isTrue()
        val pageContent = pageFile.readText()
        assertThat(pageContent).contains("fun UserProfilePage")

        // Verify ViewModel file created
        val viewModelFile = File(projectDir, "$screenSrcPath/UserProfileViewModel.kt")
        assertThat(viewModelFile.exists()).isTrue()
        val viewModelContent = viewModelFile.readText()
        assertThat(viewModelContent).contains("class UserProfileViewModel")

        // Verify AndroidManifest.xml created
        val manifestFile = File(projectDir, "user/profile/screen/src/main/AndroidManifest.xml")
        assertThat(manifestFile.exists()).isTrue()
        val manifestContent = manifestFile.readText()
        assertThat(manifestContent).contains("UserProfileActivity")
    }

    @Test
    fun `creates Screen module with Page template`() {
        val projectDir = setupTestProject()
        val project = createMockProject(projectDir)

        val template = BandLabModuleTemplate(
            project = project,
            moduleInfo = ModuleInfo("/user/settings/screen"),
            config = BandLabModuleConfig.Screen(
                isSelected = true,
                template = BandLabModuleConfig.Screen.Template.Page
            ),
            featureName = "UserSettings",
            dependsOn = emptyList()
        )

        template.create()

        val screenSrcPath = "user/settings/screen/src/main/kotlin/com/bandlab/user/settings/screen"

        // Verify Page file created with @ContributesComponent
        val pageFile = File(projectDir, "$screenSrcPath/UserSettingsPage.kt")
        assertThat(pageFile.exists()).isTrue()
        val pageContent = pageFile.readText()
        assertThat(pageContent).contains("fun UserSettingsPage")
        assertThat(pageContent).contains("@ContributesComponent")

        // Verify ViewModel file created
        val viewModelFile = File(projectDir, "$screenSrcPath/UserSettingsViewModel.kt")
        assertThat(viewModelFile.exists()).isTrue()
        val viewModelContent = viewModelFile.readText()
        assertThat(viewModelContent).contains("class UserSettingsViewModel")

        // Verify AndroidManifest.xml NOT created for Page template
        val manifestFile = File(projectDir, "user/settings/screen/src/main/AndroidManifest.xml")
        assertThat(manifestFile.exists()).isFalse()
    }

    @Test
    fun `module name validation catches empty module name`() {
        val errors = validateModuleName("")
        assertThat(errors).contains(ModuleValidationError.ModuleNameEmpty)
    }

    @Test
    fun `module name validation catches name not starting with colon`() {
        val errors = validateModuleName("user:profile")
        assertThat(errors).contains(ModuleValidationError.ModuleNameShouldStartWithColon)
    }

    @Test
    fun `module name validation catches name ending with colon`() {
        val errors = validateModuleName(":user:profile:")
        assertThat(errors).contains(ModuleValidationError.ModuleNameEndsWithColon)
    }

    @Test
    fun `module name validation catches invalid characters`() {
        val errors = validateModuleName(":user:Profile")
        assertThat(errors).contains(ModuleValidationError.ModuleNameInvalidChar)
    }

    @Test
    fun `module name validation catches name ending with config suffix`() {
        assertThat(validateModuleName(":user:profile:api")).contains(ModuleValidationError.ModuleNameEndsWithConfig)
        assertThat(validateModuleName(":user:profile:impl")).contains(ModuleValidationError.ModuleNameEndsWithConfig)
        assertThat(validateModuleName(":user:profile:ui")).contains(ModuleValidationError.ModuleNameEndsWithConfig)
        assertThat(validateModuleName(":user:profile:screen")).contains(ModuleValidationError.ModuleNameEndsWithConfig)
    }

    @Test
    fun `module name validation accepts valid module names`() {
        assertThat(validateModuleName(":user:profile")).isEmpty()
        assertThat(validateModuleName(":user:profile-settings")).isEmpty()
        assertThat(validateModuleName(":a")).isEmpty()
        assertThat(validateModuleName(":user-profile")).isEmpty()
    }

    @Test
    fun `detects duplicate module when API module exists`() {
        val projectDir = setupTestProject()
        val project = createMockProject(projectDir)

        // Create an existing API module
        BandLabModuleTemplate(
            project = project,
            moduleInfo = ModuleInfo("/user/profile/api"),
            config = BandLabModuleConfig.Api(isSelected = true),
            featureName = "UserProfile",
            dependsOn = emptyList()
        ).create()

        // Check that existing module is detected
        val allProjectsFile = File(projectDir, "gradle/all-projects.txt")
        val existingModules = allProjectsFile.readText()
            .split("\n")
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .toSet()

        assertThat(existingModules).contains(":user:profile:api")
    }

    @Test
    fun `module dependencies are correctly resolved for Screen module`() {
        val apiModuleInfo = ModuleInfo("/user/profile/api")
        val uiModuleInfo = ModuleInfo("/user/profile/ui")

        val dependencies = listOf(
            Dependency(name = uiModuleInfo.projectAccessorReference),
            Dependency(
                name = apiModuleInfo.projectAccessorReference,
                config = DependencyConfiguration.Api
            )
        )

        // Verify dependency configuration types
        assertThat(dependencies[0].config).isEqualTo(DependencyConfiguration.Implementation)
        assertThat(dependencies[1].config).isEqualTo(DependencyConfiguration.Api)

        // Verify dependency references
        assertThat(dependencies[0].name).isEqualTo("project(\":user:profile:ui\")")
        assertThat(dependencies[1].name).isEqualTo("project(\":user:profile:api\")")
    }

    @Test
    fun `TestFixtures plugin creates testFixtures folder`() {
        val projectDir = setupTestProject()
        val project = createMockProject(projectDir)

        val template = BandLabModuleTemplate(
            project = project,
            moduleInfo = ModuleInfo("/user/profile/api"),
            config = BandLabModuleConfig.Api(
                isSelected = true,
                selectedPlugins = setOf(ModulePlugin.TestFixtures)
            ),
            featureName = "UserProfile",
            dependsOn = emptyList()
        )

        template.create()

        // Verify build file has testFixtures plugin
        val buildFile = File(projectDir, "user/profile/api/build.gradle.kts")
        val buildContent = buildFile.readText()
        assertThat(buildContent).contains("alias(bandlab.plugins.testFixtures)")

        // Verify testFixtures folder created
        val testFixturesDir = File(projectDir, "user/profile/api/src/testFixtures/kotlin/com/bandlab/user/profile/api")
        assertThat(testFixturesDir.exists()).isTrue()
    }

    @Test
    fun `modules are sorted alphabetically in all-projects txt file`() {
        val projectDir = setupTestProject()
        val project = createMockProject(projectDir)

        // Create modules in non-alphabetical order
        BandLabModuleTemplate(
            project = project,
            moduleInfo = ModuleInfo("/user/settings/impl"),
            config = BandLabModuleConfig.Impl(
                isSelected = true,
                typeSelection = ModuleTypeSelection.RequireSelection(BandLabModuleType.Android)
            ),
            featureName = "UserSettings",
            dependsOn = emptyList()
        ).create()

        BandLabModuleTemplate(
            project = project,
            moduleInfo = ModuleInfo("/user/profile/api"),
            config = BandLabModuleConfig.Api(isSelected = true),
            featureName = "UserProfile",
            dependsOn = emptyList()
        ).create()

        BandLabModuleTemplate(
            project = project,
            moduleInfo = ModuleInfo("/user/authentication/screen"),
            config = BandLabModuleConfig.Screen(isSelected = true),
            featureName = "UserAuthentication",
            dependsOn = emptyList()
        ).create()

        // Verify modules are sorted alphabetically
        val allProjectsFile = File(projectDir, "gradle/all-projects.txt")
        val allProjectsContent = allProjectsFile.readText()
        val moduleLines = allProjectsContent
            .split("\n")
            .filter { it.startsWith(":user:") }

        assertThat(moduleLines).isInOrder()
        assertThat(moduleLines).containsExactly(
            ":user:authentication:screen",
            ":user:profile:api",
            ":user:settings:impl"
        ).inOrder()
    }

    @Test
    fun `ModuleInfo correctly calculates paths and references`() {
        val moduleInfo = ModuleInfo("/user/profile/api")

        assertThat(moduleInfo.path).isEqualTo("/user/profile/api")
        assertThat(moduleInfo.reference).isEqualTo(":user:profile:api")
        assertThat(moduleInfo.projectAccessorReference).isEqualTo("project(\":user:profile:api\")")
        assertThat(moduleInfo.packageToImport).isEqualTo("com.bandlab.user.profile.api")
        assertThat(moduleInfo.filesPath).isEqualTo("/user/profile/api/src/main/kotlin/com/bandlab/user/profile/api")
        assertThat(moduleInfo.testFixturesPath).isEqualTo("/user/profile/api/src/testFixtures/kotlin/com/bandlab/user/profile/api")
    }

    @Test
    fun `ModuleInfo handles hyphens in module names correctly`() {
        val moduleInfo = ModuleInfo("/user/profile-settings/impl")

        assertThat(moduleInfo.reference).isEqualTo(":user:profile-settings:impl")
        assertThat(moduleInfo.packageToImport).isEqualTo("com.bandlab.user.profile.settings.impl")
        assertThat(moduleInfo.filesPath).contains("com/bandlab/user/profile/settings/impl")
    }

    // Helper functions

    private fun setupTestProject(): File {
        val projectDir = tmpFolder.newFolder("test-project")

        // Create all-projects.txt file
        val gradleDir = File(projectDir, "gradle")
        gradleDir.mkdirs()
        val allProjectsFile = File(gradleDir, "all-projects.txt")
        allProjectsFile.writeText("""
            # All Modules
            :existing:module

        """.trimIndent())

        return projectDir
    }

    @Suppress("UnstableApiUsage")
    private fun createMockProject(projectDir: File): MockProject {
        val basePath = projectDir.absolutePath
        return object : MockProject(null, Disposer.newDisposable()) {
            override fun getBasePath(): String? = basePath
        }
    }

    private fun validateModuleName(name: String): Set<ModuleValidationError> {
        val moduleNameRegex = "^(:[a-z-]+)+$".toRegex()
        val errors = mutableSetOf<ModuleValidationError>()

        if (name.isBlank() || name == ":") {
            errors.add(ModuleValidationError.ModuleNameEmpty)
            return errors
        }
        if (!name.startsWith(':')) {
            errors.add(ModuleValidationError.ModuleNameShouldStartWithColon)
            return errors
        }
        if (name.endsWith(':')) {
            errors.add(ModuleValidationError.ModuleNameEndsWithColon)
            return errors
        }
        if (!moduleNameRegex.matches(name)) {
            errors.add(ModuleValidationError.ModuleNameInvalidChar)
            return errors
        }
        if (
            name.endsWith(":api") || name.endsWith(":impl") ||
            name.endsWith(":screen") || name.endsWith(":ui")
        ) {
            errors.add(ModuleValidationError.ModuleNameEndsWithConfig)
            return errors
        }

        return errors
    }
}
