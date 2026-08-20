// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.jenkins

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * Extracts the test classes and their `@Test` methods from a Kotlin [KtFile] using PSI.
 *
 * A file may declare several classes, and a class several tests, so the result is hierarchical:
 * [TestClass] → [TestMethod]. Nested classes are included (e.g. a `companion`-less inner test
 * group), since IntelliJ resolves them through the same [KtClassOrObject] traversal.
 *
 * "Test method" detection is annotation-name based: any function annotated with `@Test` (JUnit 4,
 * JUnit 5, or any `*.Test`) qualifies. We match on the annotation's short name so it works without
 * a resolved Gradle/test classpath — consistent with how the rest of this plugin reads PSI.
 */
object TestFileParser {

    private const val TEST_ANNOTATION_SHORT_NAME = "Test"

    /**
     * All test classes (each with at least one `@Test` method) declared in [file], in source order.
     */
    fun parse(file: KtFile): List<TestClass> =
        PsiTreeUtil.findChildrenOfType(file, KtClassOrObject::class.java).mapNotNull {
            it.toTestClassOrNull()
        }

    private fun KtClassOrObject.toTestClassOrNull(): TestClass? {
        val fqName = fqName?.asString() ?: return null
        val methods =
            declarations
                .filterIsInstance<KtNamedFunction>()
                .filter { it.isTestFunction() }
                .mapNotNull { it.name }
                .map(::TestMethod)

        if (methods.isEmpty()) return null

        return TestClass(
            fqName = fqName,
            simpleName = name ?: fqName.substringAfterLast('.'),
            methods = methods,
        )
    }

    private fun KtNamedFunction.isTestFunction(): Boolean = annotationEntries.any {
        it.shortName?.asString() == TEST_ANNOTATION_SHORT_NAME
    }
}
