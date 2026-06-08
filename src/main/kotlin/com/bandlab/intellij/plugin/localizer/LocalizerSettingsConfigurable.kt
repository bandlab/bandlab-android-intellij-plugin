package com.bandlab.intellij.plugin.localizer

import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel

/** Settings > Tools > Localizer — toggles for the plugin's nudges. */
class LocalizerSettingsConfigurable :
    BoundSearchableConfigurable("Localizer", "settings.bandlab.localizer") {

    override fun createPanel(): DialogPanel = panel {
        group("Editing") {
            row {
                checkBox("Warn when editing localizer-managed string files")
                    .bindSelected(LocalizerSettings.getInstance()::warnOnEditingManagedFile)
            }
            row {
                comment("These files are owned by bandlab-localizer — add/update strings through the Localizer actions.")
            }
        }
    }
}
