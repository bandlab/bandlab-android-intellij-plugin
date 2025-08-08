@file:Suppress("DialogTitleCapitalization", "UnstableApiUsage")

package com.bandlab.intellij.plugin.module

import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.android.tools.idea.observable.core.ObservableBool
import com.android.tools.idea.wizard.model.SkippableWizardStep
import com.android.tools.idea.wizard.model.WizardModel
import com.bandlab.intellij.plugin.module.ui.BandLabModuleWizard
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.jetbrains.jewel.bridge.JewelComposePanel
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.enableNewSwingCompositing
import javax.swing.JComponent

class EmptyModel : WizardModel() {
    override fun handleFinished() = Unit
}

class BandLabModuleWizardStep(
    private val project: Project,
    private val moduleParent: String,
    private val projectSyncInvoker: ProjectSyncInvoker,
) : SkippableWizardStep<EmptyModel>(EmptyModel(), "BandLab Convention") {

    private val wizardScope = CoroutineScope(Dispatchers.Main)
    private val viewModel = BandLabModuleWizardViewModel(wizardScope, project, moduleParent)

    override fun getComponent(): JComponent {
        @OptIn(ExperimentalJewelApi::class)
        enableNewSwingCompositing()

        return JewelComposePanel {
            BandLabModuleWizard(viewModel.state)
        }
    }

    override fun canGoForward(): ObservableBool {
        return viewModel.canCreate
    }

    override fun onWizardFinished() {
        TODO()
    }
}