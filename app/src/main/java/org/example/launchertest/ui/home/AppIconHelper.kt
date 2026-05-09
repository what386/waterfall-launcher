package org.example.launchertest.ui.home

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberAppIcon(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    // produceState launches a coroutine; Dispatchers.IO keeps decode off the main thread.
    // The null initial value means rows render immediately without blocking.
    return produceState<ImageBitmap?>(initialValue = null, key1 = packageName) {
        value = withContext(Dispatchers.IO) {
            try {
                val drawable: Drawable = context.packageManager.getApplicationIcon(packageName)
                drawable.toBitmap().asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }.value
}
