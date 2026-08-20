// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.strings

import com.google.common.truth.Truth.assertThat
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ResStringKeyAtTest : BasePlatformTestCase() {

    fun testKeyOnRStringReference() {
        val file = myFixture.configureByText("R.kt", "val x = R.string.welcome_title")
        assertThat(resStringKeyAt(file, file.text.indexOf("welcome_title")))
            .isEqualTo("welcome_title")
    }

    fun testKeyOnRPluralsReference() {
        val file = myFixture.configureByText("R.kt", "val x = R.plurals.songs")
        assertThat(resStringKeyAt(file, file.text.indexOf("songs"))).isEqualTo("songs")
    }

    fun testKeyOnPackageQualifiedReference() {
        val file = myFixture.configureByText("R.kt", "val x = com.app.R.string.foo")
        assertThat(resStringKeyAt(file, file.text.indexOf("foo"))).isEqualTo("foo")
    }

    fun testKeyOnImportAliasedReference() {
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
        assertThat(
                resStringKeyAt(file, file.text.indexOf("appR.string.foo") + "appR.string.".length)
            )
            .isEqualTo("foo")
    }

    fun testKeyOnTypeAliasedReference() {
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
        assertThat(
                resStringKeyAt(file, file.text.indexOf("appR.string.foo") + "appR.string.".length)
            )
            .isEqualTo("foo")
    }

    fun testNullOnTypeAliasToNonResourceClass() {
        myFixture.addFileToProject(
            "com/app/Other.kt",
            """
            package com.app
            object Other { object string { const val foo = 0 } }
            typealias appOther = com.app.Other
            """
                .trimIndent(),
        )
        val file =
            myFixture.configureByText(
                "Usage.kt",
                "import com.app.appOther\nval x = appOther.string.foo",
            )
        assertThat(
                resStringKeyAt(
                    file,
                    file.text.indexOf("appOther.string.foo") + "appOther.string.".length,
                )
            )
            .isNull()
    }

    fun testNullOnStringLiteral() {
        val file = myFixture.configureByText("R.kt", """val x = "welcome_title"""")
        assertThat(resStringKeyAt(file, file.text.indexOf("welcome_title"))).isNull()
    }

    fun testNullWhenNotOnResourceReference() {
        val file = myFixture.configureByText("R.kt", "val welcome_title = 1")
        assertThat(resStringKeyAt(file, file.text.indexOf("welcome_title"))).isNull()
    }
}
