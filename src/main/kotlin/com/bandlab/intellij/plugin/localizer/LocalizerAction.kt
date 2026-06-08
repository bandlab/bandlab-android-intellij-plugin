package com.bandlab.intellij.plugin.localizer

import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import javax.swing.Icon

/**
 * Base for the `Localizer:` actions. They're available two ways:
 * - **Globally** (Find Action, Tools menu) whenever the project's localizer manifest resolves;
 * - **In a file popup** (Project View / editor) only on a manifest base or translation string file.
 *
 * Neither path needs a file context to *run* or a Gradle sync — the actions read the manifest and
 * shell out to the CLI. The popup gate is purely about where the context menu item shows.
 */
abstract class LocalizerAction(text: String, description: String, icon: Icon) :
    DumbAwareAction(text, description, icon) {

    final override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    final override fun update(e: AnActionEvent) {
        val service = e.project?.service<LocalizerConfigService>()
        if (service?.isConfigured() != true) {
            e.presentation.isEnabledAndVisible = false
            return
        }
        e.presentation.isEnabledAndVisible = if (isPopupPlace(e.place)) {
            e.getData(CommonDataKeys.VIRTUAL_FILE)?.let(service::isManagedStringFile) == true
        } else {
            true
        }
    }
}

/** A right-click file popup (Project View or editor), where actions gate on the selected file. */
internal fun isPopupPlace(place: String): Boolean =
    place == ActionPlaces.PROJECT_VIEW_POPUP || place == ActionPlaces.EDITOR_POPUP
