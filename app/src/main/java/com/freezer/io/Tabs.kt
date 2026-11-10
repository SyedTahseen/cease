package com.freezer.io

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private class TabDialog(val editId: String?)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabBar(vm: MainViewModel) {
    val colors = FZ.c
    var menuFor by remember { mutableStateOf<String?>(null) }
    var dialog by remember { mutableStateOf<TabDialog?>(null) }
    var deleting by remember { mutableStateOf<TabItem?>(null) }

    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 8.dp, top = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        vm.tabs.forEach { tab ->
            val selected = tab.id == vm.currentTabId
            val textColor by animateColorAsState(if (selected) colors.tabAccent else colors.tabIdle, tween(180), label = "tabText")
            val barAlpha by animateFloatAsState(if (selected) 1f else 0f, tween(180), label = "tabBar")
            val bringIntoView = remember { BringIntoViewRequester() }
            LaunchedEffect(selected) { if (selected) bringIntoView.bringIntoView() }
            Box(Modifier.bringIntoViewRequester(bringIntoView)) {
                Column(
                    Modifier
                        .combinedClickable(
                            onClick = { if (selected) menuFor = tab.id else vm.selectGridTab(tab.id) },
                            onLongClick = { vm.selectGridTab(tab.id); menuFor = tab.id },
                        )
                        .drawBehind {
                            val h = 2.dp.toPx()
                            drawRect(
                                color = colors.tabAccent.copy(alpha = barAlpha),
                                topLeft = Offset(0f, size.height - h),
                                size = Size(size.width, h),
                            )
                        }
                        .padding(horizontal = 12.dp),
                ) {
                    Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            tab.name.uppercase(),
                            color = textColor,
                            fontSize = FZType.body,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = if (selected) "Tab menu" else null,
                            tint = colors.tabAccent,
                            modifier = Modifier.size(22.dp).alpha(barAlpha),
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                }

                FreezerMenu(expanded = menuFor == tab.id, onDismissRequest = { menuFor = null }) {
                    if (tab.mode == Mode.FREEZE) {
                        FreezerMenuItem(text = { Text("Freeze") }, onClick = { menuFor = null; vm.freezeTab(tab.id) })
                        FreezerMenuItem(text = { Text("Unfreeze") }, onClick = { menuFor = null; vm.unfreezeTab(tab.id) })
                    } else {
                        FreezerMenuItem(text = { Text("Force stop") }, onClick = { menuFor = null; vm.freezeTab(tab.id) })
                    }
                    HorizontalDivider(color = colors.divider)
                    Row(
                        Modifier.width(220.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("‹", color = colors.menuInk, fontSize = FZType.display,
                            modifier = Modifier.clickable { vm.moveTab(tab.id, -1) }.padding(horizontal = 16.dp, vertical = 6.dp))
                        Icon(Icons.Default.Edit, contentDescription = "Rename", tint = colors.menuInk,
                            modifier = Modifier.clickable { menuFor = null; dialog = TabDialog(tab.id) }.padding(12.dp))
                        Text("›", color = colors.menuInk, fontSize = FZType.display,
                            modifier = Modifier.clickable { vm.moveTab(tab.id, 1) }.padding(horizontal = 16.dp, vertical = 6.dp))
                    }
                    if (tab.id != DEFAULT_TAB) {
                        FreezerMenuItem(text = { Text("Delete") }, onClick = { menuFor = null; deleting = tab })
                    }
                    FreezerMenuItem(text = { Text("Add new tab") }, onClick = { menuFor = null; dialog = TabDialog(null) })
                }
            }
        }
    }

    dialog?.let { d -> TabDialogView(vm, d.editId) { dialog = null } }

    deleting?.let { tab ->
        FreezerConfirmDialog(
            message = "Delete tab \"${tab.name}\"? Its apps move to the default tab.",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            onConfirm = { deleting = null; vm.deleteTab(tab.id) },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun TabDialogView(vm: MainViewModel, editId: String?, onDismiss: () -> Unit) {
    val colors = FZ.c
    val existing = vm.tabs.firstOrNull { it.id == editId }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var mode by remember { mutableStateOf(vm.defaultMode) }

    FreezerDialog(
        onDismiss = onDismiss,
        title = if (existing == null) "New tab" else "Rename tab",
        confirmButton = {
            DialogButton(if (existing == null) "Create" else "OK", {
                if (existing == null) vm.addTab(name, mode) else vm.renameTab(existing.id, name)
                onDismiss()
            })
        },
        dismissButton = {
            Row {
                if (existing != null && existing.id != DEFAULT_TAB) {
                    DialogButton("Delete", { vm.deleteTab(existing.id); onDismiss() }, color = StatusRed)
                }
                DialogButton("Cancel", onDismiss)
            }
        },
    ) {
        Column {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Tab name") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.menuInk,
                    unfocusedTextColor = colors.menuInk,
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.menuMuted.copy(alpha = 0.6f),
                    focusedLabelColor = colors.accent,
                    unfocusedLabelColor = colors.menuMuted,
                    cursorColor = colors.accent,
                ),
            )
            Spacer(Modifier.height(12.dp))
            if (existing == null) {
                Text("This tab will", color = colors.menuMuted, fontSize = FZType.secondary)
                Mode.entries.forEach { m ->
                    RadioRow(
                        label = if (m == Mode.FREEZE) "Freeze apps" else "Force stop apps",
                        selected = mode == m,
                        onClick = { mode = m },
                        inkColor = colors.menuInk,
                        mutedColor = colors.menuMuted,
                    )
                }
            } else {
                Text("Mode: ${existing.mode.label}", color = colors.menuMuted, fontSize = FZType.secondary)
            }
        }
    }
}
