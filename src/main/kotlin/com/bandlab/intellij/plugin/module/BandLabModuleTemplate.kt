package com.bandlab.intellij.plugin.module

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.file.PsiDirectoryFactory
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.konan.file.File

class BandLabModuleTemplate(
    private val project: Project,
    private val config: BandLabModuleConfig
) {

    private val psiDocumentManager = PsiDocumentManager.getInstance(project)
    private val psiDirectorFactory = PsiDirectoryFactory.getInstance(project)
    private val psiFileFactory = PsiFileFactory.getInstance(project)
    private val localFileSystem = LocalFileSystem.getInstance()

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
        val modulePath = config.path + config.name
        when (config) {
            is BandLabModuleConfig.Kotlin -> createModule(
                moduleInfo = ModuleInfo(modulePath),
                applyKotlinPlugin = true
            )

            is BandLabModuleConfig.Android -> {
                if (config.composeConvention) {
                    // Create feature:screen module
                    createModule(
                        moduleInfo = ModuleInfo("$modulePath/screen"),
                        applyAndroidPlugin = true,
                        applyComposePlugin = true,
                        applyDaggerPlugin = true,
                        daggerModuleName = config.daggerModuleName,
                        generateActivity = config.generateActivity
                    )

                    // Create feature:ui module
                    createModule(
                        moduleInfo = ModuleInfo("$modulePath/ui"),
                        applyAndroidPlugin = true,
                        applyComposePlugin = true
                    )
                } else {
                    createModule(
                        moduleInfo = ModuleInfo(modulePath),
                        applyAndroidPlugin = true,
                        applyComposePlugin = config.applyComposePlugin,
                        applyDaggerPlugin = config.applyDaggerPlugin,
                        applyDatabasePlugin = config.applyDatabasePlugin,
                        daggerModuleName = config.daggerModuleName
                    )
                }
            }
        }
    }

    private fun createModule(
        moduleInfo: ModuleInfo,
        applyKotlinPlugin: Boolean = false,
        applyAndroidPlugin: Boolean = false,
        applyComposePlugin: Boolean = false,
        applyDaggerPlugin: Boolean = false,
        applyDatabasePlugin: Boolean = false,
        daggerModuleName: String? = null,
        generateActivity: Boolean = false,
    ) {
        // Create the src folder
        File(project.basePath + moduleInfo.filesPath).mkdirs()

        // Create build.gradle.kts
        fun StringBuilder.appendPlugin(pluginId: String) {
            appendLine("    id(\"$pluginId\")")
        }

        psiFileFactory.createFileFromText(
            "build.gradle.kts",
            KotlinFileType.INSTANCE,
            buildString {
                appendLine("plugins {")
                if (applyKotlinPlugin) appendPlugin("com.bandlab.kotlin.library")
                if (applyAndroidPlugin) appendPlugin("com.bandlab.android.library")
                if (applyComposePlugin) appendPlugin("com.bandlab.compose")
                if (applyDaggerPlugin) appendPlugin("com.bandlab.dagger")
                if (applyDatabasePlugin) appendPlugin("com.bandlab.database")
                appendLine("}")
                appendLine()
                appendLine(DEPENDENCIES_START)
                appendLine("    ${if (generateActivity) "implementation(projects.auth.activities)" else ""}")
                appendLine(DEPENDENCIES_END)
            }
        ).addToPath(moduleInfo.path)

        // Modify settings.gradle.kts
        modifySettingsGradleKts(moduleInfo)

        // Create the dagger module and expose the module to app
        if (daggerModuleName != null) {
            generateDaggerModule(
                moduleInfo = moduleInfo,
                name = daggerModuleName,
                addActivityComponent = generateActivity
            )
        }

        // Create the activity template
        if (generateActivity) {
            generateActivityTemplate(
                moduleInfo = moduleInfo,
                name = requireNotNull(daggerModuleName),
            )
        }
    }

    /**
     *  Insert the new module in settings.gradle.kts, and sort the modules alphabetically.
     */
    private fun modifySettingsGradleKts(moduleInfo: ModuleInfo) {
        val settingsGradle = requireVirtualFile("/settings.gradle.kts")
        val settingsGradlePsi = requireNotNull(settingsGradle.toPsiFile(project))

        val document = requireNotNull(psiDocumentManager.getDocument(settingsGradlePsi))
        val currentText = document.text

        val newText = buildString {
            appendLine(currentText)
            appendLine("include(\"${moduleInfo.reference}\")")

            val allModulesIdentifier = "// All Modules"
            val allModulesIndex = indexOf(allModulesIdentifier)
            if (allModulesIndex == -1) {
                throw RuntimeException("Can't find $allModulesIdentifier in settings.gradle.")
            }

            // Sort modules in settings.gradle.kts alphabetically
            val modulesStartIndex = indexOf(NEW_LINE, allModulesIndex) + 1
            val sortedModules = substring(modulesStartIndex)
                .split(NEW_LINE)
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
                .joinToString(NEW_LINE)

            replace(modulesStartIndex, lastIndex, sortedModules)
        }

        document.replaceString(0, document.textLength, newText.trim())

        // Refresh the VirtualFile to reflect the changes
        settingsGradle.refresh(false, false)
    }

    /**
     *  Generate a Dagger module for the feature, and expose the module to the app-level graph.
     */
    private fun generateDaggerModule(
        moduleInfo: ModuleInfo,
        name: String,
        addActivityComponent: Boolean
    ) {
        // Create the Dagger Module
        psiFileFactory.createFileFromText(
            "${name}Module.kt",
            KotlinFileType.INSTANCE,
            buildString {
                appendLine("package ${moduleInfo.packageToImport}")
                appendLine()
                if (addActivityComponent) {
                    appendLine(
                        """
                        import dagger.Module
                        import dagger.android.ContributesAndroidInjector

                        @Module
                        interface ${name}Module {

                            @ContributesAndroidInjector(modules = [${name}ActivityModule::class])
                            fun ${name.replaceFirstChar { it.lowercase() }}Activity(): ${name}Activity
                        }

                        @Module
                        internal object ${name}ActivityModule {

                            
                        }
                    """.trimIndent()
                    )
                } else {
                    appendLine(
                        """
                        import dagger.Module

                        @Module
                        object ${name}Module
                    """.trimIndent()
                    )
                }
            }
        ).addToPath(moduleInfo.filesPath)

        // Expose the new module in the app module
        exposeModuleToApp(moduleInfo)

        // Link the new dagger module to app graph
        linkDaggerModuleToAppGraph(moduleInfo, name)
    }

    private fun exposeModuleToApp(moduleInfo: ModuleInfo) {
        val appGradle = requireVirtualFile("/app/build.gradle.kts")
        val appGradlePsi = requireNotNull(appGradle.toPsiFile(project))

        val document = requireNotNull(psiDocumentManager.getDocument(appGradlePsi))
        val currentText = document.text

        val newText = buildString {
            appendLine(currentText)

            val dependenciesIndex = indexOf(DEPENDENCIES_START)
            if (dependenciesIndex == -1) {
                throw RuntimeException("Can't find $DEPENDENCIES_START in /app/build.gradle.kts.")
            }

            // Sort modules in /app/build.gradle.kts alphabetically
            val modulesToSortStartIndex = indexOf(NEW_LINE, dependenciesIndex) + 1
            val modulesToSortEndIndex = indexOf(DEPENDENCIES_END, dependenciesIndex) - 1
            val sortedModules = substring(modulesToSortStartIndex, modulesToSortEndIndex)
                .split(NEW_LINE)
                .filter { it.isNotBlank() }
                .toMutableList()
                .apply { add("    implementation(projects${moduleInfo.projectAccessorReference})") }
                .distinct()
                .sorted()
                .joinToString(NEW_LINE)

            replace(modulesToSortStartIndex, modulesToSortEndIndex, sortedModules)
        }

        document.replaceString(0, document.textLength, newText.trim())

        // Refresh the VirtualFile to reflect the changes
        appGradle.refresh(false, false)
    }

    private fun linkDaggerModuleToAppGraph(
        moduleInfo: ModuleInfo,
        name: String
    ) {
        val appComponent = requireVirtualFile("/app/src/main/kotlin/com/bandlab/bandlab/AppComponent.kt")
        val appComponentPsi = requireNotNull(appComponent.toPsiFile(project))

        val document = requireNotNull(psiDocumentManager.getDocument(appComponentPsi))
        val currentText = document.text

        val newText = buildString {
            appendLine(currentText)

            // Import the new module and sort the imports
            val importIdentifier = "import "
            val importStartIndex = indexOf(importIdentifier)
            if (importStartIndex == -1) {
                throw RuntimeException("Can't find import area in AppComponent.")
            }

            val importEndIndex = indexOf(NEW_LINE, lastIndexOf(importIdentifier))
            val sortedImports = substring(importStartIndex, importEndIndex)
                .split(NEW_LINE)
                .filter { it.isNotBlank() }
                .toMutableList()
                .apply { add("import ${moduleInfo.packageToImport}.${name}Module") }
                .distinct()
                .sorted()
                .joinToString(NEW_LINE)

            replace(importStartIndex, importEndIndex, sortedImports)

            // Sort modules in AppComponent alphabetically
            val modulesIdentifier = "@Module(includes = ["
            val modulesEndIdentifier = "])"
            val includeModuleIndex = indexOf(modulesIdentifier)
            if (includeModuleIndex == -1) {
                throw RuntimeException("Can't find $modulesIdentifier in AppComponent.")
            }

            val modulesStartIndex = indexOf(NEW_LINE, includeModuleIndex) + 1
            val modulesEndIndex = indexOf(modulesEndIdentifier, includeModuleIndex) - 1
            val sortedModules = substring(modulesStartIndex, modulesEndIndex)
                .split(NEW_LINE)
                .filter { it.isNotBlank() }
                .toMutableList()
                .apply { add("    ${name}Module::class,") }
                .distinct()
                .sorted()
                .joinToString(NEW_LINE) { module ->
                    // Append the trailing comma if it's missing
                    if (module.endsWith(',')) module else "$module,"
                }

            replace(modulesStartIndex, modulesEndIndex, sortedModules)
        }

        document.replaceString(0, document.textLength, newText.trim())

        // Refresh the VirtualFile to reflect the changes
        appComponent.refresh(false, false)
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
            import com.bandlab.auth.activities.CommonActivity2
            import com.bandlab.navigation.android.activityIntent
            import com.bandlab.uikit.compose.activity.setContent
            import javax.inject.Inject
            
            class ${name}Activity : CommonActivity2<Unit>() {
            
                @Inject internal lateinit var viewModel: ${name}ViewModel
            
                override fun parseRequiredParams(bundle: Bundle) = Unit
            
                override fun onCreate(isRestoring: Boolean) {
                    setContent {
                        
                    }
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
            
            internal class ${name}ViewModel @Inject constructor(
                
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
                        android:configChanges="colorMode|density|fontScale|keyboard|keyboardHidden|layoutDirection|locale|mcc|mnc|navigation|orientation|screenLayout|screenSize|smallestScreenSize|touchscreen|uiMode|fontWeightAdjustment" />
                </application>
            </manifest>
            """.trimIndent()
        ).addToPath("${moduleInfo.path}/src/main")
    }

    private fun PsiFile.addToPath(path: String) {
        val moduleVirtualPath = requireVirtualFile(path)
        psiDirectorFactory.createDirectory(moduleVirtualPath).add(this)
    }

    private fun requireVirtualFile(path: String): VirtualFile {
        return requireNotNull(localFileSystem.refreshAndFindFileByPath(project.basePath + path))
    }

    private companion object {
        const val DEPENDENCIES_START = "dependencies {"
        const val DEPENDENCIES_END = "}"
        const val NEW_LINE = "\n"
    }
}

private val snakeRegex = "-[a-zA-Z]".toRegex()

private data class ModuleInfo(
    // Ex: /user/profile/edit-screen
    val path: String,
) {

    // Ex: :user:profile:edit-screen
    val reference: String
        get() = path.replace('/', ':')

    // Ex: user.profile.editScreen
    val projectAccessorReference: String
        get() = snakeRegex.replace(path.replace('/', '.')) {
            it.value.replace("-", "").uppercase()
        }

    // Ex: com/bandlab/user/profile/edit/screen
    val srcPackage: String
        get() = "com/bandlab" + path.replace('-', '/')

    // Ex: com.bandlab.user.profile.edit.screen
    val packageToImport: String
        get() = srcPackage.replace('/', '.')

    // Ex: /user/profile/edit-screen/src/main/kotlin/com/bandlab/user/profile/edit/screen
    val filesPath: String
        get() = "$path/src/main/kotlin/$srcPackage"
}