package com.what386.waterfall.ui.home.shared

import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.util.concurrent.ConcurrentHashMap

private const val MaxCachedAppIcons = 128

private val appIconCache = object : LruCache<String, ImageBitmap>(MaxCachedAppIcons) {}
private val inFlightIconLoads = ConcurrentHashMap<String, Deferred<ImageBitmap?>>()

@Composable
fun rememberAppIcon(packageName: String): ImageBitmap? {
    val packageManager = LocalContext.current.applicationContext.packageManager
    return produceState<ImageBitmap?>(initialValue = appIconCache.get(packageName), key1 = packageName) {
        if (value != null) {
            return@produceState
        }

        val newLoad = async(Dispatchers.IO) {
            try {
                val drawable: Drawable = packageManager.getApplicationIcon(packageName)
                drawable.toBitmap().asImageBitmap().also { icon ->
                    appIconCache.put(packageName, icon)
                }
            } catch (_: Exception) {
                null
            }
        }
        val load = inFlightIconLoads.putIfAbsent(packageName, newLoad) ?: newLoad
        if (load !== newLoad) {
            newLoad.cancel()
        }

        value = try {
            load.await()
        } finally {
            inFlightIconLoads.remove(packageName, load)
        }
    }.value
}
