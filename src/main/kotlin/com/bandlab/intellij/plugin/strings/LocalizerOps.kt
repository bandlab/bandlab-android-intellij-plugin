package com.bandlab.intellij.plugin.strings

import com.bandlab.intellij.plugin.localizer.LocalizerConfigService
import com.bandlab.intellij.plugin.localizer.LocalizerConfigService.Target
import com.bandlab.intellij.plugin.localizer.LocalizerRunner
import com.bandlab.intellij.plugin.localizer.parseKeyList
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

/**
 * Shared localizer operations — the single place that builds CLI invocations and refreshes the
 * manifest's files afterwards. Invoked from the menu actions, the editor panel, and the intentions
 * so they never drift.
 */
internal object LocalizerOps {

    fun update(project: Project) = run(project, "Update Strings", listOf("update-strings"))

    /** Add dialog (target picker + multi-key paste), [preselected] target defaulting to the first. */
    fun add(project: Project, preselected: Target?) {
        val targets = project.service<LocalizerConfigService>().targets()
        if (targets.isEmpty()) return
        val dialog = AddStringsDialog(project, targets, preselected ?: targets.first())
        if (!dialog.showAndGet()) return
        val keys = dialog.keys
        if (keys.isEmpty()) return
        run(
            project, "Add Strings",
            listOf("update-strings", "--add-keys", keys.joinToString(","), "--add-keys-to-file", dialog.selectedTarget.addKeysToFile),
        )
    }

    /** Delete dialog (paste keys to remove from the base + every translation). */
    fun delete(project: Project) {
        val input = Messages.showMultilineInputDialog(
            project,
            "Keys to delete — comma, space, or newline separated.\nRemoved from the base file and every translation.",
            "Delete Localization Keys", null, Messages.getWarningIcon(), null,
        ) ?: return
        val keys = parseKeyList(input)
        if (keys.isEmpty()) return
        run(project, "Delete Strings", listOf("update-strings", "--delete-keys", keys.joinToString(",")))
    }

    fun addKey(project: Project, key: String) =
        run(project, "Add Strings", listOf("update-strings", "--add-keys", key))

    fun deleteKey(project: Project, key: String) =
        run(project, "Delete String", listOf("update-strings", "--delete-keys", key))

    private fun run(project: Project, title: String, args: List<String>) {
        LocalizerRunner.run(project, title, args, project.service<LocalizerConfigService>().managedFilePaths())
    }
}
