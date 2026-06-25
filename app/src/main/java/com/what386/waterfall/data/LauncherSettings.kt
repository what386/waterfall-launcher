package com.what386.waterfall.data

data class LauncherSettings(
    val hideStatusBar: Boolean = false,
    val hideAppIcons: Boolean = false,
    val hideSearchButton: Boolean = false,
    val cleanHomeScreen: Boolean = false,
    val homeRowNavigationMode: HomeRowNavigationMode = HomeRowNavigationMode.Shown,
    val font: LauncherFont = LauncherFont.System,
)

enum class HomeRowNavigationMode(
    val storageValue: String,
    val displayName: String,
    val description: String,
) {
    Shown(
        storageValue = "shown",
        displayName = "Shown",
        description = "Always show navigation buttons",
    ),
    Hidden(
        storageValue = "hidden",
        displayName = "Hidden",
        description = "Hide navigation buttons",
    );

    companion object {
        fun fromStorageValue(value: String?): HomeRowNavigationMode {
            return entries.firstOrNull { it.storageValue == value } ?: Shown
        }
    }
}
