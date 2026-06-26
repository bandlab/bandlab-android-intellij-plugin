package com.bandlab.intellij.plugin.jenkins

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TargetsTest {

    @Test
    fun collapsesToClassWhenAllMethodsSelected() {
        val testClass = testClass("com.bandlab.FooTest", "a", "b")
        val targets = JenkinsTargets.build(
            selections = listOf(
                JenkinsTargets.Selection(
                    testClass = testClass,
                    selectedMethods = testClass.methods
                )
            )
        )

        assertThat(targets).containsExactly("class com.bandlab.FooTest")
    }

    @Test
    fun emitsPerMethodWhenSubsetSelected() {
        val testClass = testClass("com.bandlab.FooTest", "a", "b")
        val targets = JenkinsTargets.build(
            selections = listOf(
                JenkinsTargets.Selection(
                    testClass,
                    selectedMethods = listOf(testClass.methods[0])
                )
            )
        )

        assertThat(targets).containsExactly("class com.bandlab.FooTest#a")
    }

    @Test
    fun dropsClassesWithNoSelectedMethods() {
        val testClass = testClass("com.bandlab.FooTest", "a")

        assertThat(
            JenkinsTargets.build(
                selections = listOf(
                    JenkinsTargets.Selection(
                        testClass,
                        selectedMethods = emptyList()
                    )
                )
            )
        ).isEmpty()
    }

    @Test
    fun toJsonProducesAStringArray() {
        val json = JenkinsTargets.toJson(listOf("class com.bandlab.FooTest#a", "class com.bandlab.BarTest"))

        assertThat(json.trim()).startsWith("[")
        assertThat(json.trim()).endsWith("]")
        assertThat(json).contains("\"class com.bandlab.FooTest#a\"")
        assertThat(json).contains("\"class com.bandlab.BarTest\"")
    }

    @Test
    fun toJsonOfEmptyListIsEmptyArray() {
        assertThat(JenkinsTargets.toJson(emptyList()).filterNot { it.isWhitespace() }).isEqualTo("[]")
    }

    private fun testClass(fqName: String, vararg methods: String) =
        TestClass(fqName, fqName.substringAfterLast('.'), methods.map(::TestMethod))
}
