package com.what386.waterfall.ui.home.shared

import android.content.ComponentName
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import com.what386.waterfall.ui.model.LauncherApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MaxCachedAppIconBytes = 16 * 1024 * 1024

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
    return produceState<ImageBitmap?>(initialValue = appIconCache.get(app.componentId), key1 = app.componentId) {
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
                    drawable.toBitmap().asImageBitmap().also { icon ->
                        appIconCache.put(app.componentId, icon)
                    }
                } catch (_: Exception) {
                    null
                }
            }
    }.value
}
