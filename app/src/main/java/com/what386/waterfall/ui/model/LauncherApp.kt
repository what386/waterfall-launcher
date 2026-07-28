package com.what386.waterfall.ui.model

data class LauncherApp(
    val label: String,
    val packageName: String,
    val activityName: String,
    val isFavorite: Boolean = false,
) {
    val componentId: String
        get() = "$packageName/$activityName"
}
