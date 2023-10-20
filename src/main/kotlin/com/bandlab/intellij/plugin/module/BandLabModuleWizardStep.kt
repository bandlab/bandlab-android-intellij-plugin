package com.bandlab.intellij.plugin.module

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.options.ConfigurationException
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.and
import com.intellij.ui.layout.not
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

    // Module type
    private lateinit var androidModuleButton: JBRadioButton
    private lateinit var kotlinModuleButton: JBRadioButton

    // Compose convention
    private lateinit var composeConventionCheckBox: JBCheckBox

    // Plugins
    private lateinit var composePluginCheckBox: JBCheckBox
    private lateinit var daggerPluginCheckBox: JBCheckBox
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
                    .comment("Eg: \"user\" or \"user/profile\", empty to locate at root")
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
                    composeConventionCheckBox = checkBox("Create :screen and :ui modules").component
                }
            }
                .enabledIf(androidModuleButton.selected)
                .topGap(TopGap.MEDIUM)

            group("Plugins") {
                row {
                    composePluginCheckBox = checkBox("Apply Compose plugin").component
                }

                row {
                    daggerPluginCheckBox = checkBox("Apply Dagger plugin").component
                }

                row {
                    databasePluginCheckBox = checkBox("Apply Database plugin").component
                }
            }
                .enabledIf(composeConventionCheckBox.selected.not().and(androidModuleButton.selected))
                .topGap(TopGap.MEDIUM)
        }
    }

    override fun updateDataModel() {
        val modulePath = buildString {
            append(File.separatorChar)
            if (modulePathInput.text.isNotBlank()) {
                append(modulePathInput.text.replace(':', File.separatorChar))
                if (!endsWith(File.separatorChar)) append(File.separatorChar)
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
            )

            else -> error("Non of the module type is selected.")
        }

        updateConfig(moduleConfig)
    }

    override fun validate(): Boolean {
        if (moduleNameInput.text.trim().isEmpty()) {
            throw ConfigurationException("Module Name cannot be empty")
        }
        return true
    }
}