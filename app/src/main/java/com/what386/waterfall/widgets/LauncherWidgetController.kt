package com.what386.waterfall.widgets

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.what386.waterfall.data.LauncherPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LauncherWidgetController(
    private val activity: ComponentActivity,
    private val preferencesRepository: LauncherPreferencesRepository,
    private val scope: CoroutineScope,
) {
    private val appWidgetManager = AppWidgetManager.getInstance(activity)
    private val appWidgetHost = AppWidgetHost(activity.applicationContext, WidgetHostId)

    init {
        scope.launch {
            val storedStacks = preferencesRepository.widgetStacks.first()
            val validStacks =
                storedStacks
                    .map { stack ->
                        stack.copy(
                            widgetIds =
                                stack.widgetIds.filter { appWidgetId ->
                                    appWidgetManager.getAppWidgetInfo(appWidgetId) != null
                                },
                        )
                    }.filter { it.widgetIds.isNotEmpty() }
            if (validStacks != storedStacks) {
                preferencesRepository.setWidgetStacks(validStacks)
            }
        }
    }

    val widgetStacks: StateFlow<List<WidgetStack>> =
        preferencesRepository.widgetStacks.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    private val configureWidgetLauncher =
        activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            val appWidgetId = pendingConfigureWidgetId ?: return@registerForActivityResult
            pendingConfigureWidgetId = null
            if (result.resultCode == Activity.RESULT_OK) {
                persistPickedWidget(appWidgetId)
            } else {
                pendingWidgetStackIndex = null
                appWidgetHost.deleteAppWidgetId(appWidgetId)
            }
        }

    private val pickWidgetLauncher =
        activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            val fallbackWidgetId = pendingPickWidgetId ?: return@registerForActivityResult
            val appWidgetId =
                result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, fallbackWidgetId)
                    ?: fallbackWidgetId
            pendingPickWidgetId = null

            if (result.resultCode != Activity.RESULT_OK) {
                pendingWidgetStackIndex = null
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
                persistPickedWidget(appWidgetId)
            }
        }

    private var pendingPickWidgetId: Int? = null
    private var pendingConfigureWidgetId: Int? = null
    private var pendingWidgetStackIndex: Int? = null

    fun startListening() {
        appWidgetHost.startListening()
    }

    fun stopListening() {
        appWidgetHost.stopListening()
    }

    fun addWidgetToNewStack() {
        pendingWidgetStackIndex = null
        launchWidgetPicker()
    }

    fun addWidgetToStack(stackIndex: Int) {
        pendingWidgetStackIndex = stackIndex
        launchWidgetPicker()
    }

    private fun launchWidgetPicker() {
        val appWidgetId = appWidgetHost.allocateAppWidgetId()
        pendingPickWidgetId = appWidgetId
        pickWidgetLauncher.launch(
            Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            },
        )
    }

    private fun persistPickedWidget(appWidgetId: Int) {
        val stackIndex = pendingWidgetStackIndex
        pendingWidgetStackIndex = null
        scope.launch {
            if (stackIndex == null) {
                preferencesRepository.addWidgetId(appWidgetId)
            } else {
                preferencesRepository.addWidgetIdToStack(appWidgetId, stackIndex)
            }
        }
    }

    fun removeWidget(appWidgetId: Int) {
        appWidgetHost.deleteAppWidgetId(appWidgetId)
        scope.launch {
            preferencesRepository.removeWidgetId(appWidgetId)
        }
    }

    fun reorderWidgetStacks(stacks: List<WidgetStack>) {
        scope.launch {
            preferencesRepository.setWidgetStacks(stacks)
        }
    }

    fun createWidgetView(appWidgetId: Int): AppWidgetHostView? {
        val providerInfo = appWidgetManager.getAppWidgetInfo(appWidgetId) ?: return null
        return appWidgetHost.createView(activity, appWidgetId, providerInfo).apply {
            setAppWidget(appWidgetId, providerInfo)
            layoutParams =
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
        }
    }

    fun getWidgetMinHeightDp(appWidgetId: Int): Int? = appWidgetManager.getAppWidgetInfo(appWidgetId)?.minHeight

    companion object {
        private const val WidgetHostId = 2048
    }
}
