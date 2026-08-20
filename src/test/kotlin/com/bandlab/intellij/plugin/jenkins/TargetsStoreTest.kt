// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.jenkins

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TargetsStoreTest {

    @Test
    fun accumulatesTargetsAcrossClasses() {
        val store = JenkinsTargetsStore()
        store.replaceForClasses(setOf("com.A"), listOf("class com.A#t1"))
        store.replaceForClasses(setOf("com.B"), listOf("class com.B"))

        assertThat(store.all()).containsExactly("class com.A#t1", "class com.B").inOrder()
    }

    @Test
    fun reselectingOneClassLeavesOtherClassesIntact() {
        val store = JenkinsTargetsStore()
        store.replaceForClasses(setOf("com.A"), listOf("class com.A#t1"))
        store.replaceForClasses(setOf("com.B"), listOf("class com.B#t1"))

        store.replaceForClasses(setOf("com.A"), listOf("class com.A#t2"))

        assertThat(store.all()).containsExactly("class com.B#t1", "class com.A#t2")
    }

    @Test
    fun emptySelectionRemovesThatClass() {
        val store = JenkinsTargetsStore()
        store.replaceForClasses(setOf("com.A"), listOf("class com.A#t1"))

        store.replaceForClasses(setOf("com.A"), emptyList())

        assertThat(store.isEmpty()).isTrue()
    }

    @Test
    fun matchesClassFromMethodTargets() {
        val store = JenkinsTargetsStore()
        store.replaceForClasses(setOf("com.A"), listOf("class com.A#t1", "class com.A#t2"))

        store.replaceForClasses(setOf("com.A"), listOf("class com.A#t1"))

        assertThat(store.all()).containsExactly("class com.A#t1")
    }

    @Test
    fun clearEmptiesEverything() {
        val store = JenkinsTargetsStore()
        store.replaceForClasses(setOf("com.A"), listOf("class com.A"))

        store.clear()

        assertThat(store.isEmpty()).isTrue()
    }
}
