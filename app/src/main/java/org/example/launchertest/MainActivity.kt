package org.example.launchertest

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import org.example.launchertest.data.AppRepository
import org.example.launchertest.data.LauncherPreferencesRepository
import org.example.launchertest.domain.LauncherInteractor
import org.example.launchertest.ui.home.LauncherHomeRoute
import org.example.launchertest.ui.theme.LauncherTheme
import org.example.launchertest.widgets.LauncherWidgetController

class MainActivity : ComponentActivity() {
    private lateinit var widgetController: LauncherWidgetController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request high refresh rate (120Hz)
        setHighRefreshRate()

        val appRepository = AppRepository(packageManager)
        val preferencesRepository = LauncherPreferencesRepository(applicationContext)
        val interactor = LauncherInteractor(appRepository, preferencesRepository)
        widgetController = LauncherWidgetController(this, preferencesRepository, lifecycleScope)

        setContent {
            LauncherTheme {
                LauncherHomeRoute(
                    interactor = interactor,
                    widgetController = widgetController,
                )
            }
        }
    }

    /**
     * Finds the highest refresh rate supported by the display and requests it.
     */
    private fun setHighRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = display ?: return
            val modes = display.supportedModes
            // Filter modes for the highest refresh rate
            val maxMode = modes.maxByOrNull { it.refreshRate }

            if (maxMode != null) {
                val params = window.attributes
                params.preferredDisplayModeId = maxMode.modeId
                window.attributes = params
            }
        } else {
            // Fallback for older versions (API 23-29)
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }
    }

    override fun onStart() {
        super.onStart()
        widgetController.startListening()
    }

    override fun onStop() {
        widgetController.stopListening()
        super.onStop()
    }
}
