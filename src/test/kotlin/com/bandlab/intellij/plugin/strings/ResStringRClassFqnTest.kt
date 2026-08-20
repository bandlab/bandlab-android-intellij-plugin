// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.strings

import com.google.common.truth.Truth.assertThat
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

class ResStringRClassFqnTest : BasePlatformTestCase() {

    fun testBareReferenceResolvesToPlainR() {
        val file = myFixture.configureByText("R.kt", "val x = R.string.welcome_title")
        assertThat(rClassFqnAt(file, "welcome_title")).isEqualTo("R")
    }

    fun testBarePluralsResolvesToPlainR() {
        val file = myFixture.configureByText("R.kt", "val x = R.plurals.songs")
        assertThat(rClassFqnAt(file, "songs")).isEqualTo("R")
    }

    fun testPackageQualifiedReferenceStripsTrailingSelector() {
        val file = myFixture.configureByText("R.kt", "val x = com.app.R.string.foo")
        assertThat(rClassFqnAt(file, "foo")).isEqualTo("com.app.R")
    }

    fun testImportAliasedReferenceResolvesToRClassFqn() {
        myFixture.addFileToProject(
            "com/app/R.kt",
            """
            package com.app
            object R { object string { const val foo = 0 } }
            """
                .trimIndent(),
        )
        val file =
            myFixture.configureByText(
                "Usage.kt",
                "import com.app.R as appR\nval x = appR.string.foo",
            )
        assertThat(rClassFqnAt(file, "foo")).isEqualTo("com.app.R")
    }

    fun testTypeAliasedReferenceResolvesToRClassFqn() {
        myFixture.addFileToProject(
            "com/app/R.kt",
            """
            package com.app
            object R { object string { const val foo = 0 } }
            typealias appR = com.app.R
            """
                .trimIndent(),
        )
        val file =
            myFixture.configureByText("Usage.kt", "import com.app.appR\nval x = appR.string.foo")
        assertThat(rClassFqnAt(file, "foo")).isEqualTo("com.app.R")
    }

    fun testMemberAliasResolvesToSamePackageRFromImport() {
        // bandlab convention: `Strings` aliases `R.string` in the strings module; `<pkg>.Strings` →
        // `<pkg>.R`.
        // Resolved from the import text alone — no need for com.bandlab.common.strings to be
        // indexed.
        val file =
            myFixture.configureByText(
                "Usage.kt",
                "import com.bandlab.common.strings.Strings\nval x = Strings.new_tool_label",
            )
        assertThat(rClassFqnAt(file, "new_tool_label")).isEqualTo("com.bandlab.common.strings.R")
    }

    fun testPluralsMemberAliasResolvesToSamePackageR() {
        val file =
            myFixture.configureByText(
                "Usage.kt",
                "import com.bandlab.common.strings.Plurals\nval x = Plurals.songs",
            )
        assertThat(rClassFqnAt(file, "songs")).isEqualTo("com.bandlab.common.strings.R")
    }

    fun testMemberAliasWithoutMatchingImportIsNotDetected() {
        // A local symbol named `Strings` with no strings-module import must not false-positive.
        val file =
            myFixture.configureByText(
                "Usage.kt",
                "object Strings { val foo = 0 }\nval x = Strings.foo",
            )
        assertThat(rClassFqnAt(file, "foo")).isNull()
    }

    fun testMemberAliasFromUnknownPackageIsNotDetected() {
        // `Strings` imported from a package we don't recognize as a strings module: don't offer
        // Add.
        val file =
            myFixture.configureByText(
                "Usage.kt",
                "import com.acme.unrelated.Strings\nval x = Strings.foo",
            )
        assertThat(rClassFqnAt(file, "foo")).isNull()
    }

    fun testNullWhenNotOnResourceReference() {
        val file = myFixture.configureByText("R.kt", "val welcome_title = 1")
        assertThat(rClassFqnAt(file, "welcome_title")).isNull()
    }

    /**
     * FQN at the [name] occurrence, resolving the enclosing name reference like the quick fix does.
     */
    private fun rClassFqnAt(file: PsiFile, name: String): String? {
        val element = file.findElementAt(file.text.indexOf(name)) ?: return null
        val ref =
            PsiTreeUtil.getParentOfType(element, KtNameReferenceExpression::class.java, false)
                ?: return null
        return resStringRClassFqn(ref)
    }
}
