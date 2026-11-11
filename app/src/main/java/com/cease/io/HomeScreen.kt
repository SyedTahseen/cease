package com.cease.io

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(vm: MainViewModel, onClose: () -> Unit) {
    val snackbar = rememberMessageSnackbar(vm)
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf<AppItem?>(null) }
    var confirmUnfreeze by remember { mutableStateOf(false) }

    BackHandler(enabled = vm.gridSearching) { vm.closeGridSearch() }
    LaunchedEffect(vm.gridQuery, vm.gridSearching, vm.items, vm.tabs) {
        if (vm.gridSearching && vm.gridQuery.isNotBlank()) {
            delay(150)
            vm.revealSearchTab()
        }
    }

    val rootOk = vm.rootOk == true
    val label = if (vm.rootOk == false) "Root access required" else "APPS"

    Scaffold(
        containerColor = FZ.c.panel,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            SlideToAct(
                label = label,
                enabled = rootOk && !vm.working,
                style = vm.sliderStyle,
                onLabelClick = { vm.openPicker() },
                onComplete = {
                    if (vm.slideAction()) {
                        vm.closeGridSearch()
                        scope.launch {
                            if (vm.sliderStyle == SliderStyle.CENTER) delay(900)
                            onClose()
                        }
                    }
                },
                trailing = {
                    IconButton(onClick = { if (vm.gridSearching) vm.closeGridSearch() else vm.openGridSearch() }) {
                        Icon(Icons.Default.Search, contentDescription = "Search apps in grid", tint = FZ.c.barIcon)
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = FZ.c.barIcon)
                        }
                        CeaseMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            if (!vm.inForceStopTab) {
                                CeaseMenuItem(
                                    text = { Text("Unfreeze all") },
                                    onClick = { menuOpen = false; confirmUnfreeze = true },
                                )
                            }
                            CeaseMenuItem(
                                text = { Text("Clear list") },
                                onClick = { menuOpen = false; vm.clearList() },
                            )
                            CeaseMenuItem(
                                text = { Text("Settings") },
                                onClick = { menuOpen = false; vm.openSettings() },
                            )
                        }
                    }
                },
            )
        },
    ) { pad ->
        val centerStyle = vm.sliderStyle == SliderStyle.CENTER

        val bottomPad = if (centerStyle) (pad.calculateBottomPadding() - CenterButtonRise).coerceAtLeast(0.dp) else pad.calculateBottomPadding()
        Column(
            Modifier
                .padding(top = pad.calculateTopPadding(), bottom = bottomPad)
                .fillMaxSize(),
        ) {
            if (vm.working) LinearProgressIndicator(Modifier.fillMaxWidth(), color = FZ.c.accent, trackColor = FZ.c.bar)

            if (vm.gridSearching) {
                CeaseSearchField(
                    value = vm.gridQuery,
                    onValueChange = { vm.gridQuery = it },
                    placeholder = "Search in grid…",
                    onClose = { vm.closeGridSearch() },
                    autoFocus = true,
                )
            }

            if (vm.tabsEnabled) TabBar(vm)

            val pageTabs = if (vm.tabsEnabled) vm.tabs.map { it.id } else listOf(DEFAULT_TAB)
            val pagerState = rememberPagerState(
                initialPage = pageTabs.indexOf(vm.currentTabId).coerceAtLeast(0),
                pageCount = { pageTabs.size },
            )

            val programmatic = remember { booleanArrayOf(false) }

            LaunchedEffect(vm.currentTabId, pageTabs.size) {
                val target = pageTabs.indexOf(vm.currentTabId)
                if (target >= 0 && target != pagerState.currentPage && !pagerState.isScrollInProgress) {
                    programmatic[0] = true
                    try { pagerState.animateScrollToPage(target) } finally { programmatic[0] = false }
                }
            }

            LaunchedEffect(pagerState, pageTabs) {
                snapshotFlow { pagerState.currentPage }.collect { p ->
                    if (vm.tabsEnabled && !programmatic[0]) {
                        pageTabs.getOrNull(p)?.let { if (it != vm.currentTabId) vm.selectGridTab(it) }
                    }
                }
            }

            if (!vm.itemsLoaded) {
                Box(Modifier.fillMaxSize())
            } else HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                key = { pageTabs.getOrNull(it) ?: it },
            ) { page ->
                val tabId = pageTabs.getOrNull(page) ?: DEFAULT_TAB
                val tabItems = vm.itemsForTab(tabId)
                val shown = vm.gridItemsFor(tabId)
                if (tabItems.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { vm.openPicker() }
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (vm.items.isEmpty()) {
                                "Tap anywhere on this screen or the bar below to choose the apps you want to freeze."
                            } else {
                                "No apps in this tab. Tap anywhere on this screen or the bar below to add apps."
                            },
                            color = FZ.c.emptyHint,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else if (shown.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No matching apps", color = FZ.c.emptyHint, textAlign = TextAlign.Center)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(vm.iconSize.columns),
                        contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp + if (centerStyle) CenterButtonRise else 0.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(shown, key = { it.packageName }) { app ->
                            AppTile(
                                app = app,
                                iconSize = vm.iconSize,
                                onClick = { vm.launchApp(app) },
                                onLongClick = { removing = app },
                            )
                        }
                    }
                }
            }
        }
    }

    removing?.let { app ->
        AppActionSheet(vm, app) { removing = null }
    }

    if (confirmUnfreeze) {
        UnfreezeAllDialog(vm) { confirmUnfreeze = false }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppTile(app: AppItem, iconSize: IconSizeOption, onClick: () -> Unit, onLongClick: () -> Unit) {
    val colors = FZ.c
    Column(
        Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(app.packageName, iconSize.sizeDp.dp, Modifier.alpha(if (app.isStopped) 0.35f else 1f))
        Row(
            Modifier.padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (app.isFrozen) {
                CeaseStarIcon(color = colors.tabAccent, iconSize = (iconSize.labelSp + 1).dp)
                Spacer(Modifier.width(3.dp))
            }
            Text(
                text = app.label,
                color = colors.appLabel,
                fontSize = iconSize.labelSp.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}
