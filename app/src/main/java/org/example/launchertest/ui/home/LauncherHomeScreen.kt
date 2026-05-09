package org.example.launchertest.ui.home

import android.content.ComponentName
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.example.launchertest.domain.LauncherInteractor
import org.example.launchertest.ui.model.LauncherApp

@Composable
fun LauncherHomeRoute(
    interactor: LauncherInteractor,
) {
    val vm: LauncherHomeViewModel = viewModel(
        factory = LauncherHomeViewModelFactory(interactor),
    )
    val state by vm.uiState.collectAsStateWithLifecycle()

    LauncherHomeScreen(
        state = state,
        onQueryChanged = vm::onQueryChanged,
        onToggleFavorite = vm::onToggleFavorite,
    )
}

@Composable
private fun LauncherHomeScreen(
    state: LauncherHomeUiState,
    onQueryChanged: (String) -> Unit,
    onToggleFavorite: (LauncherApp) -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = "Launcher",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChanged,
                label = { Text("Search apps") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp),
            )

            if (state.favorites.isNotEmpty()) {
                Text(
                    text = "Favorites",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.apps, key = { it.packageName + it.activityName }) { app ->
                    AppRow(
                        app = app,
                        onToggleFavorite = onToggleFavorite,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(
    app: LauncherApp,
    onToggleFavorite: (LauncherApp) -> Unit,
) {
    val context = LocalContext.current
    val favoritePrefix = if (app.isFavorite) "* " else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    val launchIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        component = ComponentName(app.packageName, app.activityName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                    try {
                        context.startActivity(launchIntent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "Unable to launch ${app.label}", Toast.LENGTH_SHORT).show()
                    }
                },
                onLongClick = { onToggleFavorite(app) },
            )
            .padding(horizontal = 10.dp, vertical = 12.dp),
    ) {
        Text(
            text = favoritePrefix + app.label,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
