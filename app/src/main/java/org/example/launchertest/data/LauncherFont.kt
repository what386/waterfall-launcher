package org.example.launchertest.data

enum class LauncherFont(
    val storageValue: String,
    val displayName: String,
) {
    System("system", "System"),
    SansSerif("sans_serif", "Sans Serif"),
    Serif("serif", "Serif"),
    Monospace("monospace", "Monospace"),
    Cursive("cursive", "Cursive"),
    ;

    companion object {
        fun fromStorageValue(value: String?): LauncherFont {
            return entries.firstOrNull { font -> font.storageValue == value } ?: System
        }
    }
}
