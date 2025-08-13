package com.bandlab.intellij.plugin.module.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bandlab.intellij.plugin.module.*
import org.jetbrains.jewel.ui.component.CheckboxRow
import org.jetbrains.jewel.ui.component.RadioButtonRow

@Composable
internal fun BandLabModuleConfigSelector(
    state: BandLabModuleConfig,
    onConfigClick: (BandLabModuleConfig) -> Unit,
    onModuleTypeClick: (BandLabModuleConfig, BandLabModuleType) -> Unit,
    onPluginClick: (BandLabModuleConfig, ModulePlugin) -> Unit,
    onExposureClick: (BandLabModuleConfig, ModuleExposure) -> Unit,
    screenSettingsSlot: @Composable ((BandLabModuleConfig.Screen) -> Unit)?,
    errorMessage: String?,
) {
    Column(
        modifier = Modifier.animateContentSize()
    ) {
        val configName = when (state) {
            is BandLabModuleConfig.Api -> ":api"
            is BandLabModuleConfig.Impl -> ":impl"
            is BandLabModuleConfig.Ui -> ":ui"
            is BandLabModuleConfig.Screen -> ":screen"
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CheckboxRow(
                text = configName,
                checked = state.isSelected,
                enabled = errorMessage == null,
                onCheckedChange = { _ -> onConfigClick(state) },
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            )

            if (errorMessage != null) {
                ErrorText(
                    errorMessage = errorMessage,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        if (state.isSelected) {
            val startPadding = 12.dp
            val bottomPadding = 16.dp
            val groupBorderColor = GroupBorderColor
            Column(
                modifier = Modifier
                    .drawBehind {
                        val strokeWidth = 0.5.dp.toPx()
                        val startPaddingPx = startPadding.toPx()
                        val topPaddingPx = 8.dp.toPx()
                        val bottomPaddingPx = bottomPadding.toPx()
                        drawLine(
                            color = groupBorderColor,
                            start = Offset(x = startPaddingPx, y = topPaddingPx),
                            end = Offset(x = startPaddingPx, y = size.height - bottomPaddingPx),
                            strokeWidth = strokeWidth
                        )
                        drawLine(
                            color = groupBorderColor,
                            start = Offset(x = startPaddingPx, y = size.height - bottomPaddingPx),
                            end = Offset(
                                x = startPaddingPx + GroupIndicatorWidth.toPx(),
                                y = size.height - bottomPaddingPx
                            ),
                            strokeWidth = strokeWidth
                        )
                    }
                    .padding(start = startPadding)
            ) {
                val typeSelection = state.typeSelection
                if (typeSelection is ModuleTypeSelection.RequireSelection) {
                    SettingsGroup("Module Type") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BandLabModuleType.entries.forEach { type ->
                                RadioButtonRow(
                                    text = type.name,
                                    selected = type == typeSelection.type,
                                    onClick = { onModuleTypeClick(state, type) }
                                )
                            }
                        }
                    }
                }

                SettingsGroup("Plugins") {
                    state.availablePlugins.forEach { plugin ->
                        CheckboxRow(
                            text = plugin.name,
                            checked = plugin in state.selectedPlugins,
                            onCheckedChange = { onPluginClick(state, plugin) }
                        )
                    }
                }

                val selectedExposure = state.exposure
                if (selectedExposure != null) {
                    SettingsGroup("Expose your module to?") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ModuleExposure.entries.forEach { exposure ->
                                RadioButtonRow(
                                    text = exposure.name,
                                    selected = exposure == selectedExposure,
                                    onClick = { onExposureClick(state, exposure) }
                                )
                            }
                        }
                    }
                }

                if (state is BandLabModuleConfig.Screen) {
                    screenSettingsSlot?.invoke(state)
                }

                Spacer(Modifier.height(bottomPadding))
            }
        }
    }
}