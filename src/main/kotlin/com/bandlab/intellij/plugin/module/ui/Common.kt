package com.bandlab.intellij.plugin.module.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.jewel.ui.component.Text

@Composable
internal fun SubTitle(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
internal fun TextFieldHint(hint: String) {
    Text(
        text = hint,
        fontSize = 12.sp,
        fontStyle = FontStyle.Italic,
        modifier = Modifier.padding(top = 4.dp)
    )
}

internal val GroupIndicatorWidth = 16.dp

@Composable
internal fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .drawBehind {
                val strokeWidth = 0.5.dp.toPx()
                val topPaddingPx = 24.dp.toPx()
                val widthPx = GroupIndicatorWidth.toPx()
                drawLine(
                    color = Color.LightGray,
                    start = Offset(x = 0f, y = topPaddingPx),
                    end = Offset(x = widthPx, y = topPaddingPx),
                    strokeWidth = strokeWidth
                )
            }
            .padding(start = 24.dp)
    ) {
        SubTitle(title)
        content()
    }
}