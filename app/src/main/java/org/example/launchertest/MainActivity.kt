package org.example.launchertest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.example.launchertest.data.AppRepository
import org.example.launchertest.data.FavoritesRepository
import org.example.launchertest.domain.LauncherInteractor
import org.example.launchertest.ui.home.LauncherHomeRoute
import org.example.launchertest.ui.theme.LauncherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appRepository = AppRepository(packageManager)
        val favoritesRepository = FavoritesRepository(applicationContext)
        val interactor = LauncherInteractor(appRepository, favoritesRepository)

        setContent {
            LauncherTheme {
                LauncherHomeRoute(interactor = interactor)
            }
        }
    }
}
