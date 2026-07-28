package com.what386.waterfall.domain

import com.what386.waterfall.ui.model.LauncherApp
import java.text.Normalizer
import java.util.Locale

internal fun normalizedSearchText(value: String): String =
    Normalizer
        .normalize(value.trim(), Normalizer.Form.NFKD)
        .replace(CombiningMarks, "")
        .lowercase(Locale.ROOT)

internal fun LauncherApp.matchesSearch(rawQuery: String): Boolean {
    val query = normalizedSearchText(rawQuery)
    if (query.isBlank()) return true
    val normalizedLabel = normalizedSearchText(label)
    val initials =
        normalizedLabel
            .split(NonWordCharacters)
            .mapNotNull { it.firstOrNull() }
            .joinToString("")
    return normalizedLabel.contains(query) || initials.startsWith(query)
}

internal fun bestSearchMatch(
    apps: List<LauncherApp>,
    rawQuery: String,
): LauncherApp? {
    val query = normalizedSearchText(rawQuery)
    if (query.isBlank()) return null

    return apps
        .asSequence()
        .filter { it.matchesSearch(query) }
        .minWithOrNull(
            compareBy<LauncherApp>(
                { app -> searchRank(normalizedSearchText(app.label), query) },
                { app -> normalizedSearchText(app.label) },
                { app -> app.componentId },
            ),
        )
}

private fun searchRank(
    label: String,
    query: String,
): Int {
    val words = label.split(NonWordCharacters).filter { it.isNotBlank() }
    val initials = words.mapNotNull { it.firstOrNull() }.joinToString("")
    return when {
        label == query -> 0
        label.startsWith(query) -> 1
        words.any { it.startsWith(query) } -> 2
        initials.startsWith(query) -> 3
        else -> 4
    }
}

private val CombiningMarks = Regex("\\p{M}+")
private val NonWordCharacters = Regex("[^\\p{L}\\p{N}]+")
