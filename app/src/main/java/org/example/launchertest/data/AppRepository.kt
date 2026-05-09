package org.example.launchertest.data

import android.content.Intent
import android.content.pm.PackageManager
import org.example.launchertest.ui.model.LauncherApp

class AppRepository(
    private val packageManager: PackageManager,
) {
    fun loadLauncherApps(): List<LauncherApp> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .asSequence()
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                val label = resolveInfo.loadLabel(packageManager)?.toString()?.trim().orEmpty()
                if (label.isBlank()) return@mapNotNull null

                LauncherApp(
                    label = label,
                    packageName = activityInfo.packageName,
                    activityName = activityInfo.name,
                )
            }
            .distinctBy { it.packageName to it.activityName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}
