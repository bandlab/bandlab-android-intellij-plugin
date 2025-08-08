package com.bandlab.intellij.plugin.module.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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