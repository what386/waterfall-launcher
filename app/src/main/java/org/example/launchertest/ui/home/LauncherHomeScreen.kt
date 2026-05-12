package org.example.launchertest.ui.home

import org.example.launchertest.ui.home.applist.AppListPanel
import org.example.launchertest.ui.home.applist.APP_LIST_CATEGORY_PIN_OFFSET_DP
import org.example.launchertest.ui.home.azrail.AzRailPanel
import org.example.launchertest.ui.home.azrail.buildRailLetters
import org.example.launchertest.ui.home.azrail.isFavoritesRailItem

import android.app.WallpaperManager
import android.widget.ImageView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.example.launchertest.domain.LauncherInteractor
import org.example.launchertest.widgets.LauncherWidgetController
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb

@Composable
private fun WallpaperBackground(
    modifier: Modifier = Modifier,
) {
    val fallbackColor = MaterialTheme.colorScheme.background.toArgb()

    AndroidView(
        modifier = modifier,
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP

                // Attempt to get wallpaper
                val wallpaperManager = WallpaperManager.getInstance(context)
                val drawable = try {
                    wallpaperManager.drawable
                } catch (e: SecurityException) {
                    null
                }

                if (drawable != null) {
                    setImageDrawable(drawable)
                } else {
                    setBackgroundColor(fallbackColor)
                }
            }
        },
        update = { /* Leave empty to avoid overwriting the factory setup */ }
    )
}

@Composable
fun LauncherHomeRoute(
    interactor: LauncherInteractor,
    widgetController: LauncherWidgetController,
) {
    val vm: LauncherHomeViewModel = viewModel(
        factory = LauncherHomeViewModelFactory(interactor),
    )
    val state by vm.uiState.collectAsStateWithLifecycle()
    val widgetIds by widgetController.widgetIds.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val density = LocalDensity.current
    val categoryPinOffsetPx = with(density) { APP_LIST_CATEGORY_PIN_OFFSET_DP.dp.toPx() }.toInt()

    LaunchedEffect(categoryPinOffsetPx, listState, vm) {
        vm.jumpToTarget.collectLatest { targetIndex ->
            // Negative offset pins the selected category below the top edge.
            listState.scrollToItem(
                index = targetIndex,
                scrollOffset = -categoryPinOffsetPx,
            )
        }
    }

    LauncherHomeScreen(
        state = state,
        widgetIds = widgetIds,
        listState = listState,
        categoryPinOffsetPx = categoryPinOffsetPx,
        onQueryChanged = vm::onQueryChanged,
        onSearchActivated = vm::onSearchActivated,
        onSearchDismissed = vm::onSearchDismissed,
        onToggleFavorite = vm::onToggleFavorite,
        onHideApp = vm::onHideApp,
        onReorderFavorites = vm::onFavoriteOrderChanged,
        onLetterSelected = vm::onLetterSelected,
        onAddWidget = widgetController::addWidget,
        onRemoveWidget = widgetController::removeWidget,
        createWidgetView = widgetController::createWidgetView,
    )
}

@Composable
private fun LauncherHomeScreen(
    state: LauncherHomeUiState,
    widgetIds: List<Int>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    categoryPinOffsetPx: Int,
    onQueryChanged: (String) -> Unit,
    onSearchActivated: () -> Unit,
    onSearchDismissed: () -> Unit,
    onToggleFavorite: (org.example.launchertest.ui.model.LauncherApp) -> Unit,
    onHideApp: (org.example.launchertest.ui.model.LauncherApp) -> Unit,
    onReorderFavorites: (List<org.example.launchertest.ui.model.LauncherApp>) -> Unit,
    onLetterSelected: (Char) -> Unit,
    onAddWidget: () -> Unit,
    onRemoveWidget: (Int) -> Unit,
    createWidgetView: (Int) -> android.appwidget.AppWidgetHostView?,
) {
    val scrubbingLetter = remember { mutableStateOf<Char?>(null) }
    val isScrubbing = remember { mutableStateOf(false) }
    val selectedRailItem = remember { mutableStateOf(buildRailLetters(emptyMap()).first()) }

    var showFavoritesOnly by remember { mutableStateOf(true) }

    val coroutineScope = rememberCoroutineScope()

    val railLetters = remember(state.listLayout.letterJumpTargets) {
        buildRailLetters(state.listLayout.letterJumpTargets)
    }

    fun selectRailItem(item: Char) {
        selectedRailItem.value = item

        if (isFavoritesRailItem(item)) {
            showFavoritesOnly = true
            scrubbingLetter.value = null
            isScrubbing.value = false

            coroutineScope.launch {
                listState.scrollToItem(
                    index = if (state.listLayout.favorites.isNotEmpty()) {
                        favoritesHeaderIndex(widgetIds.size)
                    } else {
                        FirstHomeContentIndex
                    },
                    scrollOffset = -categoryPinOffsetPx,
                )
            }
        } else {
            showFavoritesOnly = false
            scrubbingLetter.value = item
            isScrubbing.value = true
            onLetterSelected(item)
        }
    }

    if (state.isSearchActive) {
        BackHandler(onBack = onSearchDismissed)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent,
        contentColor = Color.White.copy(alpha = 0.92f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            AppListPanel(
                listLayout = state.listLayout,
                widgetIds = widgetIds,
                scrubbingLetter = scrubbingLetter,
                isScrubbing = isScrubbing,
                showFavoritesOnly = showFavoritesOnly,
                isSearchActive = state.isSearchActive,
                listState = listState,
                categoryPinOffsetPx = categoryPinOffsetPx,
                onSearchActivated = onSearchActivated,
                onToggleFavorite = onToggleFavorite,
                onHideApp = onHideApp,
                onReorderFavorites = onReorderFavorites,
                onAddWidget = onAddWidget,
                onRemoveWidget = onRemoveWidget,
                createWidgetView = createWidgetView,
                modifier = Modifier.fillMaxSize(),
            )

            if (state.isSearchActive) {
                SearchOverlay(
                    query = state.query,
                    onQueryChanged = onQueryChanged,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            horizontal = 12.dp,
                            vertical = 12.dp,
                        ),
                )
            }

            AzRailPanel(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(y = 95.dp)
                    .fillMaxHeight(0.5f)
                    .padding(end = 28.dp)
                    .width(36.dp),
                letters = railLetters,
                onLetterSelected = {},
                onScrubStart = ::selectRailItem,
                onScrubMove = ::selectRailItem,
                onScrubEnd = {
                    if (!isFavoritesRailItem(selectedRailItem.value)) {
                        scrubbingLetter.value = null
                        isScrubbing.value = false
                    }
                },
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(y = (-105).dp)
                    .padding(end = 28.dp)
                    .width(36.dp)
                    .fillMaxHeight(0.16f)
                    .clickable { selectRailItem(railLetters.first()) },
            )
        }
    }
}
private const val FirstHomeContentIndex = 1
private fun favoritesHeaderIndex(widgetCount: Int): Int = widgetCount + 2
