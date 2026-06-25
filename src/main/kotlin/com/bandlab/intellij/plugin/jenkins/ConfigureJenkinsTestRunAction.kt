package com.bandlab.intellij.plugin.jenkins

import com.bandlab.intellij.plugin.BandLabIcons
import com.bandlab.intellij.plugin.utils.psiFileOrNull
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import org.jetbrains.kotlin.psi.KtFile

/**
 * "Configure Jenkins Test Run" — analyzes the currently open Kotlin test file, lets the user pick
 * tests (whole classes or individual methods) in a checkbox tree, builds the Jenkins `targets` JSON,
 * and can trigger the Jenkins build.
 *
 * Available only for `.kt` files under `src/androidTest/` — the source set our instrumentation tests
 * live in (same gating convention as the Automation templates).
 */
class ConfigureJenkinsTestRunAction : DumbAwareAction(
    /* text = */ "Configure Jenkins Test Run",
    /* description = */ "Pick tests from this file and trigger a Jenkins test run.",
    /* icon = */ BandLabIcons.logo,
) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.androidTestKtFile() != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val ktFile = e.androidTestKtFile() ?: return

        val testClasses = TestFileParser.parse(ktFile)
        if (testClasses.isEmpty()) {
            Messages.showInfoMessage(
                project,
                "No @Test methods found in ${ktFile.name}.",
                "Configure Jenkins Test Run",
            )
            return
        }

        JenkinsTestRunDialog(project, testClasses).show()
    }

    /** The open file as a Kotlin file under `src/androidTest/`, or null when the action shouldn't show. */
    private fun AnActionEvent.androidTestKtFile(): KtFile? {
        val file = getData(CommonDataKeys.PSI_FILE) ?: psiFileOrNull()
        val ktFile = file as? KtFile ?: return null
        val path = ktFile.virtualFile?.path ?: return null
        return ktFile.takeIf { "/src/androidTest/" in path }
    }
}
