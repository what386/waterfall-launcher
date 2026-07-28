package com.what386.waterfall.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.what386.waterfall.ui.model.LauncherApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AppRepository(
    private val context: Context,
    private val packageManager: PackageManager,
    private val selfPackageName: String,
) {
    fun launcherAppsFlow(): Flow<List<LauncherApp>> =
        packageChangesFlow()
            .map { withContext(Dispatchers.IO) { loadLauncherApps() } }
            .distinctUntilChanged()

    fun loadLauncherApps(): List<LauncherApp> {
        val intent =
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

        return packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .asSequence()
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                val label = resolveInfo.loadLabel(packageManager).toString().trim()
                if (label.isBlank()) return@mapNotNull null

                LauncherApp(
                    label = label,
                    packageName = activityInfo.packageName,
                    activityName = activityInfo.name,
                )
            }.filterNot { it.packageName == selfPackageName }
            .distinctBy { it.packageName to it.activityName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private fun packageChangesFlow(): Flow<Unit> =
        callbackFlow {
            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(
                        context: Context?,
                        intent: Intent?,
                    ) {
                        trySend(Unit)
                    }
                }
            val filter =
                IntentFilter().apply {
                    addAction(Intent.ACTION_PACKAGE_ADDED)
                    addAction(Intent.ACTION_PACKAGE_CHANGED)
                    addAction(Intent.ACTION_PACKAGE_REMOVED)
                    addAction(Intent.ACTION_PACKAGE_REPLACED)
                    addDataScheme("package")
                }

            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            trySend(Unit)

            awaitClose { context.unregisterReceiver(receiver) }
        }.conflate()
}
