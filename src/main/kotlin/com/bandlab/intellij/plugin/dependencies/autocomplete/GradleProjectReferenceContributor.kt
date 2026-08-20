// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.dependencies.autocomplete

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.*
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/** Contributes references for project paths in project(...) calls, making them clickable. */
// this works during Gradle sync but not after sync failures
class GradleProjectReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        val gradleFilePattern =
            PlatformPatterns.psiFile()
                .withName(
                    PlatformPatterns.string()
                        .with(
                            object : PatternCondition<String>("gradle build file") {
                                override fun accepts(
                                    t: String,
                                    context: ProcessingContext?,
                                ): Boolean {
                                    return t.endsWith(".gradle") || t.endsWith(".gradle.kts")
                                }
                            }
                        )
                )

        // For Kotlin Gradle files - target string template expressions
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(KtStringTemplateExpression::class.java)
                .inFile(gradleFilePattern),
            GradleProjectReferenceProvider(),
        )

        // For Groovy Gradle files - target literal expressions
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PsiLiteralExpression::class.java).inFile(gradleFilePattern),
            GradleProjectReferenceProvider(),
        )

        // Fallback pattern for quoted strings
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement()
                .withText(StandardPatterns.string().matches("\":[^\"]+\""))
                .inFile(gradleFilePattern),
            GradleProjectReferenceProvider(),
        )
    }
}

class GradleProjectReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext,
    ): Array<PsiReference> {
        // Handle different PSI element types
        val elementText =
            when (element) {
                is KtStringTemplateExpression -> {
                    // For Kotlin string templates, get the content
                    element.entries.joinToString("") { it.text }
                }
                is PsiLiteralExpression -> {
                    // For Java/Groovy literals, get the value
                    element.value?.toString() ?: element.text
                }
                else -> element.text
            }

        // Look for string literals that contain project paths
        val projectPath =
            when {
                elementText.startsWith("\":") && elementText.endsWith("\"") -> {
                    elementText.substring(1, elementText.length - 1) // Remove double quotes
                }
                elementText.startsWith("':") && elementText.endsWith("'") -> {
                    elementText.substring(1, elementText.length - 1) // Remove single quotes
                }
                elementText.startsWith(":") &&
                    !elementText.contains("\"") &&
                    !elementText.contains("'") -> {
                    elementText // Raw project path
                }
                else -> null
            }

        if (projectPath != null && projectPath.startsWith(":")) {
            // Verify this is in a project() call context by checking ancestors
            if (isInProjectCall(element)) {
                // Check if it's a valid project path
                val projectPathService = element.project.getService(ProjectPathService::class.java)
                if (projectPathService.isValidProjectPath(projectPath)) {
                    // Calculate range based on element type
                    val range = calculateReferenceTextRange(element.text)
                    return arrayOf(GradleProjectReference(element, range, projectPath))
                }
            }
        }

        return PsiReference.EMPTY_ARRAY
    }

    private fun isInProjectCall(element: PsiElement): Boolean {
        // Walk up the PSI tree to find if we're inside a project() call
        var current: PsiElement? = element
        var depth = 0
        while (current != null && depth < 10) {
            val currentText = current.text
            if (currentText.contains("project(") && currentText.contains(element.text)) {
                return true
            }
            current = current.parent
            depth++
        }
        return false
    }
}

/** Internal function to calculate text range for references. Exposed for testing. */
internal fun calculateReferenceTextRange(elementText: String): TextRange {
    return when {
        elementText.startsWith("\"") || elementText.startsWith("'") -> {
            TextRange.create(1, elementText.length - 1) // Exclude quotes
        }
        else -> {
            TextRange.create(0, elementText.length) // Entire element
        }
    }
}
