package com.cease.io

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsPage(
    title: String,
    onBack: () -> Unit,
    snackbar: SnackbarHostState? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(onBack = onBack)
    Scaffold(
        containerColor = CeaseTheme.colors.settingsBg,
        snackbarHost = { snackbar?.let { SnackbarHost(it) } },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            SettingsHeader(title, onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), content = content)
        }
    }
}

@Composable
private fun SettingText(title: String, subtitle: String?, enabled: Boolean = true) {
    Text(title, color = if (enabled) CeaseTheme.colors.settingsInk else CeaseTheme.colors.settingsDisabledInk, style = Type.Body)
    if (subtitle != null) {
        Text(
            subtitle,
            color = if (enabled) CeaseTheme.colors.settingsSecondary else CeaseTheme.colors.settingsDisabledSub,
            style = Type.Small,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun SettingsHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).background(CeaseTheme.colors.settingsBar).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CeaseTheme.colors.settingsBarInk)
        }
        Text(
            title,
            color = CeaseTheme.colors.settingsBarInk,
            style = Type.PageTitle,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text,
        color = CeaseTheme.colors.accent,
        style = Type.Small,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
internal fun SettingRow(
    title: String,
    subtitle: String? = null,
    divider: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
            SettingText(title, subtitle)
        }
        if (divider) HorizontalDivider(color = CeaseTheme.colors.settingsDivider)
    }
}

@Composable
internal fun DropdownSettingRow(
    title: String,
    value: String,
    enabled: Boolean = true,
    divider: Boolean = true,
    options: List<Pair<String, () -> Unit>>,
) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clickable(enabled = enabled) { open = true }) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
            Column { SettingText(title, value, enabled) }
            CeaseMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { (label, action) ->
                    CeaseMenuItem(text = { Text(label) }, onClick = { open = false; action() })
                }
            }
        }
        if (divider) HorizontalDivider(color = CeaseTheme.colors.settingsDivider)
    }
}

@Composable
internal fun SwitchSettingRow(
    title: String,
    checked: Boolean,
    subtitle: String? = null,
    divider: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth().clickable { onChange(!checked) }) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(vertical = if (subtitle != null) 6.dp else 0.dp)) {
                SettingText(title, subtitle)
            }
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = CeaseTheme.colors.accent,
                    uncheckedThumbColor = CeaseTheme.colors.switchThumbOff,
                    uncheckedTrackColor = CeaseTheme.colors.switchTrackOff,
                    uncheckedBorderColor = CeaseTheme.colors.switchTrackOff,
                ),
            )
        }
        if (divider) HorizontalDivider(color = CeaseTheme.colors.settingsDivider)
    }
}
