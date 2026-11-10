package com.freezer.io

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun PickerScreen(vm: MainViewModel) {
    BackHandler { vm.closePicker() }

    val shown = vm.pickerApps()
    var sortMenu by remember { mutableStateOf(false) }
    val snackbar = rememberMessageSnackbar(vm)
    val colors = FZ.c

    Scaffold(containerColor = colors.row, snackbarHost = { SnackbarHost(snackbar) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Column(Modifier.fillMaxWidth().background(colors.bar)) {
                FreezerSearchField(
                    value = vm.query,
                    onValueChange = { vm.query = it },
                    placeholder = "Search…",
                    onClose = { vm.closePicker() },
                )

                StatsBar(vm.allApps)

                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    PickerTab("APPS", selected = !vm.showSystem) { vm.selectPickerTab(false) }
                    PickerTab("SYSTEM", selected = vm.showSystem) { vm.selectPickerTab(true) }
                    Box(Modifier.weight(1f))

                    Box(Modifier.padding(bottom = 4.dp)) {
                        IconButton(onClick = { sortMenu = true }) { SortGlyph() }
                        FreezerMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            MenuHeader("Sort by")
                            RadioItem("Name", !vm.sortByUpdate) { vm.sortByUpdate = false }
                            RadioItem("Update time", vm.sortByUpdate) { vm.sortByUpdate = true }
                            MenuHeader("Options")
                            CheckItem("Selected first", vm.sortSelectedFirst) { vm.sortSelectedFirst = !vm.sortSelectedFirst }
                            CheckItem("Only frozen", vm.onlyFrozen) {
                                vm.onlyFrozen = !vm.onlyFrozen
                                if (vm.onlyFrozen) vm.onlyNotFrozen = false
                            }
                            CheckItem("Only not frozen", vm.onlyNotFrozen) {
                                vm.onlyNotFrozen = !vm.onlyNotFrozen
                                if (vm.onlyNotFrozen) vm.onlyFrozen = false
                            }
                            MenuHeader("Others")
                            FreezerMenuItem(
                                text = { Text("Settings") },
                                onClick = { sortMenu = false; vm.openSettings() },
                            )
                        }
                    }
                }
            }

            if (vm.showSystem && vm.showSystemWarning) {
                SystemWarningPanel(vm)
            } else if (vm.allLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(shown, key = { it.packageName }) { app ->
                        val checked = app.packageName in vm.listPkgs
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(if (app.isFrozen) colors.rowFrozen else colors.row)
                                .clickable { vm.togglePick(app) }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box {
                                AppIcon(app.packageName, 50.dp)

                                if (!app.isFrozen && !app.isStopped) {
                                    RunningBadge(Modifier.align(Alignment.BottomEnd))
                                }
                            }
                            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                                Text(
                                    app.label,
                                    color = colors.rowTitle,
                                    fontSize = FZType.body,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    app.packageName,
                                    color = colors.rowSubtitle,
                                    fontSize = FZType.secondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Box(Modifier.width(1.dp).height(40.dp).background(colors.rowSeparator))
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { vm.togglePick(app) },
                                colors = freezerCheckboxColors(),
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RunningBadge(modifier: Modifier = Modifier) {
    Box(modifier.size(8.dp).background(StatusGreen, CircleShape))
}

@Composable
private fun SystemWarningPanel(vm: MainViewModel) {
    val colors = FZ.c
    var dontShow by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Info, contentDescription = null, tint = colors.rowSubtitle, modifier = Modifier.size(72.dp))
        Text(
            "Freeze system apps may break your devices. Do at your own risk.",
            color = colors.rowSubtitle,
            fontSize = FZType.title,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp),
        )
        Row(
            Modifier.padding(top = 8.dp).clickable { dontShow = !dontShow },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = dontShow, onCheckedChange = { dontShow = it }, colors = freezerCheckboxColors())
            Text("Don't show again", color = colors.rowTitle, fontSize = FZType.body)
        }
        DialogButton("Got it", { vm.dismissSystemWarning(dontShow) })
    }
}

@Composable
private fun MenuHeader(title: String) {
    Text(title, color = FZ.c.accent, fontSize = FZType.secondary, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp))
}

@Composable
private fun RadioItem(text: String, selected: Boolean, onClick: () -> Unit) {
    FreezerMenuItem(
        text = { Row(Modifier.width(200.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f)); RadioButton(selected = selected, onClick = null)
        } },
        onClick = onClick,
    )
}

@Composable
private fun CheckItem(text: String, checked: Boolean, onClick: () -> Unit) {
    FreezerMenuItem(
        text = { Row(Modifier.width(200.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f)); Checkbox(checked = checked, onCheckedChange = null)
        } },
        onClick = onClick,
    )
}

@Composable
private fun PickerTab(title: String, selected: Boolean, onClick: () -> Unit) {
    val colors = FZ.c
    Column(Modifier.clickable(onClick = onClick).padding(end = 24.dp)) {
        Column(Modifier.width(IntrinsicSize.Max)) {
            Text(
                title,
                fontSize = FZType.body,
                color = if (selected) colors.pickerTabSelected else colors.pickerTabIdle,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(if (selected) colors.tabAccent else Color.Transparent),
            )
        }
    }
}

@Composable
private fun SortGlyph() {
    val ink = FZ.c.barIcon
    Column(verticalArrangement = Arrangement.spacedBy(5.dp), horizontalAlignment = Alignment.Start) {
        Box(Modifier.width(24.dp).height(2.dp).background(ink))
        Box(Modifier.width(17.dp).height(2.dp).background(ink))
        Box(Modifier.width(10.dp).height(2.dp).background(ink))
    }
}
