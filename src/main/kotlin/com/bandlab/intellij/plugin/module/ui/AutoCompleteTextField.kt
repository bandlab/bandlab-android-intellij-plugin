package com.bandlab.intellij.plugin.module.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import org.jetbrains.jewel.ui.Outline
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField

@Composable
internal fun AutoCompleteTextField(
    state: TextFieldState,
    suggestionsFlow: Flow<Set<String>>,
    outline: Outline,
    modifier: Modifier = Modifier
) {
    var showSuggestions by remember { mutableStateOf(false) }
    var filteredSuggestions by remember { mutableStateOf(emptyList<String>()) }
    val focusRequester = remember { FocusRequester() }
    var textFieldSize by remember { mutableStateOf(IntSize.Zero) }
    val suggestions by suggestionsFlow.collectAsState(initial = emptySet())

    LaunchedEffect(state) {
        snapshotFlow { state.text }
            .drop(1) // drop initial value
            .collect {
                showSuggestions = it.isNotEmpty()
                filteredSuggestions = suggestions.filter { suggestion ->
                    suggestion != it && suggestion.startsWith(it)
                }
            }
    }

    Box(modifier = modifier) {
        TextField(
            state = state,
            outline = outline,
            modifier = Modifier
                .width(300.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    showSuggestions = if (focusState.isFocused) {
                        state.text.isNotEmpty()
                    } else {
                        false
                    }
                }
                .onSizeChanged { textFieldSize = it }
        )

        if (showSuggestions && filteredSuggestions.isNotEmpty()) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, textFieldSize.height), // Adjust offset to be right below TextField
                onDismissRequest = { showSuggestions = false },
            ) {
                LazyColumn(
                    modifier = Modifier
                        .background(
                            color = Color(red = 53, green = 55, blue = 59),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .width(300.dp),
                ) {
                    items(filteredSuggestions) { suggestion ->
                        Text(
                            text = suggestion,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    state.setTextAndPlaceCursorAtEnd(suggestion)
                                    showSuggestions = false
                                    // Request focus back to TextField after selection
                                    focusRequester.requestFocus()
                                }
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}