package com.bandlab.intellij.plugin.strings

import com.bandlab.intellij.plugin.localizer.LocalizerAction
import com.bandlab.intellij.plugin.localizer.LocalizerConfigService
import com.bandlab.intellij.plugin.localizer.LocalizerRunner
import com.bandlab.intellij.plugin.localizer.parseKeyList
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.Messages

/**
 * "Localizer: Delete Strings" — removes one or more pasted keys from the project's base and
 * translation files via `update-strings --delete-keys` (targeted, zero network).
 */
class DeleteStringsAction : LocalizerAction(
    /* text = */ "Delete Strings",
    /* description = */ "Remove localization keys from this project's string files.",
    /* icon = */ AllIcons.General.Remove,
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val input = Messages.showMultilineInputDialog(
            /* project = */ project,
            /* message = */ "Keys to delete — comma, space, or newline separated.\nRemoved from the base file and every translation.",
            /* title = */ "Delete Localization Keys",
            /* initialValue = */ null,
            /* icon = */ Messages.getWarningIcon(),
            /* validator = */ null,
        ) ?: return

        val keys = parseKeyList(input)
        if (keys.isEmpty()) return

        LocalizerRunner.run(
            project = project,
            title = "Delete Strings",
            args = listOf("update-strings", "--delete-keys", keys.joinToString(",")),
            refresh = project.service<LocalizerConfigService>().managedFilePaths(),
        )
    }
}
