package com.bandlab.intellij.plugin.jenkins

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

/**
 * Builds the Jenkins `targets` array from a user's selection in the test tree.
 *
 * Encoding matches the Jenkins job's `targets` parameter (see the job's "targets" field — an array
 * of strings like `["annotation com.bandlab.bandlab.MixEditor"]`):
 *  - a whole class  → `class <fqName>`            (e.g. `class com.bandlab.FooTest`)
 *  - a single test  → `class <fqName>#<method>`   (e.g. `class com.bandlab.FooTest#opensScreen`)
 *
 * When every method of a class is selected we collapse it to the single `class <fqName>` form
 * rather than listing each method — it's shorter and runs the whole class all the same.
 */
object JenkinsTargets {

    /** One selected test class with the subset of its methods the user checked. */
    data class Selection(val testClass: TestClass, val selectedMethods: List<TestMethod>)

    private val json = Json { prettyPrint = true }

    /** Flat list of Jenkins target strings for the given [selections] (empty selections dropped). */
    fun build(selections: List<Selection>): List<String> =
        selections.flatMap { selection ->
            val cls = selection.testClass
            val selected = selection.selectedMethods
            when {
                selected.isEmpty() -> emptyList()
                selected.size == cls.methods.size -> listOf("class ${cls.fqName}")
                else -> selected.map { "class ${cls.fqName}#${it.name}" }
            }
        }

    /** Pretty-printed JSON array of [targets], ready to drop into the Jenkins `targets` field. */
    fun toJson(targets: List<String>): String =
        json.encodeToString(JsonArray.serializer(), JsonArray(targets.map(::JsonPrimitive)))
}
