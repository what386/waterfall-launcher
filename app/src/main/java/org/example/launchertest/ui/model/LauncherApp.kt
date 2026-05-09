package org.example.launchertest.ui.model

data class LauncherApp(
    val label: String,
    val packageName: String,
    val activityName: String,
    val isFavorite: Boolean = false,
)
