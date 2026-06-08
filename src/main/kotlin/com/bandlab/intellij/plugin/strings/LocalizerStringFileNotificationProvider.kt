package com.bandlab.intellij.plugin.strings

import com.bandlab.intellij.plugin.BandLabIcons
import com.bandlab.intellij.plugin.localizer.LocalizerConfigService
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import java.util.function.Function
import javax.swing.JComponent

/**
 * Banner at the top of any manifest base or translation string file, with one-click links to the
 * Localizer operations. The same shared [LocalizerOps] the menu actions and intentions use, so the
 * three entry points never drift. Shown whenever the file is manifest-managed — no Gradle sync needed.
 */
class LocalizerStringFileNotificationProvider : EditorNotificationProvider {

    override fun collectNotificationData(
        project: Project,
        file: VirtualFile,
    ): Function<in FileEditor, out JComponent?>? {
        if (!project.service<LocalizerConfigService>().isManagedStringFile(file)) return null
        return Function { fileEditor ->
            EditorNotificationPanel(fileEditor, EditorNotificationPanel.Status.Info).apply {
                icon(BandLabIcons.logo)
                text = "Localizer-managed strings"
                createActionLabel("Update Strings") { LocalizerOps.update(project) }
                createActionLabel("Add String") {
                    LocalizerOps.add(project, project.service<LocalizerConfigService>().targetFor(file))
                }
                createActionLabel("Delete Strings") { LocalizerOps.delete(project) }
            }
        }
    }
}
