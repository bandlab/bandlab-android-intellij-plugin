package com.bandlab.intellij.plugin.strings

import com.bandlab.intellij.plugin.localizer.LocalizerAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent

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
        LocalizerOps.update(e.project ?: return)
    }
}
