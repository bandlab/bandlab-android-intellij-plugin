package com.bandlab.intellij.plugin.strings

import com.bandlab.intellij.plugin.localizer.LocalizerAction
import com.bandlab.intellij.plugin.localizer.LocalizerConfigService
import com.bandlab.intellij.plugin.localizer.isPopupPlace
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service

/**
 * "Localizer: Add Strings" — pulls existing Tolgee keys into a target string file via
 * `update-strings --add-keys --target-file`. The target is always explicit: invoked from a
 * string file's context menu it pre-selects that file's `[[file]]` group; globally it defaults to
 * the first base file. Targeted — never a full sync.
 */
class AddStringsAction : LocalizerAction(
    /* text = */ "Add Strings",
    /* description = */ "Pull existing Tolgee keys into a project string file.",
    /* icon = */ AllIcons.General.Add,
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        // When invoked from a string file's popup, pre-select that file's [[file]] group.
        val contextTarget = e.getData(CommonDataKeys.VIRTUAL_FILE)
            ?.takeIf { isPopupPlace(e.place) }
            ?.let { project.service<LocalizerConfigService>().targetFor(it) }
        LocalizerOps.add(project, contextTarget)
    }
}
