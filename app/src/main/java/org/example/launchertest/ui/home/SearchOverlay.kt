package org.example.launchertest.ui.home

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp

@Composable
fun SearchOverlay(
    query: String,
    onQueryChanged: (String) -> Unit,
    onSearchSubmitted: (String) -> Unit,
    onKeyboardDismissed: () -> Unit,
    horizontalPaddingDp: Float,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val layoutMetrics = LocalHomeLayoutMetrics.current
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    var hasSeenKeyboardVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    LaunchedEffect(imeBottom) {
        if (imeBottom > 0) {
            hasSeenKeyboardVisible = true
        } else if (hasSeenKeyboardVisible) {
            hasSeenKeyboardVisible = false
            onKeyboardDismissed()
        }
    }

    Surface(
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            placeholder = { Text("Search apps") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearchSubmitted(query)
                },
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0x66000000),
                unfocusedContainerColor = Color(0x66000000),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = horizontalPaddingDp.dp,
                    vertical = layoutMetrics.searchFieldVerticalPaddingDp.dp,
                )
                .focusRequester(focusRequester),
        )
    }
}
