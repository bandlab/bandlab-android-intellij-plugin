package com.bandlab.intellij.plugin.strings

import com.google.common.truth.Truth.assertThat
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ResStringKeyAtTest : BasePlatformTestCase() {

    fun testKeyOnRStringReference() {
        val file = myFixture.configureByText("R.kt", "val x = R.string.welcome_title")
        assertThat(resStringKeyAt(file, file.text.indexOf("welcome_title"))).isEqualTo("welcome_title")
    }

    fun testKeyOnRPluralsReference() {
        val file = myFixture.configureByText("R.kt", "val x = R.plurals.songs")
        assertThat(resStringKeyAt(file, file.text.indexOf("songs"))).isEqualTo("songs")
    }

    fun testKeyOnPackageQualifiedReference() {
        val file = myFixture.configureByText("R.kt", "val x = com.app.R.string.foo")
        assertThat(resStringKeyAt(file, file.text.indexOf("foo"))).isEqualTo("foo")
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
