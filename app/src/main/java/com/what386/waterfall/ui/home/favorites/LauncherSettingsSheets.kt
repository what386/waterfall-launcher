package com.what386.waterfall.ui.home.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.what386.waterfall.R
import com.what386.waterfall.data.HomeRowNavigationMode
import com.what386.waterfall.data.LauncherFont
import com.what386.waterfall.data.LauncherSettings
import com.what386.waterfall.ui.home.LocalHomeLayoutMetrics
import com.what386.waterfall.ui.theme.toPreviewFontFamily

@Composable
internal fun FavoritesOptionsSheet(
    isHiddenMode: Boolean,
    reorderMode: Boolean,
    widgetReorderMode: Boolean,
    hasFavorites: Boolean,
    onHiddenModeClicked: () -> Unit,
    onReorderWidgetsClicked: () -> Unit,
    onReorderFavoritesClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
) {
    val layoutMetrics = LocalHomeLayoutMetrics.current

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = layoutMetrics.sheetHorizontalPaddingDp.dp,
                    end = layoutMetrics.sheetHorizontalPaddingDp.dp,
                    bottom = layoutMetrics.sheetBottomPaddingDp.dp,
                ),
    ) {
        Text(
            text = stringResource(R.string.favorites),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = layoutMetrics.sheetTitleBottomPaddingDp.dp),
        )

        SheetActionRow(
            icon = if (isHiddenMode) "×" else "◌",
            title = stringResource(if (isHiddenMode) R.string.exit_unhide_mode else R.string.unhide_mode),
            subtitle =
                if (isHiddenMode) {
                    stringResource(R.string.return_to_normal_apps)
                } else {
                    stringResource(R.string.show_hidden_apps)
                },
            active = isHiddenMode,
            onClick = onHiddenModeClicked,
        )

        SheetActionRow(
            icon = "↕",
            title =
                stringResource(
                    if (widgetReorderMode) R.string.done_editing_widgets else R.string.edit_widgets,
                ),
            subtitle =
                if (widgetReorderMode) {
                    stringResource(R.string.save_widget_order)
                } else {
                    stringResource(R.string.edit_widgets_summary)
                },
            active = widgetReorderMode,
            onClick = onReorderWidgetsClicked,
        )

        if (hasFavorites) {
            SheetActionRow(
                icon = "↕",
                title =
                    stringResource(
                        if (reorderMode) R.string.done_reordering else R.string.reorder_favorites,
                    ),
                subtitle =
                    if (reorderMode) {
                        stringResource(R.string.save_favorite_order)
                    } else {
                        stringResource(R.string.reorder_favorites_summary)
                    },
                active = reorderMode,
                onClick = onReorderFavoritesClicked,
            )
        }

        SheetActionRow(
            icon = "⚙",
            title = stringResource(R.string.settings),
            subtitle = stringResource(R.string.launcher_settings),
            onClick = onSettingsClicked,
        )
    }
}

@Composable
internal fun SettingsSheet(
    settings: LauncherSettings,
    onHideStatusBarChanged: (Boolean) -> Unit,
    onHideAppIconsChanged: (Boolean) -> Unit,
    onHideSearchButtonChanged: (Boolean) -> Unit,
    onCleanHomeScreenChanged: (Boolean) -> Unit,
    onHomeRowClicked: () -> Unit,
    onFontClicked: () -> Unit,
    onResetClicked: () -> Unit,
    onBackClicked: () -> Unit,
) {
    val layoutMetrics = LocalHomeLayoutMetrics.current
    var showResetConfirmation by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = layoutMetrics.sheetHorizontalPaddingDp.dp,
                    end = layoutMetrics.sheetHorizontalPaddingDp.dp,
                    bottom = layoutMetrics.sheetBottomPaddingDp.dp,
                ),
    ) {
        Text(
            text = stringResource(R.string.settings),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = layoutMetrics.sheetTitleBottomPaddingDp.dp),
        )

        SheetActionRow(
            icon = "‹",
            title = stringResource(R.string.back),
            subtitle = stringResource(R.string.return_to_favorites),
            onClick = onBackClicked,
        )

        SettingsToggleRow(
            title = stringResource(R.string.hide_status_bar),
            subtitle = stringResource(R.string.hide_status_bar_summary),
            checked = settings.hideStatusBar,
            onCheckedChange = onHideStatusBarChanged,
        )

        SettingsToggleRow(
            title = stringResource(R.string.hide_app_icons),
            subtitle = stringResource(R.string.hide_app_icons_summary),
            checked = settings.hideAppIcons,
            onCheckedChange = onHideAppIconsChanged,
        )

        SettingsToggleRow(
            title = stringResource(R.string.hide_search_button),
            subtitle = stringResource(R.string.hide_search_button_summary),
            checked = settings.hideSearchButton,
            onCheckedChange = onHideSearchButtonChanged,
        )

        SettingsToggleRow(
            title = stringResource(R.string.clean_home_screen),
            subtitle = stringResource(R.string.clean_home_screen_summary),
            checked = settings.cleanHomeScreen,
            onCheckedChange = onCleanHomeScreenChanged,
        )

        SheetActionRow(
            icon = "⌂",
            title = stringResource(R.string.home_row),
            subtitle = settings.homeRowNavigationMode.displayName,
            onClick = onHomeRowClicked,
        )

        SheetActionRow(
            icon = "Aa",
            title = stringResource(R.string.font),
            subtitle = settings.font.displayName,
            onClick = onFontClicked,
        )

        SheetActionRow(
            icon = "↺",
            title = stringResource(R.string.reset_to_default),
            subtitle = stringResource(R.string.reset_to_default_summary),
            onClick = { showResetConfirmation = true },
        )
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text(stringResource(R.string.reset_settings_title)) },
            text = { Text(stringResource(R.string.reset_settings_summary)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirmation = false
                        onResetClicked()
                    },
                ) {
                    Text(stringResource(R.string.reset_to_default))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
internal fun HomeRowSheet(
    selectedMode: HomeRowNavigationMode,
    onModeSelected: (HomeRowNavigationMode) -> Unit,
    onBackClicked: () -> Unit,
) {
    val layoutMetrics = LocalHomeLayoutMetrics.current

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = layoutMetrics.sheetHorizontalPaddingDp.dp,
                    end = layoutMetrics.sheetHorizontalPaddingDp.dp,
                    bottom = layoutMetrics.sheetBottomPaddingDp.dp,
                ),
    ) {
        Text(
            text = stringResource(R.string.home_row),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = layoutMetrics.sheetTitleBottomPaddingDp.dp),
        )

        SheetActionRow(
            icon = "‹",
            title = stringResource(R.string.back),
            subtitle = stringResource(R.string.return_to_settings),
            onClick = onBackClicked,
        )

        HomeRowNavigationMode.entries.forEach { mode ->
            SheetActionRow(
                icon = if (mode == selectedMode) "✓" else " ",
                title = mode.displayName,
                subtitle = mode.description,
                active = mode == selectedMode,
                onClick = { onModeSelected(mode) },
            )
        }
    }
}

@Composable
internal fun FontSheet(
    selectedFont: LauncherFont,
    onFontSelected: (LauncherFont) -> Unit,
    onBackClicked: () -> Unit,
) {
    val layoutMetrics = LocalHomeLayoutMetrics.current

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = layoutMetrics.sheetHorizontalPaddingDp.dp,
                    end = layoutMetrics.sheetHorizontalPaddingDp.dp,
                    bottom = layoutMetrics.sheetBottomPaddingDp.dp,
                ),
    ) {
        Text(
            text = stringResource(R.string.font),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = layoutMetrics.sheetTitleBottomPaddingDp.dp),
        )

        SheetActionRow(
            icon = "‹",
            title = stringResource(R.string.back),
            subtitle = stringResource(R.string.return_to_settings),
            onClick = onBackClicked,
        )

        LauncherFont.entries.forEach { font ->
            FontOptionRow(
                icon = if (font == selectedFont) "✓" else " ",
                font = font,
                subtitle =
                    if (font == LauncherFont.System) {
                        stringResource(R.string.use_android_default_font)
                    } else {
                        stringResource(R.string.preview_font, font.displayName)
                    },
                active = font == selectedFont,
                onClick = { onFontSelected(font) },
            )
        }
    }
}

@Composable
internal fun FontOptionRow(
    icon: String,
    font: LauncherFont,
    subtitle: String,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val layoutMetrics = LocalHomeLayoutMetrics.current

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = layoutMetrics.sheetRowVerticalPaddingDp.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = icon,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.size(layoutMetrics.sheetIconSizeDp.dp),
        )

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = layoutMetrics.sheetContentStartPaddingDp.dp),
        ) {
            Text(
                text = font.displayName,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontFamily = font.toPreviewFontFamily(),
                    ),
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
internal fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val layoutMetrics = LocalHomeLayoutMetrics.current

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = layoutMetrics.sheetRowVerticalPaddingDp.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
internal fun SheetActionRow(
    icon: String,
    title: String,
    subtitle: String,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    val layoutMetrics = LocalHomeLayoutMetrics.current

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = layoutMetrics.sheetRowVerticalPaddingDp.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = icon,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.size(layoutMetrics.sheetIconSizeDp.dp),
        )

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = layoutMetrics.sheetContentStartPaddingDp.dp),
        ) {
            Text(
                text = title,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
