// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.strings

import com.bandlab.intellij.plugin.localizer.LocalizerAction
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * "Localizer: Delete Strings" — removes one or more pasted keys from the project's base and
 * translation files via `update-strings --delete-keys` (targeted, zero network).
 */
class DeleteStringsAction :
    LocalizerAction(
        /* text = */ "Delete Strings",
        /* description = */ "Remove localization keys from this project's string files.",
        /* icon = */ AllIcons.General.Remove,
    ) {
    override fun actionPerformed(e: AnActionEvent) {
        LocalizerOps.delete(e.project ?: return)
    }
}
