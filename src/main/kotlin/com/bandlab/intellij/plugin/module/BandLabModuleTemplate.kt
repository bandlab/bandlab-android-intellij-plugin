package com.bandlab.intellij.plugin.module

import com.bandlab.intellij.plugin.template.ActivityTemplateBuilder
import com.bandlab.intellij.plugin.template.PageTemplateBuilder
import com.bandlab.intellij.plugin.utils.Const.ALL_PROJECTS_PATH
import com.bandlab.intellij.plugin.utils.Const.DEPENDENCIES_END
import com.bandlab.intellij.plugin.utils.Const.DEPENDENCIES_START
import com.bandlab.intellij.plugin.utils.Const.NEW_LINE
import com.bandlab.intellij.plugin.utils.Const.PLUGINS_END
import com.bandlab.intellij.plugin.utils.Const.PLUGINS_START
import com.bandlab.intellij.plugin.utils.buildScriptName
import com.bandlab.intellij.plugin.utils.editFile
import com.bandlab.intellij.plugin.utils.isUsingKts
import com.bandlab.intellij.plugin.utils.requireVirtualFile
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.file.PsiDirectoryFactory
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.konan.file.File

class BandLabModuleTemplate(
    private val project: Project,
    private val moduleInfo: ModuleInfo,
    private val config: BandLabModuleConfig,
    private val featureName: String,
    private val dependsOn: List<Dependency>
) {

    private val psiDirectorFactory = PsiDirectoryFactory.getInstance(project)
    private val psiFileFactory = PsiFileFactory.getInstance(project)
    private val buildScriptName = project.buildScriptName()

    fun create() {
        // Create the src folder
        File(project.basePath + moduleInfo.filesPath).mkdirs()

        // Create build.gradle[.kts]
        fun StringBuilder.appendPlugin(pluginId: String) {
            appendLine("    alias(bandlab.plugins.$pluginId)")
        }

        val plugins = config.selectedPlugins
        psiFileFactory.createFileFromText(
            buildScriptName,
            KotlinFileType.INSTANCE,
            buildString {
                appendLine(PLUGINS_START)
                when (config.typeSelection.type) {
                    BandLabModuleType.Kotlin -> appendPlugin("library.kotlin")
                    BandLabModuleType.Android -> appendPlugin("library.android")
                    null -> error("Module type isn't selected, it should not be possible.")
                }
                if (ModulePlugin.Compose in plugins) appendPlugin("compose")
                if (ModulePlugin.Database in plugins) appendPlugin("database")
                if (ModulePlugin.Metro in plugins) appendPlugin("metro")
                if (ModulePlugin.PreferenceConfig in plugins) appendPlugin("preferenceConfig")
                if (ModulePlugin.RemoteConfig in plugins) appendPlugin("remoteConfig")
                if (ModulePlugin.RestApi in plugins) appendPlugin("restApi")
                if (ModulePlugin.Screen in plugins) appendPlugin("screen")
                if (ModulePlugin.TestFixtures in plugins) {
                    appendPlugin("testFixtures")
                    // Create an empty folder for testFixtures
                    File(project.basePath + moduleInfo.testFixturesPath).mkdirs()
                }

                appendLine(PLUGINS_END)
                appendLine()
                appendLine(DEPENDENCIES_START)
                if (dependsOn.isEmpty()) {
                    // Append indent
                    appendLine("    ")
                } else {
                    dependsOn.forEach { dependency ->
                        when (dependency.config) {
                            DependencyConfiguration.Implementation -> {
                                appendLine("    implementation(${dependency.name})")
                            }

                            DependencyConfiguration.Api -> {
                                appendLine("    api(${dependency.name})")
                            }
                        }
                    }
                }
                appendLine(DEPENDENCIES_END)
            }
        ).addToPath(moduleInfo.path)

        // Modify module declaration list file (all-projects.txt or settings.gradle[.kts])
        modifyModulesListFile(moduleInfo)

        // Expose the module to top-level module
        when (config.exposure) {
            ModuleExposure.AppGraph -> {
                exposeModule(moduleInfo, destinationModule = "/app")
            }

            ModuleExposure.MixEditorGraph -> {
                exposeModule(moduleInfo, destinationModule = "/mixeditor/legacy")
            }

            ModuleExposure.None, null -> Unit
        }

        // Create the screen template
        if (config is BandLabModuleConfig.Screen) {
            when (config.template) {
                BandLabModuleConfig.Screen.Template.Activity -> {
                    generateActivityTemplate(
                        moduleInfo = moduleInfo,
                        name = featureName,
                    )
                }

                BandLabModuleConfig.Screen.Template.Page -> {
                    generatePageTemplate(
                        moduleInfo = moduleInfo,
                        name = featureName,
                    )
                }

                null -> Unit
            }
        }
    }

    /**
     *  Insert the new module in module declaration file, and sort the modules alphabetically.
     *
     *  If project is using spotlight, the declaration is in /gradle/all-projects.txt
     *  otherwise, it will use /settings.gradle[.kts]
     */
    private fun modifyModulesListFile(moduleInfo: ModuleInfo) {
        val spec = ModuleListSpecification.from(project)

        project.editFile(filePath = spec.filePath, isAbsolute = false) {
            editModuleDeclaration(moduleInfo = moduleInfo, spec = spec)
        }
    }

    private fun StringBuilder.editModuleDeclaration(
        moduleInfo: ModuleInfo,
        spec: ModuleListSpecification,
    ) {
        val tagName = when {
            moduleInfo.reference.startsWith(":audiostretch:") -> "AudioStretch standalone app"
            moduleInfo.reference.startsWith(":edu:") -> "EDU app"
            else -> "All Modules"
        }
        val modulesSectionTag = "${spec.sectionIdentifier} $tagName"
        val modulesSectionTagIndex = indexOf(modulesSectionTag)
        if (modulesSectionTagIndex == -1) {
            throw RuntimeException("Can't find $modulesSectionTag in ${spec.filePath}")
        }

        val modulesStartIndex = indexOf(NEW_LINE, modulesSectionTagIndex + modulesSectionTag.length) + 1
        // Insert the new module
        insert(modulesStartIndex, spec.newModuleStatement(moduleInfo))
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

    private fun exposeModule(
        moduleInfo: ModuleInfo,
        destinationModule: String,
    ) {
        project.editFile(filePath = "$destinationModule/$buildScriptName", isAbsolute = false) {
            val dependenciesIndex = indexOf(DEPENDENCIES_START)
            if (dependenciesIndex == -1) {
                throw RuntimeException("Can't find $DEPENDENCIES_START in $destinationModule/$buildScriptName.")
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

        // Create the Page template
        psiFileFactory.createFileFromText(
            "${name}Page.kt",
            KotlinFileType.INSTANCE,
            activityTemplateBuilder.createPage()
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

    private fun generatePageTemplate(
        moduleInfo: ModuleInfo,
        name: String,
    ) {
        val pageTemplateBuilder = PageTemplateBuilder(
            name = name,
            filePackage = moduleInfo.packageToImport
        )

        psiFileFactory.createFileFromText(
            "${name}Page.kt",
            KotlinFileType.INSTANCE,
            pageTemplateBuilder.createPageWithContributesComponent()
        ).addToPath(moduleInfo.filesPath)

        psiFileFactory.createFileFromText(
            "${name}ViewModel.kt",
            KotlinFileType.INSTANCE,
            pageTemplateBuilder.createViewModel()
        ).addToPath(moduleInfo.filesPath)
    }

    private fun PsiFile.addToPath(path: String) {
        val moduleVirtualPath = project.requireVirtualFile(path, isAbsolute = false)
        val directory = psiDirectorFactory.createDirectory(moduleVirtualPath)
        if (directory.findFile(this.name) == null) {
            directory.add(this)
        } else {
            println("File ${this.name} already exists in $path, skipping...")
        }
    }

    private sealed interface ModuleListSpecification {

        val filePath: String
        val sectionIdentifier: String
        val newModuleStatement: (ModuleInfo) -> String

        class SettingsGradle(isUsingKts: Boolean) : ModuleListSpecification {
            override val filePath: String = if (isUsingKts) "/settings.gradle.kts" else "/settings.gradle"
            override val sectionIdentifier: String = "//"
            override val newModuleStatement: (ModuleInfo) -> String = {
                "include(\"${it.reference}\")\n"
            }
        }

        class SpotlightAllProject : ModuleListSpecification {
            override val filePath: String = ALL_PROJECTS_PATH
            override val sectionIdentifier: String = "#"
            override val newModuleStatement: (ModuleInfo) -> String = { "${it.reference}\n" }
        }

        companion object {
            fun from(project: Project): ModuleListSpecification {
                val useSpotlight = project.basePath?.let { File(it, ALL_PROJECTS_PATH) }?.exists == true
                return if (useSpotlight) SpotlightAllProject() else SettingsGradle(project.isUsingKts())
            }
        }
    }
}

enum class DependencyConfiguration {
    Implementation, Api
}

data class Dependency(
    val name: String,
    val config: DependencyConfiguration = DependencyConfiguration.Implementation
)
