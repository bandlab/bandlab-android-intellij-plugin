package com.bandlab.intellij.plugin.jenkins

import com.intellij.openapi.components.Service

/**
 * Accumulates the selected Jenkins `targets` across dialog sessions and across files.
 *
 * The test tree only ever shows the currently open file, but a run usually spans tests from several
 * files. So the selection lives here, outside the dialog: open the action on file A, pick some tests,
 * close it; open it on file B, and A's targets are still there — B's just get added.
 *
 * In-memory and project-scoped: it survives reopening the dialog within the IDE session but is not
 * persisted to disk (a stale target list across restarts would be more surprising than helpful).
 * Cleared after a successful Send, or manually via the dialog's "Clear" button.
 */
@Service(Service.Level.PROJECT)
class JenkinsTargetsStore {

    // LinkedHashSet: stable order, no duplicates.
    private val targets = LinkedHashSet<String>()

    fun all(): List<String> = targets.toList()

    fun isEmpty(): Boolean = targets.isEmpty()

    fun clear() = targets.clear()

    /**
     * Replaces the stored targets that belong to [classFqns] with [newTargets], leaving targets from
     * every other class untouched. Called whenever the current file's checkbox selection changes, so
     * re-selecting within one file never disturbs what was picked in other files.
     */
    fun replaceForClasses(classFqns: Set<String>, newTargets: List<String>) {
        targets.removeAll { classFqnOf(it) in classFqns }
        targets.addAll(newTargets)
    }

    /** `class com.foo.Bar#test` / `class com.foo.Bar` → `com.foo.Bar`. */
    private fun classFqnOf(target: String): String =
        target.removePrefix("class ").substringBefore('#').trim()
}
