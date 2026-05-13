package org.example.launchertest.widgets

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.example.launchertest.data.LauncherPreferencesRepository

class LauncherWidgetController(
    private val activity: ComponentActivity,
    private val preferencesRepository: LauncherPreferencesRepository,
    private val scope: CoroutineScope,
) {
    private val appWidgetManager = AppWidgetManager.getInstance(activity)
    private val appWidgetHost = AppWidgetHost(activity.applicationContext, WidgetHostId)

    val widgetIds: StateFlow<List<Int>> = preferencesRepository.widgetIds.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )

    private val configureWidgetLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val appWidgetId = pendingConfigureWidgetId ?: return@registerForActivityResult
        pendingConfigureWidgetId = null
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch { preferencesRepository.addWidgetId(appWidgetId) }
        } else {
            appWidgetHost.deleteAppWidgetId(appWidgetId)
        }
    }

    private val pickWidgetLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val fallbackWidgetId = pendingPickWidgetId ?: return@registerForActivityResult
        val appWidgetId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, fallbackWidgetId)
            ?: fallbackWidgetId
        pendingPickWidgetId = null

        if (result.resultCode != Activity.RESULT_OK) {
            appWidgetHost.deleteAppWidgetId(appWidgetId)
            return@registerForActivityResult
        }

        val providerInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
        if (providerInfo?.configure != null) {
            pendingConfigureWidgetId = appWidgetId
            configureWidgetLauncher.launch(
                Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                    component = providerInfo.configure
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                },
            )
        } else {
            scope.launch { preferencesRepository.addWidgetId(appWidgetId) }
        }
    }

    private var pendingPickWidgetId: Int? = null
    private var pendingConfigureWidgetId: Int? = null

    fun startListening() {
        appWidgetHost.startListening()
    }

    fun stopListening() {
        appWidgetHost.stopListening()
    }

    fun addWidget() {
        val appWidgetId = appWidgetHost.allocateAppWidgetId()
        pendingPickWidgetId = appWidgetId
        pickWidgetLauncher.launch(
            Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            },
        )
    }

    fun removeWidget(appWidgetId: Int) {
        appWidgetHost.deleteAppWidgetId(appWidgetId)
        scope.launch {
            preferencesRepository.removeWidgetId(appWidgetId)
        }
    }

    fun reorderWidgets(appWidgetIds: List<Int>) {
        scope.launch {
            preferencesRepository.setWidgetIds(appWidgetIds)
        }
    }

    fun createWidgetView(appWidgetId: Int): AppWidgetHostView? {
        val providerInfo = appWidgetManager.getAppWidgetInfo(appWidgetId) ?: return null
        return appWidgetHost.createView(activity, appWidgetId, providerInfo).apply {
            setAppWidget(appWidgetId, providerInfo)
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
    }

    companion object {
        private const val WidgetHostId = 2048
    }
}
