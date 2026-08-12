package com.bandlab.intellij.plugin.precommit

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * On project open, silently ensures the pre-commit.com hook is installed. The real work — and the
 * cheap "already installed?" guard — lives in [PreCommitHookInstaller]; this just wires it to the
 * startup lifecycle.
 */
class InstallPreCommitHookActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        PreCommitHookInstaller.installIfMissing(project)
    }
}
