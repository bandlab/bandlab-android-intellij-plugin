package com.bandlab.intellij.plugin.strings

import com.bandlab.intellij.plugin.localizer.LocalizerAction
import com.bandlab.intellij.plugin.localizer.LocalizerConfigService
import com.bandlab.intellij.plugin.localizer.LocalizerRunner
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

/**
 * "Localizer: Update Strings" — full sync of every configured string file from Tolgee, via
 * `bandlab-localizer update-strings`. The only Localizer action that re-pulls everything.
 */
class UpdateStringsAction : LocalizerAction(
    /* text = */ "Update Strings",
    /* description = */ "Sync all localized strings from Tolgee.",
    /* icon = */ AllIcons.Actions.Refresh,
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        LocalizerRunner.run(
            project = project,
            title = "Update Strings",
            args = listOf("update-strings"),
            refresh = project.service<LocalizerConfigService>().managedFilePaths(),
        )
    }
}
