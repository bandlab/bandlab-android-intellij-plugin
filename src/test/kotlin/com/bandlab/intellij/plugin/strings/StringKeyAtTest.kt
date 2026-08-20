// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.strings

import com.google.common.truth.Truth.assertThat
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class StringKeyAtTest : BasePlatformTestCase() {

    fun testKeyOnStringElement() {
        val file =
            myFixture.configureByText(
                "strings.xml",
                """<resources><string name="hello">Hi there</string></resources>""",
            )
        assertThat(stringKeyAt(file, file.text.indexOf("Hi there"))).isEqualTo("hello")
    }

    fun testKeyOnStringNameAttribute() {
        val file =
            myFixture.configureByText(
                "strings.xml",
                """<resources><string name="welcome_title">Welcome</string></resources>""",
            )
        assertThat(stringKeyAt(file, file.text.indexOf("welcome_title"))).isEqualTo("welcome_title")
    }

    fun testKeyOnPluralsItem() {
        val file =
            myFixture.configureByText(
                "strings-plurals.xml",
                """<resources><plurals name="songs"><item quantity="one">1 song</item></plurals></resources>""",
            )
        assertThat(stringKeyAt(file, file.text.indexOf("1 song"))).isEqualTo("songs")
    }

    fun testKeyOnOpeningTagName() {
        val file =
            myFixture.configureByText(
                "strings.xml",
                """<resources><string name="greeting">Hi</string></resources>""",
            )
        // caret on the "string" tag name
        assertThat(stringKeyAt(file, file.text.indexOf("<string") + 2)).isEqualTo("greeting")
    }

    fun testKeyOnClosingTag() {
        val file =
            myFixture.configureByText(
                "strings.xml",
                """<resources><string name="bye">Bye</string></resources>""",
            )
        assertThat(stringKeyAt(file, file.text.indexOf("</string>") + 2)).isEqualTo("bye")
    }

    fun testNullOutsideAnyKeyedElement() {
        val file =
            myFixture.configureByText(
                "strings.xml",
                """<resources>   <string name="a">x</string></resources>""",
            )
        assertThat(stringKeyAt(file, file.text.indexOf("<resources>") + 5)).isNull()
    }
}
