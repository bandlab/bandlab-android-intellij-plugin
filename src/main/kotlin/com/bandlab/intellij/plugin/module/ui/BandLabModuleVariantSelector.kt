package com.bandlab.intellij.plugin.module.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bandlab.intellij.plugin.module.BandLabModuleVariant
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.Text

@Composable
internal fun BandLabModuleVariantSelector(
    state: BandLabModuleVariant,
    onClick: (BandLabModuleVariant) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier.clickable { onClick(state) }
        ) {
            Checkbox(
                checked = state.isSelected,
                onCheckedChange = { _ -> onClick(state) },
            )

            Spacer(modifier = Modifier.width(4.dp))

            val text = when (state) {
                is BandLabModuleVariant.Api -> ":api"
                is BandLabModuleVariant.Impl -> ":impl"
                is BandLabModuleVariant.Ui -> ":ui"
                is BandLabModuleVariant.Screen -> ":screen"
            }
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        if (state.isSelected) {


            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}