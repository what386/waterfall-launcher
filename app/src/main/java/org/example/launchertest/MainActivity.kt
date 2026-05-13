package org.example.launchertest

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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

        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }


        hideNavigationBar()
        setHighRefreshRate()

        val appRepository = AppRepository(
            packageManager = packageManager,
            selfPackageName = packageName,
        )

        val preferencesRepository = LauncherPreferencesRepository(applicationContext)

        val interactor = LauncherInteractor(
            appRepository = appRepository,
            preferencesRepository = preferencesRepository,
        )


        widgetController = LauncherWidgetController(
            activity = this,
            preferencesRepository = preferencesRepository,
            scope = lifecycleScope,
        )

        setContent {
            LauncherTheme {
                LauncherHomeRoute(
                    interactor = interactor,
                    widgetController = widgetController,
                )
            }
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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            hideNavigationBar()
        }
    }

    private fun hideNavigationBar() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    /**
     * Finds the highest refresh rate supported by the display and requests it.
     */
    private fun setHighRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = display ?: return
            val maxMode = display.supportedModes.maxByOrNull { it.refreshRate }

            if (maxMode != null) {
                val params = window.attributes
                params.preferredDisplayModeId = maxMode.modeId
                window.attributes = params
            }
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        }
    }
}
