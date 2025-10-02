/*
 * Copyright (C) 2025 Slack Technologies, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Copied and modified from
// https://github.com/slackhq/foundry/blob/main/platforms/intellij/skate/src/main/kotlin/foundry/intellij/skate/gradle/GradleProjectCompletionContributor.kt
package com.bandlab.intellij.plugin.dependencies.autocomplete

import com.bandlab.intellij.plugin.utils.GradleProjectUtils
import com.bandlab.intellij.plugin.utils.resolvePath
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext

/** Provides autocomplete functionality for project(...) dependencies in Gradle build files. */
class GradleProjectCompletionContributor : CompletionContributor() {

    init {
        extend(CompletionType.BASIC, createProjectCallPattern(), ProjectPathCompletionProvider())
    }

    private fun createProjectCallPattern(): PsiElementPattern.Capture<PsiElement> {
        return PlatformPatterns.psiElement()
            .inFile(
                PlatformPatterns.psiFile()
                    .withName(
                        PlatformPatterns.string()
                            .with(
                                object : PatternCondition<String>("gradle build file") {
                                    override fun accepts(t: String, context: ProcessingContext?): Boolean {
                                        return t.endsWith(".gradle") || t.endsWith(".gradle.kts")
                                    }
                                }
                            )
                    )
            )
    }

    private class ProjectPathCompletionProvider : CompletionProvider<CompletionParameters>() {
        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet,
        ) {
            // Exclude gradle dir from auto-complete, those projects are not declared in all-projects.txt
            if (parameters.originalFile.resolvePath()?.contains("/gradle/") == true) {
                return
            }

            val project = parameters.position.project
            val projectPathService = project.getService(ProjectPathService::class.java)

            // Simple check: look at file content around the cursor position
            val document = parameters.editor.document
            val offset = parameters.offset

            // Get text around the cursor to see if we're in a project() call
            val fileText = document.text
            val startOffset = maxOf(0, offset - 100)
            val endOffset = minOf(fileText.length, offset + 100)
            val surroundingText = fileText.substring(startOffset, endOffset)

            if (!surroundingText.contains("project(")) {
                return
            }

            // Get all project paths and exclude the current one
            val allPaths = projectPathService.getProjectPaths()
            val currentProjectPath = getCurrentProjectPath(parameters)
            val filteredPaths = allPaths.filter { it != currentProjectPath }

            for (path in filteredPaths) {
                val lookupElement =
                    LookupElementBuilder.create(path)
                        .withIcon(AllIcons.Nodes.Module)
                        .withTypeText("Gradle Project")

                result.addElement(lookupElement)
            }

            // Stop other completion contributors from running for cleaner results
            result.stopHere()
        }

        private fun getCurrentProjectPath(parameters: CompletionParameters): String? {
            val file = parameters.originalFile
            val project = parameters.position.project

            // Find the directory containing this build file
            val buildFileDir = file.virtualFile?.parent ?: return null

            // Use existing utility to get the Gradle project path
            return GradleProjectUtils.getGradleProjectPath(project, buildFileDir)
        }
    }
}