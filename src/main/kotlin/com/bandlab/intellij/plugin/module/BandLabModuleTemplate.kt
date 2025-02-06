package com.bandlab.intellij.plugin.module

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
                plugins = config.plugins.copy(compose = true, daggerCompiler = true),
                daggerConfig = config.daggerConfig,
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
            )
        } else {
            createModule(
                moduleInfo = ModuleInfo(modulePath),
                type = config.type,
                plugins = config.plugins,
                daggerConfig = config.daggerConfig
            )
        }
    }

    private fun createModule(
        moduleInfo: ModuleInfo,
        type: BandLabModuleType,
        plugins: ModulePlugins,
        daggerConfig: DaggerModuleConfig? = null,
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
                if (plugins.anvil) appendPlugin("anvil")
                if (plugins.compose) appendPlugin("compose")
                if (plugins.daggerCompiler) appendPlugin("dagger.compiler.library")
                if (plugins.database) appendPlugin("database")
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

        // Create the dagger module and expose the module to app
        if (daggerConfig != null) {
            generateDaggerModule(
                moduleInfo = moduleInfo,
                daggerConfig = daggerConfig,
                addActivityComponent = generateActivity
            )
        }

        // Create the activity template
        if (generateActivity) {
            generateActivityTemplate(
                moduleInfo = moduleInfo,
                name = requireNotNull(daggerConfig?.name),
            )
        }
    }

    /**
     *  Insert the new module in settings.gradle.kts, and sort the modules alphabetically.
     */
    private fun modifySettingsGradleKts(moduleInfo: ModuleInfo) {
        project.editFile(filePath = "/settings.gradle.kts", isAbsolute = false) {
            appendLine("include(\"${moduleInfo.reference}\")")

            val allModulesIdentifier = "// All Modules"
            val allModulesIndex = indexOf(allModulesIdentifier)
            if (allModulesIndex == -1) {
                throw RuntimeException("Can't find $allModulesIdentifier in settings.gradle.")
            }

            // Sort modules in settings.gradle.kts alphabetically
            val modulesStartIndex = indexOf(NEW_LINE, allModulesIndex + allModulesIdentifier.length) + 1
            val sortedModules = substring(modulesStartIndex)
                .split(NEW_LINE)
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
                .joinToString(NEW_LINE)

            replace(modulesStartIndex, lastIndex, sortedModules)
        }
    }

    /**
     *  Generate a Dagger module for the feature, and expose the module to the app-level graph.
     */
    private fun generateDaggerModule(
        moduleInfo: ModuleInfo,
        daggerConfig: DaggerModuleConfig,
        addActivityComponent: Boolean
    ) {
        val (name, exposure) = daggerConfig

        // Create the Dagger Module
        if (!addActivityComponent) {
            val scopeImport = when (exposure) {
                DaggerModuleExposure.None -> null
                DaggerModuleExposure.AppComponent -> "import com.bandlab.common.di.AppGraph"
                DaggerModuleExposure.MixEditorViewComponent -> "import com.bandlab.common.di.MixEditorViewGraph"
            }

            val contributionImport = when (exposure) {
                DaggerModuleExposure.None -> null
                DaggerModuleExposure.AppComponent -> "@ContributesTo(AppGraph::class)"
                DaggerModuleExposure.MixEditorViewComponent -> "@ContributesTo(MixEditorViewGraph::class)"
            }

            psiFileFactory.createFileFromText(
                "${name}Module.kt",
                KotlinFileType.INSTANCE,
                buildString {
                    appendLine("package ${moduleInfo.packageToImport}")
                    appendLine()
                    if (scopeImport != null) {
                        appendLine(scopeImport)
                        appendLine("import com.squareup.anvil.annotations.ContributesTo")
                    }
                    appendLine("import dagger.Module")
                    appendLine()
                    appendLine("@Module")
                    contributionImport?.let(::appendLine)
                    appendLine("interface ${name}Module")
                }
            ).addToPath(moduleInfo.filesPath)
        }

        when (exposure) {
            DaggerModuleExposure.None -> Unit

            DaggerModuleExposure.AppComponent -> {
                exposeModule(moduleInfo, destinationModule = "/app")
            }

            DaggerModuleExposure.MixEditorViewComponent -> {
                exposeModule(moduleInfo, destinationModule = "/mixeditor/legacy")
            }
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
     *  Generate an Activity that extends CommonActivity2, as well as the ViewModel and Manifest.
     */
    private fun generateActivityTemplate(
        moduleInfo: ModuleInfo,
        name: String,
    ) {
        // Create the Activity template
        psiFileFactory.createFileFromText(
            "${name}Activity.kt",
            KotlinFileType.INSTANCE,
            """
            package ${moduleInfo.packageToImport}

            import android.content.Context
            import android.content.Intent
            import android.os.Bundle
            import com.bandlab.android.common.activity.CommonActivity2
            import com.bandlab.android.common.activity.CommonActivityDependencies
            import com.bandlab.android.common.activity.componentCreator
            import com.bandlab.common.android.di.ContributesComponent
            import com.bandlab.common.android.di.HasServiceProvider
            import com.bandlab.navigation.android.activityIntent
            import com.bandlab.uikit.compose.activity.WindowInsetsType
            import com.bandlab.uikit.compose.activity.setContent
            import javax.inject.Inject
            
            @ContributesComponent(dependency = ${name}Activity.ServiceProvider::class)
            class ${name}Activity : CommonActivity2<Unit>(), HasServiceProvider {
            
                @Inject override lateinit var dependencies: CommonActivityDependencies
                @Inject lateinit var viewModel: ${name}ViewModel
            
                private val component by componentCreator(Dagger${name}ActivityComponent.factory())
            
                override fun parseRequiredParams(bundle: Bundle) = Unit
            
                override fun onCreate() {
                    setContent(windowInsets = WindowInsetsType.Scrolling) {
                        
                    }
                }
            
                override fun <T> resolve(): T = HasServiceProvider.resolveFrom(component)
            
                interface ServiceProvider {
                    
                }
            
                companion object {
            
                    fun buildIntent(context: Context): Intent {
                        return activityIntent<${name}Activity>(context)
                    }
                }
            }
            """.trimIndent()
        ).addToPath(moduleInfo.filesPath)

        // Create the ViewModel template
        psiFileFactory.createFileFromText(
            "${name}ViewModel.kt",
            KotlinFileType.INSTANCE,
            """
            package ${moduleInfo.packageToImport}

            import javax.inject.Inject
            
            class ${name}ViewModel @Inject constructor(
                
            ) {
                
            }
            """.trimIndent()
        ).addToPath(moduleInfo.filesPath)

        // Create the manifest file
        psiFileFactory.createFileFromText(
            "AndroidManifest.xml",
            XmlFileType.INSTANCE,
            """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
            
                <application>
                    <activity
                        android:name=".${name}Activity"
                        android:configChanges="colorMode|density|fontScale|keyboard|keyboardHidden|layoutDirection|mcc|mnc|navigation|orientation|screenLayout|screenSize|smallestScreenSize|touchscreen|fontWeightAdjustment" />
                </application>
            </manifest>
            """.trimIndent()
        ).addToPath("${moduleInfo.path}/src/main")
    }

    private fun PsiFile.addToPath(path: String) {
        val moduleVirtualPath = project.requireVirtualFile(path, isAbsolute = false)
        psiDirectorFactory.createDirectory(moduleVirtualPath).add(this)
    }
}