@file:Suppress("DialogTitleCapitalization", "UnstableApiUsage")

package com.bandlab.intellij.plugin.module

import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.android.tools.idea.npw.template.BlankModel
import com.android.tools.idea.observable.core.BoolValueProperty
import com.android.tools.idea.observable.core.ObservableBool
import com.android.tools.idea.wizard.model.SkippableWizardStep
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.DEFAULT_COMMENT_WIDTH
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.actionListener
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.whenStateChangedFromUi
import com.intellij.ui.layout.not
import com.intellij.ui.layout.or
import com.intellij.ui.layout.selected
import java.io.File
import javax.swing.JComponent

class BandLabModuleWizardStep(
    private val project: Project,
    private val projectSyncInvoker: ProjectSyncInvoker
) : SkippableWizardStep<BlankModel>(BlankModel(), "BandLab Convention") {

    // Inputs
    private lateinit var modulePathInput: JBTextField
    private lateinit var moduleNameInput: JBTextField
    private lateinit var daggerModuleNameInput: JBTextField

    // Module type
    private lateinit var androidModuleButton: JBRadioButton
    private lateinit var kotlinModuleButton: JBRadioButton

    // Compose convention
    private lateinit var composeConventionCheckBox: JBCheckBox
    private lateinit var generateActivityCheckBox: JBCheckBox

    // Plugins
    private lateinit var composePluginCheckBox: JBCheckBox
    private lateinit var anvilPluginCheckBox: JBCheckBox
    private lateinit var generateDaggerModuleCheckBox: JBCheckBox
    private lateinit var databasePluginCheckBox: JBCheckBox

    // Dagger module exposure
    private lateinit var appModuleButton: JBRadioButton
    private lateinit var meViewModuleButton: JBRadioButton
    private lateinit var noneModuleButton: JBRadioButton

    private val canCreate = BoolValueProperty(false)

    override fun getComponent(): JComponent = panel {
        indent {
            row {
                label("Create modules with BandLab Android project convention.").bold()
            }
                .topGap(TopGap.MEDIUM)
                .bottomGap(BottomGap.SMALL)

            row {
                modulePathInput = textField()
                    .label("Module Path:", LabelPosition.LEFT)
                    .comment("Eg: \"user\" or \"user/profile\", empty to locate the module at root level")
                    .component
            }

            row {
                moduleNameInput = textField()
                    .label("Module Name:", LabelPosition.LEFT)
                    .comment("Eg: \"profile-edit\" or \"comments\"")
                    .component
            }

            buttonsGroup {
                row {
                    androidModuleButton = radioButton("Android Module")
                        .component
                        .also { it.isSelected = true }

                    kotlinModuleButton = radioButton("Kotlin Module")
                        .actionListener { _, component ->
                            if (component.isSelected) {
                                composeConventionCheckBox.isSelected = false
                                composePluginCheckBox.isSelected = false
                                generateActivityCheckBox.isSelected = false
                            }
                        }
                        .component
                }.topGap(TopGap.SMALL)
            }

            group("Compose Feature Convention") {
                row {
                    text(
                        text = "Building a new UI feature with compose? Follow our project convention to split up composables with your business logic.",
                        maxLineLength = DEFAULT_COMMENT_WIDTH
                    ).bold()
                }

                row {
                    browserLink(
                        "Check out the convention",
                        "https://bandlab.atlassian.net/wiki/spaces/Android/pages/2712862819/Isolate+composables+from+business+logic+and+bindings"
                    )
                }

                row {
                    composeConventionCheckBox = checkBox("Create :screen and :ui modules")
                        .whenStateChangedFromUi { selected ->
                            if (!selected) generateActivityCheckBox.isSelected = false
                        }
                        .component
                }

                row {
                    generateActivityCheckBox = checkBox("Generate Activity template")
                        .comment("Create empty Activity, ViewModel, and Manifest")
                        .component
                }.visibleIf(composeConventionCheckBox.selected)
            }
                .visibleIf(androidModuleButton.selected)
                .topGap(TopGap.MEDIUM)

            group("Plugins") {
                row {
                    composePluginCheckBox = checkBox("Apply Compose plugin").component
                }.enabledIf(androidModuleButton.selected)

                row {
                    anvilPluginCheckBox = checkBox("Apply Anvil plugin")
                        .whenStateChangedFromUi { selected ->
                            if (!selected) generateDaggerModuleCheckBox.isSelected = false
                        }
                        .component
                }

                indent {
                    row {
                        generateDaggerModuleCheckBox = checkBox("Generate Dagger Module").component
                    }.visibleIf(anvilPluginCheckBox.selected)
                }

                row {
                    databasePluginCheckBox = checkBox("Apply Database plugin").component
                }
            }
                .visibleIf(composeConventionCheckBox.selected.not())
                .topGap(TopGap.MEDIUM)

            group("Dagger Module") {
                row {
                    daggerModuleNameInput = textField()
                        .label("Name:", LabelPosition.LEFT)
                        .comment("Eg: \"Album\" or \"UserProfileEdit\", no need to mention \"Module\"")
                        .component
                }

                buttonsGroup {
                    row {
                        label("Contribute the new dagger module to:")
                    }

                    row {
                        appModuleButton = radioButton("AppGraph")
                            .component
                            .also { it.isSelected = true }
                    }

                    row {
                        meViewModuleButton = radioButton("MixEditorViewGraph")
                            .comment("After the MixEditor is initialized.")
                            .component
                    }

                    row {
                        noneModuleButton = radioButton("None").component
                    }
                }
            }
                .visibleIf(composeConventionCheckBox.selected.or(generateDaggerModuleCheckBox.selected))
                .topGap(TopGap.MEDIUM)

            modulePathInput.whenTextChanged {
                configureDaggerModuleName(path = modulePathInput.text, name = moduleNameInput.text)
            }

            moduleNameInput.whenTextChanged {
                configureDaggerModuleName(path = modulePathInput.text, name = moduleNameInput.text)
            }
        }
    }

    private fun configureDaggerModuleName(path: String, name: String) {
        val pathCamelCase = path.split('/', '-')
            .filter { it.isNotBlank() }
            .joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } }

        val nameCamelCase = name.split('-')
            .filter { it.isNotBlank() }
            .joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } }

        daggerModuleNameInput.text = pathCamelCase + nameCamelCase
        canCreate.set(name.isNotBlank())
    }

    override fun canGoForward(): ObservableBool {
        return canCreate
    }

    override fun onWizardFinished() {
        val modulePath = buildString {
            append(File.separatorChar)
            if (modulePathInput.text.isNotBlank()) {
                append(modulePathInput.text.replace(':', '/'))
                if (!endsWith('/')) append('/')
            }
        }
        val moduleName = moduleNameInput.text

        val moduleConfig = BandLabModuleConfig(
            type = when {
                kotlinModuleButton.isSelected -> BandLabModuleType.Kotlin
                androidModuleButton.isSelected -> BandLabModuleType.Android
                else -> error("No module type is selected")
            },
            path = modulePath,
            name = moduleName,
            composeConvention = composeConventionCheckBox.isSelected,
            applyComposePlugin = composePluginCheckBox.isSelected,
            applyAnvilPlugin = anvilPluginCheckBox.isSelected,
            applyDatabasePlugin = databasePluginCheckBox.isSelected,
            daggerConfig = if (generateDaggerModuleCheckBox.isSelected || composeConventionCheckBox.isSelected) {
                DaggerModuleConfig(
                    name = daggerModuleNameInput.text,
                    exposure = when {
                        appModuleButton.isSelected -> DaggerModuleExposure.AppComponent
                        meViewModuleButton.isSelected -> DaggerModuleExposure.MixEditorViewComponent
                        noneModuleButton.isSelected -> DaggerModuleExposure.None
                        else -> DaggerModuleExposure.None
                    }
                )
            } else {
                null
            },
            generateActivity = generateActivityCheckBox.isSelected
        )

        // Create and modify all the required files
        BandLabModuleTemplate(project, moduleConfig).create()

        // Sync the project
        projectSyncInvoker.syncProject(project)
    }
}