@file:Suppress("DialogTitleCapitalization", "UnstableApiUsage")

package com.bandlab.intellij.plugin.module

import com.android.build.attribution.ui.warningIcon
import com.android.tools.idea.npw.model.ProjectSyncInvoker
import com.android.tools.idea.npw.template.BlankModel
import com.android.tools.idea.observable.core.BoolValueProperty
import com.android.tools.idea.observable.core.ObservableBool
import com.android.tools.idea.wizard.model.SkippableWizardStep
import com.bandlab.intellij.plugin.utils.Const.BUILD_GRADLE
import com.bandlab.intellij.plugin.utils.visibleIf
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.observable.util.whenTextChanged
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.and
import com.intellij.ui.layout.not
import com.intellij.ui.layout.or
import com.intellij.ui.layout.selected
import javax.swing.JComponent

class BandLabModuleWizardStep(
    private val project: Project,
    private val moduleParent: String,
    private val projectSyncInvoker: ProjectSyncInvoker,
) : SkippableWizardStep<BlankModel>(BlankModel(), "BandLab Convention") {

    // Inputs
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
    private lateinit var restApiPluginCheckBox: JBCheckBox
    private lateinit var remoteConfigPluginCheckBox: JBCheckBox
    private lateinit var preferenceConfigPluginCheckBox: JBCheckBox
    private lateinit var databasePluginCheckBox: JBCheckBox
    private lateinit var testFixturesPluginCheckBox: JBCheckBox

    // Dagger module exposure
    private lateinit var appModuleButton: JBRadioButton
    private lateinit var meComponentModuleButton: JBRadioButton
    private lateinit var noneModuleButton: JBRadioButton

    private val isModuleNameInvalid = AtomicBooleanProperty(false)
    private val isModuleAlreadyExists = AtomicBooleanProperty(false)
    private val canCreate = BoolValueProperty(false)

    private val existingModuleNames = project.modules.map { module ->
        ':' + module.name
            .split('.')
            .drop(1) // drop the root folder
            .joinToString(":")
    }

    override fun getComponent(): JComponent = panel {
        indent {
            row {
                label("Create modules with BandLab Android project convention.").bold()
            }
                .topGap(TopGap.MEDIUM)
                .bottomGap(BottomGap.SMALL)

            row {
                moduleNameInput = textField()
                    .label("Module Name:", LabelPosition.LEFT)
                    .comment("Eg: \":user:profile-edit\" or \":comments-api\"")
                    .component

                moduleNameInput.text = moduleParent
            }

            row {
                icon(warningIcon())
                label(text = "Module name is invalid").bold()
            }.visibleIf(isModuleNameInvalid)

            row {
                icon(warningIcon())
                label(text = "Module already exists").bold()
            }.visibleIf(isModuleAlreadyExists)

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
                }
                    .enabledIf(androidModuleButton.selected)
                    .visibleIf(composeConventionCheckBox.selected.not())

                row {
                    anvilPluginCheckBox = checkBox("Apply Anvil plugin")
                        .whenStateChangedFromUi { selected ->
                            if (!selected) generateDaggerModuleCheckBox.isSelected = false
                        }
                        .component
                }.visibleIf(composeConventionCheckBox.selected.not())

                indent {
                    row {
                        generateDaggerModuleCheckBox = checkBox("Generate Dagger Module").component
                    }.visibleIf(composeConventionCheckBox.selected.not().and(anvilPluginCheckBox.selected))
                }

                row {
                    restApiPluginCheckBox = checkBox("Apply Rest API plugin").component
                }

                row {
                    remoteConfigPluginCheckBox = checkBox("Apply Remote Config plugin").component
                }

                row {
                    preferenceConfigPluginCheckBox = checkBox("Apply Preference Config plugin").component
                }

                row {
                    databasePluginCheckBox = checkBox("Apply Database plugin").component
                }

                row {
                    testFixturesPluginCheckBox = checkBox("Apply Test Fixtures plugin").component
                }
            }.topGap(TopGap.MEDIUM)

            group("Feature Configuration") {
                row {
                    daggerModuleNameInput = textField()
                        .label("Name:", LabelPosition.LEFT)
                        .comment("Eg: \"Album\" or \"UserProfileEdit\", no need to mention \"Module\" or \"Activity\"")
                        .component
                }

                buttonsGroup {
                    row {
                        label("Contribute the new module/ activity to:")
                    }

                    row {
                        appModuleButton = radioButton("AppGraph")
                            .component
                            .also { it.isSelected = true }
                    }

                    row {
                        meComponentModuleButton = radioButton("MixEditorComponentGraph").component
                    }

                    row {
                        noneModuleButton = radioButton("None").component
                    }
                }
            }
                .visibleIf(composeConventionCheckBox.selected.or(generateDaggerModuleCheckBox.selected))
                .topGap(TopGap.MEDIUM)

            moduleNameInput.whenTextChanged {
                configureDaggerModuleName()
                validateModuleName()
            }
        }
    }

    private fun configureDaggerModuleName() {
        val nameInCamelCase = moduleNameInput.text
            .split(':', '-')
            .joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } }

        daggerModuleNameInput.text = nameInCamelCase
    }

    private fun validateModuleName() {
        val moduleName = moduleNameInput.text

        val isNameInvalid = !moduleName.startsWith(':')
                || moduleName.contains('/')
                || moduleName.contains("::")

        val isModuleExists = moduleName in existingModuleNames

        isModuleNameInvalid.set(isNameInvalid)
        isModuleAlreadyExists.set(isModuleExists)
        canCreate.set(!isNameInvalid && !isModuleExists)
    }

    override fun canGoForward(): ObservableBool {
        return canCreate
    }

    override fun onWizardFinished() {
        val modulePath = moduleNameInput.text.replace(':', '/')
        val moduleConfig = BandLabModuleConfig(
            type = when {
                kotlinModuleButton.isSelected -> BandLabModuleType.Kotlin
                androidModuleButton.isSelected -> BandLabModuleType.Android
                else -> error("No module type is selected")
            },
            path = modulePath,
            composeConvention = composeConventionCheckBox.isSelected,
            plugins = ModulePlugins(
                compose = composePluginCheckBox.isSelected,
                anvil = anvilPluginCheckBox.isSelected,
                restApi = restApiPluginCheckBox.isSelected,
                remoteConfig = remoteConfigPluginCheckBox.isSelected,
                preferenceConfig = preferenceConfigPluginCheckBox.isSelected,
                database = databasePluginCheckBox.isSelected,
                testFixtures = testFixturesPluginCheckBox.isSelected
            ),
            daggerConfig = if (generateDaggerModuleCheckBox.isSelected || composeConventionCheckBox.isSelected) {
                DaggerModuleConfig(
                    name = daggerModuleNameInput.text,
                    exposure = when {
                        appModuleButton.isSelected -> DaggerModuleExposure.AppComponent
                        meComponentModuleButton.isSelected -> DaggerModuleExposure.MixEditorComponent
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

        NotificationGroupManager.getInstance()
            .getNotificationGroup("BandLab Notification Group")
            .createNotification("Module ${moduleNameInput.text} is created", NotificationType.INFORMATION)
            .addActions(
                setOf(
                    BandLabModuleSyncAction(projectSyncInvoker),
                    BandLabModuleEditFileAction(
                        buttonText = "Edit $BUILD_GRADLE",
                        filePath = if (composeConventionCheckBox.isSelected) {
                            "$modulePath/screen/$BUILD_GRADLE"
                        } else {
                            "$modulePath/$BUILD_GRADLE"
                        }
                    )
                )
            )
            .notify(project)
    }
}