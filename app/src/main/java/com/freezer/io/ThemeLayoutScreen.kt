package com.freezer.io

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

private val DpiScales = listOf(0.7f, 0.8f, 0.9f, 1f, 1.1f, 1.2f, 1.3f, 1.5f)

private enum class LayoutDialog { THEME, SLIDER, ICON, DPI }

@Composable
fun ThemeLayoutScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val systemDpi = ctx.resources.displayMetrics.densityDpi
    var dialog by remember { mutableStateOf<LayoutDialog?>(null) }

    SettingsPage(title = "Theme & layout", onBack = { vm.closeThemeLayout() }) {
        SectionLabel("Theme")
        SettingRow(
            title = "Theme",
            subtitle = vm.themeMode.label,
            divider = false,
            onClick = { dialog = LayoutDialog.THEME },
        )

        SectionLabel("Layout")
        SettingRow(
            title = "Slider position",
            subtitle = vm.sliderStyle.label,
            onClick = { dialog = LayoutDialog.SLIDER },
        )
        SettingRow(
            title = "Icon size",
            subtitle = vm.iconSize.label,
            onClick = { dialog = LayoutDialog.ICON },
        )
        SettingRow(
            title = "App DPI",
            subtitle = "${(systemDpi * vm.dpiScale).roundToInt()} dpi",
            divider = false,
            onClick = { dialog = LayoutDialog.DPI },
        )

        SectionLabel("Tabs")
        SwitchSettingRow(
            title = "Enable tabs",
            checked = vm.tabsEnabled,
            subtitle = "Show tabs on top of the apps grid. Long press a tab to add or rename tabs.",
            divider = false,
        ) { vm.updateTabsEnabled(it) }
    }

    val close = { dialog = null }
    when (dialog) {
        LayoutDialog.THEME -> ChoiceDialog(
            title = "Set theme",
            options = ThemeMode.entries,
            isSelected = { it == vm.themeMode },
            label = { it.label },
            onPick = { vm.updateThemeMode(it); close() },
            onDismiss = close,
        )
        LayoutDialog.SLIDER -> ChoiceDialog(
            title = "Slider position",
            options = SliderStyle.entries,
            isSelected = { it == vm.sliderStyle },
            label = { it.label },
            onPick = { vm.updateSliderStyle(it); close() },
            onDismiss = close,
        )
        LayoutDialog.ICON -> ChoiceDialog(
            title = "Set icon size",
            options = IconSizeOption.entries,
            isSelected = { it == vm.iconSize },
            label = { it.label },
            onPick = { vm.updateIconSize(it); close() },
            onDismiss = close,
            trailing = { AppIcon(ctx.packageName, it.sizeDp.dp) },
        )
        LayoutDialog.DPI -> ChoiceDialog(
            title = "Set app DPI",
            options = DpiScales,
            isSelected = { abs(vm.dpiScale - it) < 0.01f },
            label = { scale ->
                val dpi = (systemDpi * scale).roundToInt()
                if (scale == 1f) "$dpi dpi (default)" else "$dpi dpi"
            },
            onPick = { vm.updateDpiScale(it); close() },
            onDismiss = close,
        )
        null -> Unit
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    isSelected: (T) -> Boolean,
    label: (T) -> String,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
    trailing: @Composable (T) -> Unit = {},
) {
    FreezerDialog(
        onDismiss = onDismiss,
        title = title,
        settingsStyle = true,
        dismissButton = { DialogButton("Cancel", onDismiss) },
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            options.forEach { option ->
                RadioRow(
                    label = label(option),
                    selected = isSelected(option),
                    onClick = { onPick(option) },
                    inkColor = FZ.c.settingsInk,
                    mutedColor = FZ.c.settingsSecondary,
                    trailing = { trailing(option) },
                )
            }
        }
    }
}
