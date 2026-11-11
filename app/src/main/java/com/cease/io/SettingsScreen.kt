package com.cease.io

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val version = remember {
        @Suppress("DEPRECATION")
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull().orEmpty()
    }
    var confirmUnfreeze by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) vm.writeBackup(uri) }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) pendingRestore = uri }

    SettingsPage(
        title = "Settings",
        onBack = { vm.closeSettings() },
        snackbar = rememberMessageSnackbar(vm),
    ) {
        SectionLabel("Appearance")
        SettingRow(
            title = "Theme & layout",
            subtitle = "Theme, slider, icon size, app DPI and tabs",
            divider = false,
            onClick = { vm.openThemeLayout() },
        )

        SectionLabel("Freezing")
        SettingRow(
            title = "Auto Freeze",
            subtitle = "Freeze apps automatically: ${vm.autoMode.label}",
            onClick = { vm.openAutoFreeze() },
        )
        DropdownSettingRow(
            title = "Default mode",
            value = vm.defaultMode.label,
            divider = false,
            options = Mode.entries.map { m -> m.label to { vm.updateDefaultMode(m) } },
        )

        if (!vm.inForceStopTab) {
            SectionLabel("Tools")
            SettingRow(
                title = "Unfreeze all",
                subtitle = "Unfreeze every frozen app in your list",
                divider = false,
                onClick = { confirmUnfreeze = true },
            )
        }

        SectionLabel("Backup")
        SettingRow(
            title = "Back up settings",
            subtitle = "Save theme, layout, tabs, apps and all settings to a cease_(date).json file",
            onClick = { backupLauncher.launch(backupFileName()) },
        )
        SettingRow(
            title = "Restore settings",
            subtitle = "Load a Cease backup file",
            divider = false,
            onClick = { restoreLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
        )

        SectionLabel("Root & setup")
        SettingRow(
            title = "Root access",
            subtitle = when (vm.rootOk) {
                null -> "Checking…"
                true -> "Granted. Tap to re-check."
                false -> "Not granted. Allow Cease in Magisk / KernelSU, then tap to re-check."
            },
            onClick = { vm.checkRoot() },
        )
        SettingRow(
            title = "Setup",
            subtitle = "Check root and permissions",
            divider = false,
            onClick = { vm.openSetup() },
        )

        SectionLabel("About")
        SettingRow(title = "Version", subtitle = "Cease $version", divider = false)
    }

    pendingRestore?.let { uri ->
        CeaseConfirmDialog(
            message = "Restore this backup? It will replace your current settings, tabs and app list.",
            confirmLabel = "Restore",
            dismissLabel = "Cancel",
            settingsStyle = true,
            onConfirm = { pendingRestore = null; vm.restoreBackup(uri) },
            onDismiss = { pendingRestore = null },
        )
    }

    if (confirmUnfreeze) {
        UnfreezeAllDialog(vm, settingsStyle = true) { confirmUnfreeze = false }
    }
}
