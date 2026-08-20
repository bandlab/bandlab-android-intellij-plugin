// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.intellij.plugin.dependencies.autocomplete

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

/** Intention action to refresh project paths from the all-projects.txt file. */
@Suppress("IntentionDescriptionNotFoundInspection")
class RefreshProjectPathsIntentionAction : IntentionAction, PriorityAction {

    override fun getText(): String = "Refresh project paths"

    override fun getFamilyName(): String = "Gradle project paths"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        return file?.name == "build.gradle" || file?.name == "build.gradle.kts"
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val projectPathService = project.getService(ProjectPathService::class.java)
        projectPathService.invalidateCache()
    }

    override fun startInWriteAction(): Boolean = false

    override fun getPriority(): PriorityAction.Priority = PriorityAction.Priority.HIGH
}
