package com.bandlab.intellij.plugin.module

import com.bandlab.intellij.plugin.template.ActivityTemplateBuilder
import com.bandlab.intellij.plugin.utils.Const.BUILD_GRADLE
import com.bandlab.intellij.plugin.utils.Const.DEPENDENCIES_END
import com.bandlab.intellij.plugin.utils.Const.DEPENDENCIES_START
import com.bandlab.intellij.plugin.utils.Const.NEW_LINE
import com.bandlab.intellij.plugin.utils.Const.PLUGINS_END
import com.bandlab.intellij.plugin.utils.Const.PLUGINS_START
import com.bandlab.intellij.plugin.utils.editFile
import com.bandlab.intellij.plugin.utils.requireVirtualFile
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.file.PsiDirectoryFactory
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.konan.file.File

class BandLabModuleTemplate(
    private val project: Project,
    private val config: BandLabModuleConfig
) {

    private val psiDirectorFactory = PsiDirectoryFactory.getInstance(project)
    private val psiFileFactory = PsiFileFactory.getInstance(project)

    fun create() {
        WriteCommandAction.runWriteCommandAction(
            /* project = */ project,
            /* commandName = */ "Create Template",
            /* groupID = */ null,
            /* runnable = */ ::createTemplate
        )
    }

    private fun createTemplate() {
        // Ex: /user/profile/edit-screen
        val modulePath = config.path

        if (config.composeConvention) {
            val uiModuleInfo = ModuleInfo("$modulePath/ui")

            // Create feature:screen module
            createModule(
                moduleInfo = ModuleInfo("$modulePath/screen"),
                type = BandLabModuleType.Android,
                plugins = config.plugins.copy(compose = true, metro = true),
                exposure = config.exposure,
                generateActivity = config.generateActivity,
                dependsOn = buildList {
                    add("projects.common.android.screen")
                    // Depends on the ui module where the composables located
                    add(uiModuleInfo.projectAccessorReference)
                }
            )

            // Create feature:ui module
            createModule(
                moduleInfo = uiModuleInfo,
                type = BandLabModuleType.Android,
                plugins = ModulePlugins(compose = true),
                // Do not expose ui module to top-level graph
                exposure = ModuleExposure.None
            )
        } else {
            createModule(
                moduleInfo = ModuleInfo(modulePath),
                type = config.type,
                plugins = config.plugins,
                exposure = config.exposure
            )
        }
    }

    private fun createModule(
        moduleInfo: ModuleInfo,
        type: BandLabModuleType,
        plugins: ModulePlugins,
        exposure: ModuleExposure,
        generateActivity: Boolean = false,
        dependsOn: List<String>? = null
    ) {
        // Create the src folder
        File(project.basePath + moduleInfo.filesPath).mkdirs()

        // Create build.gradle.kts
        fun StringBuilder.appendPlugin(pluginId: String) {
            appendLine("    alias(bandlab.plugins.$pluginId)")
        }

        psiFileFactory.createFileFromText(
            BUILD_GRADLE,
            KotlinFileType.INSTANCE,
            buildString {
                appendLine(PLUGINS_START)
                when (type) {
                    BandLabModuleType.Kotlin -> appendPlugin("library.kotlin")
                    BandLabModuleType.Android -> appendPlugin("library.android")
                }
                if (plugins.compose) appendPlugin("compose")
                if (plugins.database) appendPlugin("database")
                if (plugins.metro) appendPlugin("metro")
                if (plugins.preferenceConfig) appendPlugin("preferenceConfig")
                if (plugins.remoteConfig) appendPlugin("remoteConfig")
                if (plugins.restApi) appendPlugin("restApi")
                if (plugins.testFixtures) {
                    appendPlugin("testFixtures")
                    // Create an empty folder for testFixtures
                    File(project.basePath + moduleInfo.testFixturesPath).mkdirs()
                }

                appendLine(PLUGINS_END)
                appendLine()
                appendLine(DEPENDENCIES_START)
                if (dependsOn != null) {
                    dependsOn.forEach { dependency ->
                        appendLine("    implementation($dependency)")
                    }
                } else {
                    // Append indent
                    appendLine("    ")
                }
                appendLine(DEPENDENCIES_END)
            }
        ).addToPath(moduleInfo.path)

        // Modify settings.gradle.kts
        modifySettingsGradleKts(moduleInfo)

        // Expose the module to top-level module
        when (exposure) {
            ModuleExposure.AppGraph -> {
                exposeModule(moduleInfo, destinationModule = "/app")
            }

            ModuleExposure.MixEditorGraph -> {
                exposeModule(moduleInfo, destinationModule = "/mixeditor/legacy")
            }

            ModuleExposure.None -> Unit
        }

        // Create the activity template
        if (generateActivity) {
            generateActivityTemplate(
                moduleInfo = moduleInfo,
                name = config.featureName,
            )
        }
    }

    /**
     *  Insert the new module in settings.gradle.kts, and sort the modules alphabetically.
     */
    private fun modifySettingsGradleKts(moduleInfo: ModuleInfo) {
        project.editFile(filePath = "/settings.gradle.kts", isAbsolute = false) {
            val modulesSectionTag = when {
                moduleInfo.reference.startsWith(":audiostretch:") -> "// AudioStretch standalone app"
                moduleInfo.reference.startsWith(":edu:") -> "// EDU app"
                else -> "// All Modules"
            }
            val modulesSectionTagIndex = indexOf(modulesSectionTag)
            if (modulesSectionTagIndex == -1) {
                throw RuntimeException("Can't find $modulesSectionTag in settings.gradle.")
            }

            val modulesStartIndex = indexOf(NEW_LINE, modulesSectionTagIndex + modulesSectionTag.length) + 1
            // Insert the new module
            insert(modulesStartIndex, "include(\"${moduleInfo.reference}\")\n")
            // Try to find the end of the module declaration, return null if it's the end of the file
            val modulesEndIndex = indexOf(NEW_LINE + NEW_LINE, modulesStartIndex)
                .takeUnless { it == -1 } ?: length
            // Sort all modules alphabetically
            val sortedModules = substring(startIndex = modulesStartIndex, endIndex = modulesEndIndex)
                .split(NEW_LINE)
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
                .joinToString(NEW_LINE)

            replace(modulesStartIndex, modulesEndIndex, sortedModules)
        }
    }

    private fun exposeModule(
        moduleInfo: ModuleInfo,
        destinationModule: String,
    ) {
        project.editFile(filePath = "$destinationModule/$BUILD_GRADLE", isAbsolute = false) {
            val dependenciesIndex = indexOf(DEPENDENCIES_START)
            if (dependenciesIndex == -1) {
                throw RuntimeException("Can't find $DEPENDENCIES_START in $destinationModule/$BUILD_GRADLE.")
            }

            // Sort modules in /app/build.gradle.kts alphabetically
            val modulesToSortStartIndex = indexOf(NEW_LINE, dependenciesIndex) + 1
            val modulesToSortEndIndex = indexOf(DEPENDENCIES_END, modulesToSortStartIndex) - 1
            val sortedModules = substring(modulesToSortStartIndex, modulesToSortEndIndex)
                .split(NEW_LINE)
                .filter { it.isNotBlank() }
                .toMutableList()
                .apply { add("    implementation(${moduleInfo.projectAccessorReference})") }
                .distinct()
                .sorted()
                .joinToString(NEW_LINE)

            replace(modulesToSortStartIndex, modulesToSortEndIndex, sortedModules)
        }
    }

    /**
     *  Generate an Activity that extends CommonActivity, as well as the ViewModel and Manifest.
     */
    private fun generateActivityTemplate(
        moduleInfo: ModuleInfo,
        name: String,
    ) {
        val activityTemplateBuilder = ActivityTemplateBuilder(
            name = name,
            filePackage = moduleInfo.packageToImport
        )

        // Create the Activity template
        psiFileFactory.createFileFromText(
            "${name}Activity.kt",
            KotlinFileType.INSTANCE,
            activityTemplateBuilder.createActivity()
        ).addToPath(moduleInfo.filesPath)

        // Create the ViewModel template
        psiFileFactory.createFileFromText(
            "${name}ViewModel.kt",
            KotlinFileType.INSTANCE,
            activityTemplateBuilder.createViewModel()
        ).addToPath(moduleInfo.filesPath)

        // Create the manifest file
        psiFileFactory.createFileFromText(
            "AndroidManifest.xml",
            XmlFileType.INSTANCE,
            activityTemplateBuilder.createManifest()
        ).addToPath("${moduleInfo.path}/src/main")
    }

    private fun PsiFile.addToPath(path: String) {
        val moduleVirtualPath = project.requireVirtualFile(path, isAbsolute = false)
        psiDirectorFactory.createDirectory(moduleVirtualPath).add(this)
    }
}