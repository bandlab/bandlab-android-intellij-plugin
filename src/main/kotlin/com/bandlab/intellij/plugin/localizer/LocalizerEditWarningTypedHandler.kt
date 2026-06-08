package com.bandlab.intellij.plugin.localizer

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import java.util.concurrent.ConcurrentHashMap

/**
 * Nudges devs away from hand-editing localizer-managed string files. On the first keystroke in such
 * a file — once per file per IDE session — it shows a non-blocking warning notification and lets the
 * keystroke through (returns [Result.CONTINUE]). Opt-out via Settings > Tools > Localizer.
 *
 * Fires only on literal typing (not on the plugin's own CLI-driven reloads, refactors, or
 * formatters), so it never false-warns on the localizer's own writes.
 */
class LocalizerEditWarningTypedHandler : TypedHandlerDelegate() {

    override fun beforeCharTyped(
        c: Char,
        project: Project,
        editor: Editor,
        file: PsiFile,
        fileType: FileType,
    ): Result {
        if (!LocalizerSettings.getInstance().warnOnEditingManagedFile) return Result.CONTINUE
        val virtualFile = file.virtualFile ?: return Result.CONTINUE
        if (virtualFile.path in warnedFiles) return Result.CONTINUE
        if (!project.service<LocalizerConfigService>().isManagedStringFile(virtualFile)) return Result.CONTINUE

        warnedFiles.add(virtualFile.path)
        NotificationGroupManager.getInstance()
            .getNotificationGroup("bandlab-localizer")
            .createNotification(
                "Localizer-managed string file",
                "Don't edit \"${virtualFile.name}\" by hand — bandlab-localizer owns it. " +
                    "Use the Localizer actions: Add a string that already exists on Tolgee, or Update one to re-pull it.",
                NotificationType.WARNING,
            )
            .notify(project)
        return Result.CONTINUE
    }

    private companion object {
        /** Files already warned this session — keyed by path so each file nags at most once. */
        val warnedFiles: MutableSet<String> = ConcurrentHashMap.newKeySet()
    }
}
