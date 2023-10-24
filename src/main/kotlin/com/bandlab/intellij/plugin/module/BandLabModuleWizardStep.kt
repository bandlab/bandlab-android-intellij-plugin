@file:Suppress("DialogTitleCapitalization", "UnstableApiUsage")

package com.bandlab.intellij.plugin.module

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.openapi.options.ConfigurationException
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.and
import com.intellij.ui.layout.not
import com.intellij.ui.layout.or
import com.intellij.ui.layout.selected
import java.io.File
import javax.swing.JComponent

class BandLabModuleWizardStep(
    private val context: WizardContext,
    private val updateConfig: (BandLabModuleConfig) -> Unit
) : ModuleWizardStep() {

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
    private lateinit var daggerPluginCheckBox: JBCheckBox
    private lateinit var generateDaggerModuleCheckBox: JBCheckBox
    private lateinit var databasePluginCheckBox: JBCheckBox

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

                    kotlinModuleButton = radioButton("Kotlin Module").component
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
                }

                row {
                    daggerPluginCheckBox = checkBox("Apply Dagger plugin")
                        .whenStateChangedFromUi { selected ->
                            if (!selected) generateDaggerModuleCheckBox.isSelected = false
                        }
                        .component
                }

                indent {
                    row {
                        generateDaggerModuleCheckBox = checkBox("Generate Dagger Module")
                            .comment("Create a module and expose it to the app-level graph")
                            .component
                    }.visibleIf(daggerPluginCheckBox.selected)
                }

                row {
                    databasePluginCheckBox = checkBox("Apply Database plugin").component
                }
            }
                .visibleIf(composeConventionCheckBox.selected.not().and(androidModuleButton.selected))
                .topGap(TopGap.MEDIUM)

            group("Dagger Module") {
                row {
                    daggerModuleNameInput = textField()
                        .label("Name:", LabelPosition.LEFT)
                        .comment("Eg: \"Album\" or \"UserProfileEdit\", no need to mention \"Module\"")
                        .component
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
    }

    override fun updateDataModel() {
        val modulePath = buildString {
            append(File.separatorChar)
            if (modulePathInput.text.isNotBlank()) {
                append(modulePathInput.text.replace(':', '/'))
                if (!endsWith('/')) append('/')
            }
        }
        val moduleName = moduleNameInput.text

        context.projectName = moduleName
        context.setProjectFileDirectory(modulePath)

        val moduleConfig = when {
            kotlinModuleButton.isSelected -> BandLabModuleConfig.Kotlin(path = modulePath, name = moduleName)
            androidModuleButton.isSelected -> BandLabModuleConfig.Android(
                path = modulePath,
                name = moduleName,
                composeConvention = composeConventionCheckBox.isSelected,
                applyComposePlugin = composePluginCheckBox.isSelected,
                applyDaggerPlugin = daggerPluginCheckBox.isSelected,
                applyDatabasePlugin = databasePluginCheckBox.isSelected,
                daggerModuleName = daggerModuleNameInput.text,
                generateActivity = generateActivityCheckBox.isSelected
            )

            else -> error("Non of the module type is selected.")
        }

        updateConfig(moduleConfig)
    }

    override fun validate(): Boolean {
        if (moduleNameInput.text.trim().isEmpty()) {
            throw ConfigurationException("Module Name cannot be empty")
        }
        if (daggerModuleNameInput.isVisible && daggerModuleNameInput.text.trim().isEmpty()) {
            throw ConfigurationException("Dagger Module Name cannot be empty")
        }
        return true
    }
}