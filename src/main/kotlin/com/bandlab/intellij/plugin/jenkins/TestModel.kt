// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.jenkins

/**
 * A single `@Test` method discovered in the open file.
 *
 * [name] is the method's simple name (used both for display and for the `#method` suffix in a
 * Jenkins target).
 */
data class TestMethod(val name: String)

/**
 * A test class discovered in the open file together with its `@Test` methods.
 *
 * [fqName] is the fully-qualified class name (e.g. `com.bandlab.feature.FooTest`) — the value that
 * goes into a Jenkins `class …` target. [simpleName] is just the class name, for display.
 */
data class TestClass(
    val fqName: String,
    val simpleName: String,
    val methods: List<TestMethod>,
)
