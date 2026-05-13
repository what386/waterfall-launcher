package org.example.launchertest.ui.home

import org.example.launchertest.ui.home.applist.AppListPanel
import org.example.launchertest.ui.home.applist.APP_LIST_CATEGORY_PIN_OFFSET_DP
import org.example.launchertest.ui.home.azrail.AzRailPanel
import org.example.launchertest.ui.home.azrail.buildRailLetters
import org.example.launchertest.ui.home.azrail.isFavoritesRailItem

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.example.launchertest.domain.LauncherInteractor
import org.example.launchertest.data.LauncherFont
import org.example.launchertest.ui.model.LauncherApp
import org.example.launchertest.ui.theme.LauncherTheme
import org.example.launchertest.widgets.LauncherWidgetController
import org.example.launchertest.widgets.WidgetStack
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
    val widgetStacks by widgetController.widgetStacks.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current

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

    LauncherTheme(font = state.settings.font) {
        LauncherHomeScreen(
            state = state,
            widgetStacks = widgetStacks,
            listState = listState,
            categoryPinOffsetPx = categoryPinOffsetPx,
            onQueryChanged = vm::onQueryChanged,
            onSearchActivated = vm::onSearchActivated,
            onSearchDismissed = vm::onSearchDismissed,
            onHiddenModeChanged = vm::onHiddenModeChanged,
            onToggleFavorite = vm::onToggleFavorite,
            onHideApp = vm::onHideApp,
            onUnhideApp = vm::onUnhideApp,
            onReorderFavorites = vm::onFavoriteOrderChanged,
            onHideStatusBarChanged = vm::onHideStatusBarChanged,
            onHideAppIconsChanged = vm::onHideAppIconsChanged,
            onFontChanged = vm::onFontChanged,
            onRestartLauncher = {
                context.findActivity()?.recreate()
            },
            onLetterSelected = vm::onLetterSelected,
            onAddWidget = widgetController::addWidgetToNewStack,
            onAddWidgetToStack = widgetController::addWidgetToStack,
            onRemoveWidget = widgetController::removeWidget,
            onReorderWidgetStacks = widgetController::reorderWidgetStacks,
            createWidgetView = widgetController::createWidgetView,
            getWidgetMinHeightDp = widgetController::getWidgetMinHeightDp,
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun LauncherHomeScreen(
    state: LauncherHomeUiState,
    widgetStacks: List<WidgetStack>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    categoryPinOffsetPx: Int,
    onQueryChanged: (String) -> Unit,
    onSearchActivated: () -> Unit,
    onSearchDismissed: () -> Unit,
    onHiddenModeChanged: (Boolean) -> Unit,
    onToggleFavorite: (org.example.launchertest.ui.model.LauncherApp) -> Unit,
    onHideApp: (org.example.launchertest.ui.model.LauncherApp) -> Unit,
    onUnhideApp: (org.example.launchertest.ui.model.LauncherApp) -> Unit,
    onReorderFavorites: (List<org.example.launchertest.ui.model.LauncherApp>) -> Unit,
    onHideStatusBarChanged: (Boolean) -> Unit,
    onHideAppIconsChanged: (Boolean) -> Unit,
    onFontChanged: (LauncherFont) -> Unit,
    onRestartLauncher: () -> Unit,
    onLetterSelected: (Char) -> Unit,
    onAddWidget: () -> Unit,
    onAddWidgetToStack: (Int) -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onReorderWidgetStacks: (List<WidgetStack>) -> Unit,
    createWidgetView: (Int) -> android.appwidget.AppWidgetHostView?,
    getWidgetMinHeightDp: (Int) -> Int?,
) {
    val scrubbingLetter = remember { mutableStateOf<Char?>(null) }
    val isScrubbing = remember { mutableStateOf(false) }
    val selectedRailItem = remember { mutableStateOf(buildRailLetters(emptyMap()).first()) }
    val context = LocalContext.current
    val view = LocalView.current
    var isRailDragging by remember { mutableStateOf(false) }

    var contentMode by remember { mutableStateOf(HomeContentMode.Favorites) }

    val coroutineScope = rememberCoroutineScope()

    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        val controller = WindowInsetsControllerCompat(window, view)
        if (state.settings.hideStatusBar) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            controller.hide(WindowInsetsCompat.Type.statusBars())
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            controller.show(WindowInsetsCompat.Type.statusBars())
        }
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    val railLetters = remember(state.isHiddenMode, state.listLayout.letterJumpTargets) {
        buildRailLetters(state.listLayout.letterJumpTargets)
            .filterNot { state.isHiddenMode && isFavoritesRailItem(it) }
    }

    fun selectRailItem(item: Char) {
        selectedRailItem.value = item

        if (isFavoritesRailItem(item)) {
            contentMode = HomeContentMode.Favorites
            scrubbingLetter.value = null
            isScrubbing.value = false

            coroutineScope.launch {
                listState.scrollToItem(
                    index = if (state.listLayout.favorites.isNotEmpty()) {
                        favoritesHeaderIndex(widgetStacks.size)
                    } else {
                        FirstHomeContentIndex
                    },
                    scrollOffset = -categoryPinOffsetPx,
                )
            }
        } else {
            contentMode = HomeContentMode.Apps
            scrubbingLetter.value = item
            isScrubbing.value = true
            onLetterSelected(item)
        }
    }

    fun activateSearch() {
        contentMode = HomeContentMode.Search
        scrubbingLetter.value = null
        isScrubbing.value = false
        onSearchActivated()

        coroutineScope.launch {
            listState.scrollToItem(index = 0)
        }
    }

    fun setHiddenMode(enabled: Boolean) {
        onHiddenModeChanged(enabled)
        contentMode = if (enabled) HomeContentMode.Apps else HomeContentMode.Favorites
        scrubbingLetter.value = null
        isScrubbing.value = false

        coroutineScope.launch {
            listState.scrollToItem(index = 0)
        }
    }

    fun dismissSearch() {
        contentMode = if (state.isHiddenMode) HomeContentMode.Apps else HomeContentMode.Favorites
        scrubbingLetter.value = null
        isScrubbing.value = false
        onSearchDismissed()
    }

    fun launchBestSearchMatch() {
        val app = state.listLayout.apps.firstOrNull() ?: return
        launchApp(context, app)
        dismissSearch()
    }

    if (state.isSearchActive) {
        BackHandler(onBack = ::dismissSearch)
    }

    LaunchedEffect(
        state.isSearchActive,
        state.query,
        state.listLayout.apps,
    ) {
        if (!state.isSearchActive ||
            state.query.trim().length < SEARCH_AUTO_OPEN_MIN_QUERY_LENGTH ||
            state.listLayout.apps.size != 1
        ) {
            return@LaunchedEffect
        }

        launchBestSearchMatch()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent,
        contentColor = Color.White.copy(alpha = 0.92f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            AnimatedContent(
                targetState = contentMode,
                transitionSpec = {
                    homeModeTransition(initialState, targetState)
                },
                label = "homeContentMode",
            ) { mode ->
                AppListPanel(
                    listLayout = state.listLayout,
                    widgetStacks = widgetStacks,
                    scrubbingLetter = scrubbingLetter,
                    isScrubbing = isScrubbing,
                    isHiddenMode = state.isHiddenMode,
                    showFavoritesOnly = mode == HomeContentMode.Favorites,
                    isSearchActive = mode == HomeContentMode.Search,
                    listState = listState,
                    categoryPinOffsetPx = categoryPinOffsetPx,
                    onSearchActivated = ::activateSearch,
                    onHiddenModeChanged = ::setHiddenMode,
                    onToggleFavorite = onToggleFavorite,
                    onHideApp = onHideApp,
                    onUnhideApp = onUnhideApp,
                    onReorderFavorites = onReorderFavorites,
                    onAddWidget = onAddWidget,
                    onAddWidgetToStack = onAddWidgetToStack,
                    onRemoveWidget = onRemoveWidget,
                    onReorderWidgetStacks = onReorderWidgetStacks,
                    settings = state.settings,
                    onHideStatusBarChanged = onHideStatusBarChanged,
                    onHideAppIconsChanged = onHideAppIconsChanged,
                    onFontChanged = onFontChanged,
                    onRestartLauncher = onRestartLauncher,
                    createWidgetView = createWidgetView,
                    getWidgetMinHeightDp = getWidgetMinHeightDp,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            AnimatedVisibility(
                visible = state.isSearchActive,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 },
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                SearchOverlay(
                    query = state.query,
                    onQueryChanged = onQueryChanged,
                    onSearchSubmitted = ::launchBestSearchMatch,
                    onKeyboardDismissed = ::dismissSearch,
                    modifier = Modifier
                        .padding(
                            horizontal = 0.dp,
                            vertical = 0.dp,
                        ),
                )
            }

            AnimatedVisibility(
                visible = !state.isSearchActive && railLetters.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                AzRailPanel(
                    modifier = Modifier
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
                    onDragStateChanged = { dragging ->
                        isRailDragging = dragging
                    },
                )
            }

            if (!state.isSearchActive && !state.isHiddenMode) {
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

            AnimatedVisibility(
                visible = contentMode == HomeContentMode.Apps && !state.isSearchActive && !isRailDragging,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 28.dp, bottom = 40.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier.clickable { activateSearch() },
                ) {
                    Text(
                        text = "\u2315",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    )
                }
            }
        }
    }
}
private const val FirstHomeContentIndex = 1
private const val SEARCH_AUTO_OPEN_MIN_QUERY_LENGTH = 3

private fun favoritesHeaderIndex(widgetCount: Int): Int = widgetCount + 2

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun launchApp(
    context: Context,
    app: LauncherApp,
) {
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
        component = ComponentName(app.packageName, app.activityName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(
            context,
            "Unable to launch ${app.label}",
            Toast.LENGTH_SHORT,
        ).show()
    }
}

private enum class HomeContentMode {
    Favorites,
    Apps,
    Search,
}

private fun homeModeTransition(
    initialState: HomeContentMode,
    targetState: HomeContentMode,
): ContentTransform {
    return if (targetState == HomeContentMode.Search) {
        (fadeIn() + slideInVertically { it / 8 }) togetherWith
            (fadeOut() + slideOutVertically { -it / 8 })
    } else if (initialState == HomeContentMode.Search) {
        (fadeIn() + slideInVertically { -it / 8 }) togetherWith
            (fadeOut() + slideOutVertically { it / 8 })
    } else {
        fadeIn() togetherWith fadeOut()
    }
}
