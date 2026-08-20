// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.localizer

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile

/**
 * Guards localizer-managed string files from accidental hand-edits. On the first keystroke in such
 * a file — unless the current git branch has already been marked as allowed — it **consumes** the
 * keystroke ([Result.STOP]) and, outside the write action, opens a blocking dialog that lets the
 * developer either:
 *
 * - Allow edits on this branch (persisted in the project workspace file via [EditAllowedBranches]).
 *   When the branch can't be determined, a sentinel stands in so the prompt is shown at most once.
 * - Cancel (the dialog reappears on the next keystroke). The Localizer actions stay reachable via
 *   the string context actions (⌥⏎) and the editor toolbar.
 *
 * Reminding on every fresh branch is deliberate — it builds the habit of going through the actions
 * rather than hand-editing. There's intentionally no global opt-out; the per-branch allow covers
 * it.
 */
class LocalizerEditWarningTypedHandler : TypedHandlerDelegate() {

    override fun beforeCharTyped(
        c: Char,
        project: Project,
        editor: Editor,
        file: PsiFile,
        fileType: FileType,
    ): Result {
        val vFile = file.virtualFile ?: return Result.CONTINUE
        if (!project.service<LocalizerConfigService>().isManagedStringFile(vFile))
            return Result.CONTINUE

        // Fall back to a sentinel when the branch can't be determined (no git, detached HEAD, IO
        // error) so the dialog still appears — but only once ever, then the choice is remembered.
        val branch = currentGitBranch(project) ?: NO_BRANCH
        if (EditAllowedBranches.getInstance(project).isAllowed(branch)) return Result.CONTINUE

        // Schedule the dialog outside the current write action (invokeLater posts to the EDT after
        // the write action unwinds). The triggering character is discarded (Result.STOP); the user
        // re-types it after dismissing the dialog.
        ApplicationManager.getApplication()
            .invokeLater(
                { showEditWarningDialog(project, vFile, branch) },
                project.disposed,
            )
        return Result.STOP
    }

    private fun showEditWarningDialog(project: Project, vFile: VirtualFile, branch: String) {
        val options = arrayOf("Edit on this branch", "Cancel")
        val choice =
            Messages.showDialog(
                project,
                "\"${vFile.name}\" is managed by bandlab-localizer.\n\n" +
                    "Hand-edit it directly only when you're on a feature branch whose strings aren't finalized yet " +
                    "(adding temporary/custom strings, or tweaking copy locally).\n\n" +
                    "Otherwise use the Localizer context actions on a string (⌥⏎) or the editor toolbar to keep " +
                    "changes in sync with Tolgee.",
                "Edit Localizer-Managed File",
                options,
                /* defaultOptionIndex = */ -1, // no default button — don't emphasize either choice
                Messages.getWarningIcon(),
            )
        if (choice == 0) EditAllowedBranches.getInstance(project).allow(branch)
        // choice 1 (Cancel) or -1 (Esc/close): do nothing — the dialog reappears on the next
        // keystroke.
    }

    private companion object {
        // Branch names can't contain spaces, so this never collides with a real branch. Used when
        // the branch is unknown (no git / detached HEAD / IO error): the prompt still shows, and
        // "Edit on this branch" remembers the choice so it's asked at most once ever.
        const val NO_BRANCH = "(no git branch)"
    }
}
