package com.freezer.io

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionSheet(vm: MainViewModel, app: AppItem, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val current = vm.items.find { it.packageName == app.packageName } ?: app
    val pkg = current.packageName
    var moving by remember { mutableStateOf(false) }
    val colors = FZ.c

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        contentColor = colors.sheetInk,
        shape = RectangleShape,
        tonalElevation = 0.dp,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.matchParentSize()) {
                Box(Modifier.weight(1f).fillMaxWidth())
                Box(Modifier.weight(1f).fillMaxWidth().background(colors.sheet))
            }
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.Bottom) {
                AppIcon(pkg, 72.dp)
                Text(
                    current.label,
                    color = colors.sheetTitle,
                    fontSize = FZType.headline,
                    modifier = Modifier.padding(start = 20.dp),
                )
            }
        }
        Column(Modifier.background(colors.sheet).navigationBarsPadding().padding(bottom = 24.dp)) {
            SheetRow("Run app", icon = Icons.Default.PlayArrow) {
                onDismiss()
                vm.launchApp(current)
            }
            if (current.mode == Mode.FORCE_STOP) {
                SheetRow("Force stop", custom = { Box(Modifier.size(14.dp).background(colors.sheetIconGlyph)) }) {
                    onDismiss()
                    vm.forceStop(current)
                }
            } else {
                SheetRow(if (current.isFrozen) "Unfreeze" else "Freeze", custom = { SnowflakeIcon(color = colors.sheetIconGlyph) }) {
                    onDismiss()
                    vm.toggleFreeze(current)
                }
            }
            SheetDivider()
            if (vm.tabsEnabled && vm.tabs.size > 1) {
                SheetRow("Move to tab", icon = Icons.AutoMirrored.Filled.ArrowForward) { moving = true }
            }
            SheetRow("Remove from list", icon = Icons.Default.Close) {
                onDismiss()
                vm.removeAndUnfreeze(current)
            }
            SheetDivider()
            SheetRow("Configure in other apps", icon = Icons.Default.Settings) {
                onDismiss()
                ctx.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
            SheetRow("Add to launcher", icon = Icons.Default.Add) {
                onDismiss()
                if (!pinLaunchShortcut(ctx, current)) {
                    Toast.makeText(ctx, "Your launcher doesn't support pinned shortcuts", Toast.LENGTH_SHORT).show()
                }
            }
            SheetRow("Uninstall", icon = Icons.Default.Delete) {
                onDismiss()
                ctx.startActivity(
                    Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    if (moving) {
        FreezerDialog(
            onDismiss = { moving = false },
            title = "Move to tab",
            dismissButton = { DialogButton("Cancel", { moving = false }) },
        ) {
            Column {
                vm.tabs.forEach { tab ->
                    Text(
                        tab.name + if (tab.id == current.tabId) "  ✓" else "",
                        color = colors.menuInk,
                        fontSize = FZType.body,
                        modifier = Modifier.fillMaxWidth().clickable {
                            moving = false
                            vm.moveApp(pkg, tab.id)
                            onDismiss()
                        }.padding(vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetDivider() {
    HorizontalDivider(color = FZ.c.divider, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun SheetRow(
    label: String,
    icon: ImageVector? = null,
    custom: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(FZ.c.sheetIconCircle),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) Icon(icon, contentDescription = null, tint = FZ.c.sheetIconGlyph)
            custom?.invoke()
        }
        Text(label, color = FZ.c.sheetInk, fontSize = FZType.body, modifier = Modifier.padding(start = 24.dp))
    }
}
