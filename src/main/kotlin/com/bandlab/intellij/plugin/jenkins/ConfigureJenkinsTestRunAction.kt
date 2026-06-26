package com.bandlab.intellij.plugin.jenkins

import com.bandlab.intellij.plugin.BandLabIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import org.jetbrains.kotlin.psi.KtFile

/**
 * "Configure Jenkins Test Run" — lets the user pick tests from the open Kotlin test file (whole
 * classes or individual methods), builds the Jenkins `targets`, and triggers the build.
 *
 * Shown only for a `.kt` file under `src/androidTest/` that actually declares `@Test` methods — both
 * [update] and [actionPerformed] parse the file with [TestFileParser], so the menu item is hidden
 * when there's nothing to run (no info dialog needed).
 */
class ConfigureJenkinsTestRunAction : DumbAwareAction(
    /* text = */ "Configure Jenkins Test Run",
    /* description = */ "Pick tests from this file and trigger a Jenkins test run.",
    /* icon = */ BandLabIcons.logo,
) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val ktFile = e.androidTestKtFile()
        e.presentation.isEnabledAndVisible = ktFile != null && TestFileParser.parse(ktFile).isNotEmpty()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val ktFile = e.androidTestKtFile() ?: return

        val testClasses = TestFileParser.parse(ktFile)
        if (testClasses.isEmpty()) return

        JenkinsTestRunDialog(project, testClasses).show()
    }

    /** The open file as a Kotlin file under `src/androidTest/`, else null. */
    private fun AnActionEvent.androidTestKtFile(): KtFile? {
        val psiFile = getData(CommonDataKeys.PSI_FILE) as? KtFile ?: return null
        val path = psiFile.virtualFile?.path ?: return null
        return psiFile.takeIf { isAndroidTestKtPath(path) }
    }
}

/** A Kotlin file under an `androidTest` source set — where instrumentation tests live. */
internal fun isAndroidTestKtPath(path: String): Boolean =
    path.endsWith(".kt") && "/src/androidTest/" in path
