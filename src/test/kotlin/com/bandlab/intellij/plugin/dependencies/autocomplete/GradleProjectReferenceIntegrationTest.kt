// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.dependencies.autocomplete

import com.bandlab.intellij.plugin.utils.writeTestFile
import com.google.common.truth.Truth.assertThat
import com.intellij.openapi.components.service
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

class GradleProjectReferenceIntegrationTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        writeFile("gradle/all-projects.txt", ":features:profile:api\n:features:profile:ui\n")
        project.service<ProjectPathService>().invalidateCache()
    }

    fun testReferenceResolvesToKotlinBuildFileWhenBothBuildScriptsExist() {
        writeFile("features/profile/api/build.gradle", "plugins {}")
        val kotlinBuildFile = writeFile("features/profile/api/build.gradle.kts", "plugins {}")

        assertThat(referenceTo(":features:profile:api").resolve()?.containingFile?.virtualFile)
            .isEqualTo(kotlinBuildFile)
    }

    fun testReferenceResolvesToGroovyBuildFileWhenKotlinScriptIsAbsent() {
        val groovyBuildFile = writeFile("features/profile/ui/build.gradle", "plugins {}")

        assertThat(referenceTo(":features:profile:ui").resolve()?.containingFile?.virtualFile)
            .isEqualTo(groovyBuildFile)
    }

    fun testReferenceDoesNotResolveUndeclaredProjectEvenWhenBuildFileExists() {
        writeFile("features/undeclared/api/build.gradle.kts", "plugins {}")

        assertThat(referenceTo(":features:undeclared:api").resolve()).isNull()
    }

    fun testProviderCreatesReferenceForDeclaredPathInsideProjectCall() {
        val expression = stringExpression("implementation(project(\":features:profile:api\"))")

        val references =
            GradleProjectReferenceProvider().getReferencesByElement(expression, ProcessingContext())

        assertThat(references).hasLength(1)
        assertThat(references.single().canonicalText).isEqualTo(":features:profile:api")
        assertThat(references.single().rangeInElement)
            .isEqualTo(TextRange(1, expression.textLength - 1))
    }

    fun testProviderIgnoresPathOutsideProjectCall() {
        val expression = stringExpression("val path = \":features:profile:api\"")

        assertThat(
                GradleProjectReferenceProvider()
                    .getReferencesByElement(expression, ProcessingContext()),
            )
            .isEmpty()
    }

    private fun referenceTo(path: String): GradleProjectReference {
        val expression = stringExpression("implementation(project(\"$path\"))")
        return GradleProjectReference(expression, TextRange(1, expression.textLength - 1), path)
    }

    private fun stringExpression(content: String): KtStringTemplateExpression {
        val file = myFixture.configureByText("build.gradle.kts", content)
        var expression: KtStringTemplateExpression? = null
        file.accept(
            object : PsiRecursiveElementWalkingVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (element is KtStringTemplateExpression) expression = element
                    super.visitElement(element)
                }
            },
        )
        return requireNotNull(expression)
    }

    private fun writeFile(relativePath: String, content: String): VirtualFile {
        return project.writeTestFile(relativePath, content)
    }
}
