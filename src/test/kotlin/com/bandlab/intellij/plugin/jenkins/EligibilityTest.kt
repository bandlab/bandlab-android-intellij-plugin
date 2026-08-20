// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.jenkins

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EligibilityTest {

    @Test
    fun acceptsKotlinFileUnderAndroidTest() {
        assertThat(isAndroidTestKtPath("/p/feature/src/androidTest/kotlin/com/bandlab/FooTest.kt"))
            .isTrue()
    }

    @Test
    fun rejectsMainSource() {
        assertThat(isAndroidTestKtPath("/p/feature/src/main/kotlin/com/bandlab/Foo.kt")).isFalse()
    }

    @Test
    fun rejectsNonKotlinFile() {
        assertThat(isAndroidTestKtPath("/p/feature/src/androidTest/AndroidManifest.xml")).isFalse()
    }
}
