package com.what386.waterfall.ui.theme

import android.graphics.Typeface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.what386.waterfall.data.LauncherFont

private val LauncherDarkColors = darkColorScheme()

@Composable
fun LauncherTheme(
    font: LauncherFont = LauncherFont.System,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LauncherDarkColors,
        typography = MaterialTheme.typography.withLauncherFont(font),
        content = content,
    )
}

private fun Typography.withLauncherFont(font: LauncherFont): Typography {
    val family = font.toFontFamily() ?: return this
    return Typography(
        displayLarge = displayLarge.withFontFamily(family),
        displayMedium = displayMedium.withFontFamily(family),
        displaySmall = displaySmall.withFontFamily(family),
        headlineLarge = headlineLarge.withFontFamily(family),
        headlineMedium = headlineMedium.withFontFamily(family),
        headlineSmall = headlineSmall.withFontFamily(family),
        titleLarge = titleLarge.withFontFamily(family),
        titleMedium = titleMedium.withFontFamily(family),
        titleSmall = titleSmall.withFontFamily(family),
        bodyLarge = bodyLarge.withFontFamily(family),
        bodyMedium = bodyMedium.withFontFamily(family),
        bodySmall = bodySmall.withFontFamily(family),
        labelLarge = labelLarge.withFontFamily(family),
        labelMedium = labelMedium.withFontFamily(family),
        labelSmall = labelSmall.withFontFamily(family),
    )
}

private fun TextStyle.withFontFamily(fontFamily: FontFamily): TextStyle = copy(fontFamily = fontFamily)

internal fun LauncherFont.toPreviewFontFamily(): FontFamily = toFontFamily() ?: FontFamily.Default

private fun LauncherFont.toFontFamily(): FontFamily? =
    when (this) {
        LauncherFont.System -> null
        LauncherFont.SansSerif -> FontFamily.SansSerif
        LauncherFont.SansSerifCondensed ->
            FontFamily(
                Typeface.create("sans-serif-condensed", Typeface.NORMAL),
            )
        LauncherFont.SansSerifMedium ->
            FontFamily(
                Typeface.create("sans-serif-medium", Typeface.NORMAL),
            )
        LauncherFont.Serif -> FontFamily.Serif
        LauncherFont.Monospace -> FontFamily.Monospace
        LauncherFont.Cursive -> FontFamily.Cursive
        LauncherFont.Casual ->
            FontFamily(
                Typeface.create("casual", Typeface.NORMAL),
            )
        LauncherFont.SmallCaps ->
            FontFamily(
                Typeface.create("sans-serif-smallcaps", Typeface.NORMAL),
            )
    }
