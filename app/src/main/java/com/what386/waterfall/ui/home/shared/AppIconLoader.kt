package com.what386.waterfall.ui.home.shared

import android.content.ComponentName
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.what386.waterfall.ui.model.LauncherApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MaxCachedAppIconBytes = 16 * 1024 * 1024
private val AppIconDecodeSizeDp = 64.dp

private val appIconCache =
    object : LruCache<String, ImageBitmap>(MaxCachedAppIconBytes) {
        override fun sizeOf(
            key: String,
            value: ImageBitmap,
        ): Int = value.width * value.height * 4
    }

@Composable
fun rememberAppIcon(app: LauncherApp): ImageBitmap? {
    val packageManager = LocalContext.current.applicationContext.packageManager
    val iconDecodeSizePx = with(LocalDensity.current) { AppIconDecodeSizeDp.roundToPx() }
    val cacheKey = "${app.componentId}@$iconDecodeSizePx"
    return produceState<ImageBitmap?>(
        initialValue = appIconCache.get(cacheKey),
        key1 = app.componentId,
        key2 = iconDecodeSizePx,
    ) {
        if (value != null) {
            return@produceState
        }

        value =
            withContext(Dispatchers.IO) {
                try {
                    val drawable =
                        packageManager.getActivityIcon(
                            ComponentName(app.packageName, app.activityName),
                        )
                    drawable
                        .toBitmap(
                            width = iconDecodeSizePx,
                            height = iconDecodeSizePx,
                            config = Bitmap.Config.ARGB_8888,
                        ).asImageBitmap().also { icon ->
                        appIconCache.put(cacheKey, icon)
                    }
                } catch (_: Exception) {
                    null
                }
            }
    }.value
}
