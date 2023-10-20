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
        val modulePath = config.path + config.name
        when (config) {
            is BandLabModuleConfig.Kotlin -> createModule(
                modulePath = modulePath,
                applyKotlinPlugin = true
            )

            is BandLabModuleConfig.Android -> {
                if (config.composeConvention) {
                    // Create feature:screen module
                    createModule(
                        modulePath = "$modulePath/screen",
                        applyAndroidPlugin = true,
                        applyComposePlugin = true,
                        applyDaggerPlugin = true,
                        createManifest = true,
                    )

                    // Create feature:ui module
                    createModule(
                        modulePath = "$modulePath/ui",
                        applyAndroidPlugin = true,
                        applyComposePlugin = true
                    )
                } else {
                    createModule(
                        modulePath = modulePath,
                        applyAndroidPlugin = true,
                        applyComposePlugin = config.applyComposePlugin,
                        applyDaggerPlugin = config.applyDaggerPlugin,
                        applyDatabasePlugin = config.applyDatabasePlugin,
                    )
                }
            }
        }
    }

    private fun createModule(
        modulePath: String,
        applyKotlinPlugin: Boolean = false,
        applyAndroidPlugin: Boolean = false,
        applyComposePlugin: Boolean = false,
        applyDaggerPlugin: Boolean = false,
        applyDatabasePlugin: Boolean = false,
        createManifest: Boolean = false,
    ) {
        val modulePackage = "com/bandlab" + modulePath.replace('-', '/')

        // Create the src folder
        File(project.basePath + modulePath + "/src/main/kotlin/" + modulePackage).mkdirs()

        // Create build.gradle.kts
        psiFileFactory.createFileFromText(
            /* fileName = */ "build.gradle.kts",
            /* fileType = */ KotlinFileType.INSTANCE,
            /* text = */ buildString {
                appendLine("plugins {")
                if (applyKotlinPlugin) appendPlugin("com.bandlab.kotlin.library")
                if (applyAndroidPlugin) appendPlugin("com.bandlab.android.library")
                if (applyComposePlugin) appendPlugin("com.bandlab.compose")
                if (applyDaggerPlugin) appendPlugin("com.bandlab.dagger")
                if (applyDatabasePlugin) appendPlugin("com.bandlab.database")

                appendLine(
                    """
                    }
                    
                    dependencies {
                        
                    }
                    """.trimIndent()
                )
            }
        ).addToPath(modulePath)

        // Create the manifest file
        if (createManifest) {
            psiFileFactory.createFileFromText(
                /* fileName = */ "AndroidManifest.xml",
                /* fileType = */ XmlFileType.INSTANCE,
                /* text = */ """
                    <?xml version="1.0" encoding="utf-8"?>
                    <manifest xmlns:android="http://schemas.android.com/apk/res/android">

                        <application>
                            
                        </application>
                    </manifest>
                """.trimIndent()
            ).addToPath("$modulePath/src/main")
        }

        // Modify settings.gradle.kts
        val settingsGradle = requireVirtualFile("/settings.gradle.kts")
        val settingsGradlePsi = settingsGradle.toPsiFile(project)!!

        val moduleRef = modulePath.replace('/', ':')
        val document = requireNotNull(psiDocumentManager.getDocument(settingsGradlePsi))
        val currentText = document.text.trimEnd()

        val newText = buildString {
            appendLine(currentText)
            appendLine("include(\"$moduleRef\")")

            val allModuleIdentifierIndex = indexOf(ALL_MODULE_IDENTIFIER)
            if (allModuleIdentifierIndex == -1) {
                throw RuntimeException("Can't find $ALL_MODULE_IDENTIFIER in settings.gradle.")
            }

            // Sort modules in settings.gradle.kts alphabetically
            val modulesToSortIndex = indexOf(NEW_LINE, allModuleIdentifierIndex) + 1
            val sortedModules = substring(modulesToSortIndex)
                .split(NEW_LINE)
                .filter { it.isNotBlank() }
                .sorted()
                .joinToString(NEW_LINE)

            replace(modulesToSortIndex, lastIndex, sortedModules)
        }

        document.replaceString(0, document.textLength, newText)

        // Refresh the VirtualFile to reflect the changes
        settingsGradle.refresh(false, false)
    }

    private fun StringBuilder.appendPlugin(pluginId: String) {
        appendLine("    id(\"$pluginId\")")
    }

    private fun PsiFile.addToPath(path: String) {
        val moduleVirtualPath = requireVirtualFile(path)
        psiDirectorFactory.createDirectory(moduleVirtualPath).add(this)
    }

    private fun requireVirtualFile(path: String): VirtualFile {
        return requireNotNull(localFileSystem.refreshAndFindFileByPath(project.basePath + path))
    }

    private companion object {
        const val ALL_MODULE_IDENTIFIER = "// All Modules"
        const val NEW_LINE = "\n"
    }
}