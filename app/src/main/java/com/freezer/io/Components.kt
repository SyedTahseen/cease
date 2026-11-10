package com.freezer.io

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val FreezerShape = RoundedCornerShape(0.5.dp)

@Composable
fun AppIcon(pkg: String, size: Dp, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current.applicationContext
    val icon by produceState<ImageBitmap?>(IconCache.get(pkg), pkg) {
        if (value == null) value = withContext(Dispatchers.IO) { IconCache.load(ctx, pkg) }
    }
    val bmp = icon
    if (bmp != null) {
        Image(bitmap = bmp, contentDescription = null, modifier = modifier.size(size))
    } else {
        Box(modifier.size(size).background(FZ.c.iconPlaceholder, RoundedCornerShape(12.dp)))
    }
}

@Composable
fun rememberMessageSnackbar(vm: MainViewModel): SnackbarHostState {
    val state = remember { SnackbarHostState() }
    LaunchedEffect(vm.message) {
        vm.message?.let { state.showSnackbar(it); vm.message = null }
    }
    return state
}

@Composable
fun FreezerSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onClose: () -> Unit,
    autoFocus: Boolean = false,
) {
    val colors = FZ.c
    val focus = remember { FocusRequester() }
    if (autoFocus) LaunchedEffect(Unit) { focus.requestFocus() }

    Surface(
        shape = RoundedCornerShape(50),
        color = colors.searchField,
        shadowElevation = if (colors.isDark) 0.dp else 3.dp,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    ) {
        Row(Modifier.height(52.dp).padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, contentDescription = null, tint = colors.searchIcon)
            Box(Modifier.weight(1f).padding(start = 12.dp), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(placeholder, color = colors.searchHint, fontSize = FZType.body)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(color = colors.searchInk, fontSize = FZType.body),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }

            IconButton(onClick = { if (value.isNotEmpty()) onValueChange("") else onClose() }) {
                Icon(Icons.Default.Close, contentDescription = "Clear", tint = colors.searchIcon)
            }
        }
    }
}

@Composable
fun FreezerMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        shape = FreezerShape,
        containerColor = FZ.c.menu,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        content = content,
    )
}

@Composable
fun FreezerMenuItem(text: @Composable () -> Unit, onClick: () -> Unit) {
    DropdownMenuItem(
        text = text,
        onClick = onClick,
        colors = MenuDefaults.itemColors(textColor = FZ.c.menuInk),
    )
}

@Composable
fun DialogButton(text: String, onClick: () -> Unit, color: Color = FZ.c.accent) {
    TextButton(
        onClick = onClick,
        shape = FreezerShape,
        colors = ButtonDefaults.textButtonColors(contentColor = color),
    ) { Text(text.uppercase()) }
}

@Composable
fun FreezerDialog(
    onDismiss: () -> Unit,
    title: String? = null,
    settingsStyle: Boolean = false,
    containerColor: Color = if (settingsStyle) FZ.c.settingsDialog else FZ.c.menu,
    inkColor: Color = if (settingsStyle) FZ.c.settingsInk else FZ.c.menuInk,
    confirmButton: @Composable () -> Unit = {},
    dismissButton: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = FreezerShape,
        containerColor = containerColor,
        tonalElevation = 0.dp,
        title = title?.let { { Text(it, color = inkColor) } },
        text = content,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
    )
}

@Composable
fun FreezerConfirmDialog(
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    settingsStyle: Boolean = false,
) {
    FreezerDialog(
        onDismiss = onDismiss,
        settingsStyle = settingsStyle,
        confirmButton = { DialogButton(confirmLabel, onConfirm) },
        dismissButton = { DialogButton(dismissLabel, onDismiss) },
    ) {
        Text(message, color = if (settingsStyle) FZ.c.settingsInk else FZ.c.menuInk, fontSize = FZType.body)
    }
}

@Composable
fun UnfreezeAllDialog(vm: MainViewModel, settingsStyle: Boolean = false, onDismiss: () -> Unit) {
    FreezerConfirmDialog(
        message = "Are you sure you want to unfreeze all apps in your list?",
        confirmLabel = "Yes",
        dismissLabel = "No",
        onConfirm = { onDismiss(); vm.unfreezeAll() },
        onDismiss = onDismiss,
        settingsStyle = settingsStyle,
    )
}

@Composable
fun RadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    inkColor: Color,
    mutedColor: Color,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = FZ.c.accent, unselectedColor = mutedColor),
        )
        Text(label, color = inkColor, fontSize = FZType.body, modifier = Modifier.weight(1f).padding(start = 8.dp))
        trailing()
    }
}

@Composable
fun SnowflakeIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    iconSize: Dp = 24.dp,
) {
    Canvas(modifier.size(iconSize)) {
        val c = center
        val r = size.minDimension / 2f
        val stroke = size.minDimension * 0.085f
        for (i in 0 until 6) {
            rotate(degrees = i * 60f, pivot = c) {
                drawLine(color = color, start = c, end = Offset(c.x, c.y - r), strokeWidth = stroke, cap = StrokeCap.Round)
                val by = c.y - r * 0.58f
                val bl = r * 0.32f
                drawLine(color = color, start = Offset(c.x, by), end = Offset(c.x - bl, by - bl), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(color = color, start = Offset(c.x, by), end = Offset(c.x + bl, by - bl), strokeWidth = stroke, cap = StrokeCap.Round)
            }
        }
    }
}
