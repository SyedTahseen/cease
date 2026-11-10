package com.freezer.io

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

private val DelayOptions = listOf(0, 60, 300)

private fun delayLabel(sec: Int) = when (sec) {
    0 -> "Instant"
    60 -> "1 min"
    300 -> "5 min"
    else -> "$sec sec"
}

@Composable
fun AutoFreezeScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    SettingsPage(title = "Auto Freeze", onBack = { vm.closeAutoFreeze() }) {
        SectionLabel("Trigger")
        DropdownSettingRow(
            title = "Auto Freeze when",
            value = vm.autoMode.label,
            options = AutoMode.entries.map { mode ->
                mode.label to {
                    vm.updateAutoMode(mode)
                    if (mode != AutoMode.NEVER && Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            },
        )

        DropdownSettingRow(
            title = "Screen off delay",
            value = delayLabel(vm.screenOffDelaySec),
            enabled = vm.autoMode == AutoMode.SCREEN_OFF,
            divider = false,
            options = DelayOptions.map { sec -> delayLabel(sec) to { vm.updateScreenOffDelay(sec) } },
        )

        SectionLabel("Except for")
        SwitchSettingRow("Charging", vm.exceptCharging) { vm.updateExceptCharging(it) }
        SwitchSettingRow("Apps with notifications", vm.exceptNotifications) { vm.updateExceptNotifications(it) }
        SwitchSettingRow("Apps in foreground", vm.exceptForeground, divider = false) { vm.updateExceptForeground(it) }

        Text(
            "Auto Freeze runs a small background service and freezes the apps in your list using root. " +
                "Tap / Double Tap Home detection depends on your Android version and device.",
            color = FZ.c.settingsSecondary,
            fontSize = FZType.secondary,
            modifier = Modifier.padding(16.dp),
        )
    }
}
