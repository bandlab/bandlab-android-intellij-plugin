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

        if (config.composeConvention) {
            val uiModuleInfo = ModuleInfo("$modulePath/ui")

            // Create feature:screen module
            createModule(
                moduleInfo = ModuleInfo("$modulePath/screen"),
                type = BandLabModuleType.Android,
                applyComposePlugin = true,
                applyAnvilPlugin = true,
                daggerConfig = config.daggerConfig,
                generateActivity = config.generateActivity,
                dependsOn = buildList {
                    add("projects.common.android.composeScreen")
                    // Depends on the ui module where the composables located
                    add(uiModuleInfo.projectAccessorReference)
                }
            )

            // Create feature:ui module
            createModule(
                moduleInfo = uiModuleInfo,
                type = BandLabModuleType.Android,
                applyComposePlugin = true
            )
        } else {
            createModule(
                moduleInfo = ModuleInfo(modulePath),
                type = config.type,
                applyComposePlugin = config.applyComposePlugin,
                applyAnvilPlugin = config.applyAnvilPlugin,
                applyDatabasePlugin = config.applyDatabasePlugin,
                daggerConfig = config.daggerConfig
            )
        }
    }

    private fun createModule(
        moduleInfo: ModuleInfo,
        type: BandLabModuleType,
        applyComposePlugin: Boolean = false,
        applyAnvilPlugin: Boolean = false,
        applyDatabasePlugin: Boolean = false,
        daggerConfig: DaggerModuleConfig? = null,
        generateActivity: Boolean = false,
        dependsOn: List<String>? = null
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
                when (type) {
                    BandLabModuleType.Kotlin -> appendPlugin("com.bandlab.kotlin.library")
                    BandLabModuleType.Android -> appendPlugin("com.bandlab.android.library")
                }
                if (applyComposePlugin) appendPlugin("com.bandlab.compose")
                if (applyAnvilPlugin) appendPlugin("com.bandlab.anvil")
                if (applyDatabasePlugin) appendPlugin("com.bandlab.database")
                appendLine("}")
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
            val modulesStartIndex = indexOf(NEW_LINE, allModulesIndex + allModulesIdentifier.length) + 1
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
        daggerConfig: DaggerModuleConfig,
        addActivityComponent: Boolean
    ) {
        val (name, exposure) = daggerConfig

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

        // Create the Dagger Module
        if (!addActivityComponent) {
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
        val destGradle = requireVirtualFile("$destinationModule/build.gradle.kts")
        val destGradlePsi = requireNotNull(destGradle.toPsiFile(project))

        val document = requireNotNull(psiDocumentManager.getDocument(destGradlePsi))
        val currentText = document.text

        val newText = buildString {
            appendLine(currentText)

            val dependenciesIndex = indexOf(DEPENDENCIES_START)
            if (dependenciesIndex == -1) {
                throw RuntimeException("Can't find $DEPENDENCIES_START in $destinationModule/build.gradle.kts.")
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

        document.replaceString(0, document.textLength, newText.trim())

        // Refresh the VirtualFile to reflect the changes
        destGradle.refresh(false, false)
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
            import androidx.activity.compose.setContent
            import com.bandlab.auth.activities.CommonActivity2
            import com.bandlab.common.android.di.ContributesInjector
            import com.bandlab.navigation.android.activityIntent
            import javax.inject.Inject
            
            @ContributesInjector
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
                        android:configChanges="colorMode|density|fontScale|keyboard|keyboardHidden|layoutDirection|locale|mcc|mnc|navigation|orientation|screenLayout|screenSize|smallestScreenSize|touchscreen|fontWeightAdjustment" />
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

    // Ex: projects.user.profile.editScreen
    val projectAccessorReference: String
        get() = "projects" + snakeRegex.replace(path.replace('/', '.')) {
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