// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
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

    /** Update dialog — All strings (full sync) or Selected strings (`--update-keys`). */
    fun update(project: Project) {
        val dialog = UpdateStringsDialog(project)
        if (!dialog.showAndGet()) return
        if (dialog.allStrings) {
            run(project, "Update Strings", listOf("update-strings"))
        } else {
            val keys = dialog.keys
            if (keys.isEmpty()) return
            run(
                project,
                "Update Strings",
                listOf("update-strings", "--update-keys", keys.joinToString(",")),
            )
        }
    }

    /**
     * Add dialog (target picker + multi-key paste) over the full target list. [preselected] picks
     * the initial target; null forces an explicit choice (no default).
     */
    fun add(project: Project, preselected: Target?) {
        val targets = project.service<LocalizerConfigService>().targets()
        if (targets.isEmpty()) return
        addWithDialog(project, dialogTargets = targets, preselected = preselected, initialKeys = "")
    }

    /**
     * Add strings for an unresolved `R.string.X`/`R.plurals.X` reference. Resolves candidate
     * targets from the reference's R class FQN: exactly one → run immediately; none → dialog over
     * the full list (forced pick); several → dialog over just the candidates (forced pick). [key]
     * pre-fills.
     */
    fun addForReference(project: Project, key: String, rClassFqn: String?) {
        val service = project.service<LocalizerConfigService>()
        if (service.targets().isEmpty()) return
        val candidates = rClassFqn?.let { service.targetsForRClass(it) }.orEmpty()
        when (candidates.size) {
            1 -> addDirect(project, listOf(key), candidates.single())
            0 -> addWithDialog(project, service.targets(), preselected = null, initialKeys = key)
            else -> addWithDialog(project, candidates, preselected = null, initialKeys = key)
        }
    }

    /** Add [keys] to [target] immediately, no dialog. */
    fun addDirect(project: Project, keys: List<String>, target: Target) {
        if (keys.isEmpty()) return
        runAdd(project, keys, target)
    }

    private fun addWithDialog(
        project: Project,
        dialogTargets: List<Target>,
        preselected: Target?,
        initialKeys: String,
    ) {
        val dialog = AddStringsDialog(project, dialogTargets, preselected, initialKeys)
        if (!dialog.showAndGet()) return
        val keys = dialog.keys
        if (keys.isEmpty()) return
        runAdd(project, keys, dialog.selectedTarget)
    }

    private fun runAdd(project: Project, keys: List<String>, target: Target) {
        run(
            project,
            "Add Strings",
            listOf(
                "update-strings",
                "--add-keys",
                keys.joinToString(","),
                "--target-file",
                target.addKeysToFile,
            ),
        )
    }

    /** Delete dialog (paste keys to remove from the base + every translation). */
    fun delete(project: Project) {
        val input =
            Messages.showMultilineInputDialog(
                project,
                "Keys to delete — comma, space, or newline separated.\nRemoved from the base file and every translation.",
                "Delete Localization Keys",
                null,
                Messages.getWarningIcon(),
                null,
            ) ?: return
        val keys = parseKeyList(input)
        if (keys.isEmpty()) return
        run(
            project,
            "Delete Strings",
            listOf("update-strings", "--delete-keys", keys.joinToString(",")),
        )
    }

    fun deleteKey(project: Project, key: String) =
        run(project, "Delete String", listOf("update-strings", "--delete-keys", key))

    fun updateKey(project: Project, key: String) =
        run(project, "Update String", listOf("update-strings", "--update-keys", key))

    private fun run(project: Project, title: String, args: List<String>) {
        LocalizerRunner.run(
            project,
            title,
            args,
            project.service<LocalizerConfigService>().managedFilePaths(),
        )
    }
}
