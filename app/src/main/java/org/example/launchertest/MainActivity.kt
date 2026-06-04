package org.example.launchertest

import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.example.launchertest.data.AppRepository
import org.example.launchertest.data.HomeRowNavigationMode
import org.example.launchertest.data.LauncherPreferencesRepository
import org.example.launchertest.domain.LauncherInteractor
import org.example.launchertest.ui.home.LauncherHomeRoute
import org.example.launchertest.ui.theme.LauncherTheme
import org.example.launchertest.widgets.LauncherWidgetController

class MainActivity : ComponentActivity() {
    private lateinit var widgetController: LauncherWidgetController
    private var homeRowNavigationMode = HomeRowNavigationMode.Shown
    private val homeIntentPressCount = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }


        setHighRefreshRate()

        val appRepository = AppRepository(
            context = applicationContext,
            packageManager = packageManager,
            selfPackageName = packageName,
        )

        val preferencesRepository = LauncherPreferencesRepository(applicationContext)
        lifecycleScope.launch {
            preferencesRepository.settings
                .map { settings -> settings.homeRowNavigationMode }
                .distinctUntilChanged()
                .collect { mode ->
                    homeRowNavigationMode = mode
                    applyHomeRowNavigationMode(mode)
                }
        }

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
                    homeIntentPressCount = homeIntentPressCount,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            homeIntentPressCount.value += 1
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
            applyHomeRowNavigationMode(homeRowNavigationMode)
        }
    }

    private fun applyHomeRowNavigationMode(mode: HomeRowNavigationMode) {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            if (mode == HomeRowNavigationMode.Hidden) {
                hide(WindowInsetsCompat.Type.navigationBars())
            } else {
                show(WindowInsetsCompat.Type.navigationBars())
            }
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
