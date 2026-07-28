package com.what386.waterfall.ui.home

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.what386.waterfall.R
import com.what386.waterfall.data.HomeRowNavigationMode
import com.what386.waterfall.data.LauncherFont
import com.what386.waterfall.domain.LauncherInteractor
import com.what386.waterfall.domain.bestSearchMatch
import com.what386.waterfall.ui.home.applist.AppListPanel
import com.what386.waterfall.ui.home.azrail.AzRailPanel
import com.what386.waterfall.ui.home.azrail.buildRailLetters
import com.what386.waterfall.ui.home.azrail.isFavoritesRailItem
import com.what386.waterfall.ui.model.LauncherApp
import com.what386.waterfall.ui.theme.LauncherTheme
import com.what386.waterfall.widgets.LauncherWidgetController
import com.what386.waterfall.widgets.WidgetStack
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun LauncherHomeRoute(
    interactor: LauncherInteractor,
    widgetController: LauncherWidgetController,
    homeIntentPressCount: StateFlow<Int>,
) {
    val vm: LauncherHomeViewModel =
        viewModel(
            factory = LauncherHomeViewModelFactory(interactor),
        )
    val state by vm.uiState.collectAsStateWithLifecycle()
    val widgetStacks by widgetController.widgetStacks.collectAsStateWithLifecycle()
    val appListState = rememberLazyListState()
    val favoritesListState = rememberLazyListState()
    val context = LocalContext.current

    LauncherTheme(font = state.settings.font) {
        LauncherHomeScreen(
            state = state,
            widgetStacks = widgetStacks,
            homeIntentPressCount = homeIntentPressCount.collectAsStateWithLifecycle().value,
            appListState = appListState,
            favoritesListState = favoritesListState,
            jumpToTarget = vm.jumpToTarget,
            onQueryChanged = vm::onQueryChanged,
            onSearchActivated = vm::onSearchActivated,
            onAppListActivated = vm::onAppListActivated,
            onFavoritesActivated = vm::onFavoritesActivated,
            onSearchDismissed = vm::onSearchDismissed,
            onHiddenModeChanged = vm::onHiddenModeChanged,
            onToggleFavorite = vm::onToggleFavorite,
            onHideApp = vm::onHideApp,
            onUnhideApp = vm::onUnhideApp,
            onReorderFavorites = vm::onFavoriteOrderChanged,
            onHideStatusBarChanged = vm::onHideStatusBarChanged,
            onHideAppIconsChanged = vm::onHideAppIconsChanged,
            onHideSearchButtonChanged = vm::onHideSearchButtonChanged,
            onCleanHomeScreenChanged = vm::onCleanHomeScreenChanged,
            onHomeRowNavigationModeChanged = vm::onHomeRowNavigationModeChanged,
            onFontChanged = vm::onFontChanged,
            onResetSettings = vm::onResetSettings,
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
    homeIntentPressCount: Int,
    appListState: androidx.compose.foundation.lazy.LazyListState,
    favoritesListState: androidx.compose.foundation.lazy.LazyListState,
    jumpToTarget: kotlinx.coroutines.flow.Flow<Int>,
    onQueryChanged: (String) -> Unit,
    onSearchActivated: () -> Unit,
    onAppListActivated: () -> Unit,
    onFavoritesActivated: () -> Unit,
    onSearchDismissed: () -> Unit,
    onHiddenModeChanged: (Boolean) -> Unit,
    onToggleFavorite: (com.what386.waterfall.ui.model.LauncherApp) -> Unit,
    onHideApp: (com.what386.waterfall.ui.model.LauncherApp) -> Unit,
    onUnhideApp: (com.what386.waterfall.ui.model.LauncherApp) -> Unit,
    onReorderFavorites: (List<com.what386.waterfall.ui.model.LauncherApp>) -> Unit,
    onHideStatusBarChanged: (Boolean) -> Unit,
    onHideAppIconsChanged: (Boolean) -> Unit,
    onHideSearchButtonChanged: (Boolean) -> Unit,
    onCleanHomeScreenChanged: (Boolean) -> Unit,
    onHomeRowNavigationModeChanged: (HomeRowNavigationMode) -> Unit,
    onFontChanged: (LauncherFont) -> Unit,
    onResetSettings: () -> Unit,
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
    val resources = LocalResources.current
    val view = LocalView.current
    val density = LocalDensity.current
    var isRailDragging by remember { mutableStateOf(false) }
    val searchAppsLabel = stringResource(R.string.search_apps)
    val undoLabel = stringResource(R.string.undo)
    val snackbarHostState = remember { SnackbarHostState() }

    var lastHandledHomeIntentPressCount by remember { mutableIntStateOf(homeIntentPressCount) }

    val coroutineScope = rememberCoroutineScope()

    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        val controller = WindowInsetsControllerCompat(window, view)
        if (state.settings.hideStatusBar) {
            controller.hide(WindowInsetsCompat.Type.statusBars())
        } else {
            controller.show(WindowInsetsCompat.Type.statusBars())
        }
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    val railLetters =
        remember(state.isHiddenMode, state.listLayout.letterJumpTargets) {
            buildRailLetters(state.listLayout.letterJumpTargets)
                .filterNot { state.isHiddenMode && isFavoritesRailItem(it) }
        }

    fun selectRailItem(
        item: Char,
        categoryPinOffsetPx: Int,
    ) {
        selectedRailItem.value = item

        if (isFavoritesRailItem(item)) {
            onFavoritesActivated()
            scrubbingLetter.value = null
            isScrubbing.value = false

            coroutineScope.launch {
                favoritesListState.scrollToItem(
                    index =
                        if (state.listLayout.favorites.isNotEmpty()) {
                            favoritesHeaderIndex(widgetStacks.size)
                        } else {
                            FirstHomeContentIndex
                        },
                    scrollOffset = -categoryPinOffsetPx,
                )
            }
        } else {
            scrubbingLetter.value = item
            isScrubbing.value = true
            onLetterSelected(item)
        }
    }

    fun activateSearch() {
        scrubbingLetter.value = null
        isScrubbing.value = false
        onSearchActivated()

        coroutineScope.launch {
            appListState.scrollToItem(index = 0)
        }
    }

    fun activateAppList() {
        scrubbingLetter.value = null
        isScrubbing.value = false
        onAppListActivated()

        coroutineScope.launch {
            appListState.scrollToItem(index = 0)
        }
    }

    fun setHiddenMode(enabled: Boolean) {
        onHiddenModeChanged(enabled)
        scrubbingLetter.value = null
        isScrubbing.value = false

        coroutineScope.launch {
            if (enabled) {
                appListState.scrollToItem(index = 0)
            } else {
                favoritesListState.scrollToItem(index = 0)
            }
        }
    }

    fun dismissSearch() {
        scrubbingLetter.value = null
        isScrubbing.value = false
        onSearchDismissed()
    }

    fun returnToFavoritesMenu(categoryPinOffsetPx: Int) {
        onFavoritesActivated()
        selectedRailItem.value = buildRailLetters(state.listLayout.letterJumpTargets).first()
        scrubbingLetter.value = null
        isScrubbing.value = false

        coroutineScope.launch {
            favoritesListState.scrollToItem(
                index =
                    if (state.listLayout.favorites.isNotEmpty()) {
                        favoritesHeaderIndex(widgetStacks.size)
                    } else {
                        FirstHomeContentIndex
                    },
                scrollOffset = -categoryPinOffsetPx,
            )
        }
    }

    fun launchBestSearchMatch(submittedQuery: String = state.query) {
        val app =
            bestSearchMatch(
                apps = state.listLayout.apps,
                rawQuery = submittedQuery,
            ) ?: return
        launchApp(context, app)
        dismissSearch()
    }

    fun toggleFavoriteWithUndo(app: LauncherApp) {
        val wasFavorite = app.isFavorite
        onToggleFavorite(app)
        coroutineScope.launch {
            val message =
                resources.getString(
                    if (wasFavorite) R.string.favorite_removed else R.string.favorite_added,
                    app.label,
                )
            if (snackbarHostState.showSnackbar(message, undoLabel) == SnackbarResult.ActionPerformed) {
                onToggleFavorite(app)
            }
        }
    }

    fun hideWithUndo(app: LauncherApp) {
        onHideApp(app)
        coroutineScope.launch {
            val message = resources.getString(R.string.app_hidden, app.label)
            if (snackbarHostState.showSnackbar(message, undoLabel) == SnackbarResult.ActionPerformed) {
                onUnhideApp(app)
            }
        }
    }

    BackHandler(enabled = state.isSearchActive, onBack = ::dismissSearch)
    BackHandler(enabled = state.isHiddenMode) { setHiddenMode(false) }
    BackHandler(
        enabled = state.contentMode == HomeContentMode.Apps && !state.isHiddenMode,
        onBack = onFavoritesActivated,
    )

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

        launchBestSearchMatch(state.query)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent,
        contentColor = Color.White.copy(alpha = 0.92f),
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
        ) {
            val statusBarTopDp =
                with(density) {
                    WindowInsets.statusBars
                        .getTop(this)
                        .toDp()
                        .value
                }
            val navigationBarBottomDp =
                with(density) {
                    WindowInsets.navigationBars
                        .getBottom(this)
                        .toDp()
                        .value
                }
            val layoutMetrics =
                remember(
                    maxWidth,
                    maxHeight,
                    statusBarTopDp,
                    navigationBarBottomDp,
                ) {
                    calculateHomeLayoutMetrics(
                        screenWidthDp = maxWidth.value,
                        screenHeightDp = maxHeight.value,
                        statusBarTopDp = statusBarTopDp,
                        navigationBarBottomDp = navigationBarBottomDp,
                    )
                }
            val categoryPinOffsetPx =
                with(density) {
                    layoutMetrics.categoryPinOffsetDp.dp.toPx()
                }.toInt()
            val searchButtonRailYOffsetDp =
                run {
                    val railHalfHeightDp = maxHeight.value * layoutMetrics.railHeightFraction / 2f
                    val buttonHalfHeightDp = layoutMetrics.searchButtonSizeDp / 2f
                    layoutMetrics.railYOffsetDp +
                        railHalfHeightDp +
                        layoutMetrics.searchButtonRailGapDp +
                        buttonHalfHeightDp
                }

            CompositionLocalProvider(LocalHomeLayoutMetrics provides layoutMetrics) {
                LaunchedEffect(homeIntentPressCount) {
                    if (homeIntentPressCount > lastHandledHomeIntentPressCount) {
                        lastHandledHomeIntentPressCount = homeIntentPressCount
                        returnToFavoritesMenu(categoryPinOffsetPx)
                    }
                }

                LaunchedEffect(categoryPinOffsetPx, appListState, jumpToTarget) {
                    jumpToTarget.collectLatest { targetIndex ->
                        // Negative offset pins the selected category below the top edge.
                        appListState.scrollToItem(
                            index = targetIndex,
                            scrollOffset = -categoryPinOffsetPx,
                        )
                    }
                }

                AnimatedContent(
                    targetState = state.contentMode,
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
                        highlightedAppComponentId =
                            bestSearchMatch(
                                apps = state.listLayout.apps,
                                rawQuery = state.query,
                            )?.componentId,
                        listState =
                            if (mode == HomeContentMode.Favorites) {
                                favoritesListState
                            } else {
                                appListState
                            },
                        categoryPinOffsetPx = categoryPinOffsetPx,
                        layoutMetrics = layoutMetrics,
                        onSearchActivated = ::activateSearch,
                        onAppListActivated = ::activateAppList,
                        onHiddenModeChanged = ::setHiddenMode,
                        onToggleFavorite = ::toggleFavoriteWithUndo,
                        onHideApp = ::hideWithUndo,
                        onUnhideApp = onUnhideApp,
                        onReorderFavorites = onReorderFavorites,
                        onAddWidget = onAddWidget,
                        onAddWidgetToStack = onAddWidgetToStack,
                        onRemoveWidget = onRemoveWidget,
                        onReorderWidgetStacks = onReorderWidgetStacks,
                        settings = state.settings,
                        onHideStatusBarChanged = onHideStatusBarChanged,
                        onHideAppIconsChanged = onHideAppIconsChanged,
                        onHideSearchButtonChanged = onHideSearchButtonChanged,
                        onCleanHomeScreenChanged = onCleanHomeScreenChanged,
                        onHomeRowNavigationModeChanged = onHomeRowNavigationModeChanged,
                        onFontChanged = onFontChanged,
                        onResetSettings = onResetSettings,
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
                        onKeyboardDismissed = ::dismissSearch,
                        horizontalPaddingDp = layoutMetrics.searchFieldHorizontalPaddingDp,
                        modifier =
                            Modifier
                                .padding(
                                    horizontal = 0.dp,
                                    vertical = 0.dp,
                                ),
                    )
                }

                val isCleanFavoritesHome =
                    state.contentMode == HomeContentMode.Favorites && state.settings.cleanHomeScreen

                AnimatedVisibility(
                    visible =
                        !state.isSearchActive &&
                            railLetters.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    AzRailPanel(
                        modifier =
                            Modifier
                                .offset(y = layoutMetrics.railYOffsetDp.dp)
                                .fillMaxHeight(layoutMetrics.railHeightFraction)
                                .padding(end = layoutMetrics.railEndPaddingDp.dp)
                                .width(layoutMetrics.railWidthDp.dp)
                                .graphicsLayer {
                                    alpha = if (!isCleanFavoritesHome || isRailDragging) 1f else 0f
                                },
                        letters = railLetters,
                        onLetterSelected = {},
                        onScrubStart = { item -> selectRailItem(item, categoryPinOffsetPx) },
                        onScrubMove = { item -> selectRailItem(item, categoryPinOffsetPx) },
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

                if (!state.isSearchActive &&
                    !state.isHiddenMode &&
                    !(state.contentMode == HomeContentMode.Favorites && state.settings.cleanHomeScreen)
                ) {
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.CenterEnd)
                                .offset(y = layoutMetrics.homeRailHitYOffsetDp.dp)
                                .padding(end = layoutMetrics.railEndPaddingDp.dp)
                                .width(layoutMetrics.railWidthDp.dp)
                                .fillMaxHeight(layoutMetrics.homeRailHitHeightFraction)
                                .clickable { selectRailItem(railLetters.first(), categoryPinOffsetPx) },
                    )
                }

                AnimatedVisibility(
                    visible =
                        state.contentMode == HomeContentMode.Apps &&
                            !state.isSearchActive &&
                            !isRailDragging &&
                            !state.settings.hideSearchButton,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .offset(y = searchButtonRailYOffsetDp.dp)
                            .padding(
                                end = layoutMetrics.searchButtonEdgePaddingDp.dp,
                            ),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier =
                            Modifier
                                .size(layoutMetrics.searchButtonSizeDp.dp)
                                .semantics {
                                    contentDescription = searchAppsLabel
                                    role = Role.Button
                                }.clickable { activateSearch() },
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Text(
                                text = "\u2315",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.navigationBars),
                )
            }
        }
    }
}

private const val FirstHomeContentIndex = 1
private const val SEARCH_AUTO_OPEN_MIN_QUERY_LENGTH = 2

private fun favoritesHeaderIndex(widgetCount: Int): Int = widgetCount + 2

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun launchApp(
    context: Context,
    app: LauncherApp,
) {
    val intent =
        Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(app.packageName, app.activityName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast
            .makeText(
                context,
                context.getString(R.string.unable_to_launch, app.label),
                Toast.LENGTH_SHORT,
            ).show()
    }
}

private fun homeModeTransition(
    initialState: HomeContentMode,
    targetState: HomeContentMode,
): ContentTransform =
    if (targetState == HomeContentMode.Search) {
        (fadeIn() + slideInVertically { it / 8 }) togetherWith
            (fadeOut() + slideOutVertically { -it / 8 })
    } else if (initialState == HomeContentMode.Search) {
        (fadeIn() + slideInVertically { -it / 8 }) togetherWith
            (fadeOut() + slideOutVertically { it / 8 })
    } else {
        fadeIn() togetherWith fadeOut()
    }
