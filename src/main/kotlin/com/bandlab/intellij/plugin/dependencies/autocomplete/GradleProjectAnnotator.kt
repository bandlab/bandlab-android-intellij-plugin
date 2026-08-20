// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.dependencies.autocomplete

import com.bandlab.intellij.plugin.utils.hasAllProjectsFile
import com.bandlab.intellij.plugin.utils.resolvePath
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import java.util.regex.Pattern

internal val PROJECT_CALL_PATTERN = Pattern.compile("""project\s*\(\s*["']([^"']+)["']\s*\)""")

/**
 * Annotator that detects invalid project paths in project(...) calls and highlights them as errors.
 */
class GradleProjectAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        // Check if the Project is using spotlight
        if (!element.project.hasAllProjectsFile()) {
            return
        }

        // Only process elements in Gradle build files
        val file = element.containingFile
        if (
            file?.name?.endsWith(".gradle") != true && file?.name?.endsWith(".gradle.kts") != true
        ) {
            return
        }

        // Exclude gradle dir from the annotator, those projects are not declared in
        // all-projects.txt
        if (file.resolvePath()?.contains("/gradle/") == true) {
            return
        }

        val elementText = element.text

        // Only process elements that contain project( AND don't have children containing project(
        // This prevents processing both parent and child elements with the same content
        if (!elementText.contains("project(")) {
            return
        }

        // Skip if any child element also contains "project(" - let the child handle it
        if (element.children.any { it.text.contains("project(") }) {
            return
        }

        val projectPathService = element.project.getService(ProjectPathService::class.java)

        // Find all project(...) calls in the current element
        val matcher = PROJECT_CALL_PATTERN.matcher(elementText)

        while (matcher.find()) {
            val projectPath = matcher.group(1)
            val startOffset = element.textRange.startOffset + matcher.start(1)
            val endOffset = element.textRange.startOffset + matcher.end(1)
            val range = TextRange.create(startOffset, endOffset)

            if (!projectPathService.isValidProjectPath(projectPath)) {
                // Create error annotation for invalid project path
                holder
                    .newAnnotation(HighlightSeverity.ERROR, "Unknown project path: '$projectPath'")
                    .range(range)
                    .withFix(RefreshProjectPathsIntentionAction())
                    .create()
            }
        }
    }
}
