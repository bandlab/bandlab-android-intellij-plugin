package com.bandlab.intellij.plugin.module.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bandlab.intellij.plugin.module.BandLabModuleVariant
import com.bandlab.intellij.plugin.module.ModuleExposure
import com.bandlab.intellij.plugin.module.ModulePlugin
import org.jetbrains.jewel.ui.component.CheckboxRow
import org.jetbrains.jewel.ui.component.RadioButtonRow

@Composable
internal fun BandLabModuleVariantSelector(
    state: BandLabModuleVariant,
    onVariantClick: (BandLabModuleVariant) -> Unit,
    onPluginClick: (BandLabModuleVariant, ModulePlugin) -> Unit,
    onExposureClick: (BandLabModuleVariant, ModuleExposure) -> Unit,
    screenSettingsSlot: @Composable ((BandLabModuleVariant.Screen) -> Unit)? = null
) {
    Column(
        modifier = Modifier.animateContentSize()
    ) {
        val variantName = when (state) {
            is BandLabModuleVariant.Api -> ":api"
            is BandLabModuleVariant.Impl -> ":impl"
            is BandLabModuleVariant.Ui -> ":ui"
            is BandLabModuleVariant.Screen -> ":screen"
        }

        CheckboxRow(
            text = variantName,
            checked = state.isSelected,
            onCheckedChange = { _ -> onVariantClick(state) },
            textStyle = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        )

        if (state.isSelected) {
            Row(
                modifier = Modifier.padding(start = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(
                    modifier = Modifier
                        .background(Color.White)
                        .width(1.dp)
                        .requiredHeight(IntrinsicSize.Max)
                )

                Column {
                    SubTitle("Plugins")

                    state.availablePlugins.forEach { plugin ->
                        CheckboxRow(
                            text = plugin.name,
                            checked = plugin in state.selectedPlugins,
                            onCheckedChange = { onPluginClick(state, plugin) }
                        )
                    }

                    val selectedExposure = state.exposure
                    if (selectedExposure != null) {
                        SubTitle("Expose your module to :app, :mixeditor?")

                        ModuleExposure.entries.forEach { exposure ->
                            RadioButtonRow(
                                text = exposure.name,
                                selected = exposure == selectedExposure,
                                onClick = { onExposureClick(state, exposure) }
                            )
                        }
                    }

                    if (state is BandLabModuleVariant.Screen) {
                        screenSettingsSlot?.invoke(state)
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}