package com.what386.waterfall.data

enum class LauncherFont(
    val storageValue: String,
    val displayName: String,
) {
    System("system", "System"),
    SansSerif("sans_serif", "Sans Serif"),
    SansSerifCondensed("sans_serif_condensed", "Sans Serif Condensed"),
    SansSerifMedium("sans_serif_medium", "Sans Serif Medium"),
    Serif("serif", "Serif"),
    Monospace("monospace", "Monospace"),
    Cursive("cursive", "Cursive"),
    Casual("casual", "Casual"),
    SmallCaps("small_caps", "Small Caps"),
    ;

    companion object {
        fun fromStorageValue(value: String?): LauncherFont = entries.firstOrNull { font -> font.storageValue == value } ?: System
    }
}
