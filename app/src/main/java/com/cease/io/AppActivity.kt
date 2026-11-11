package com.cease.io

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat

class AppActivity : ComponentActivity() {
    private val vm: MainViewModel by lazy { sharedViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm.pageActivityOpen = true
        setContent {
            CeaseTheme(vm) {
                val anyPage = vm.anyPageOpen
                val colors = FZ.c
                LaunchedEffect(vm.showSetup, vm.showAutoFreeze, vm.showThemeLayout, vm.showSettings, colors) {
                    applyBars(vm.showSetup, !vm.showSetup && (vm.showAutoFreeze || vm.showThemeLayout || vm.showSettings), colors)
                }
                LaunchedEffect(anyPage) { if (!anyPage) finish() }

                BackHandler(enabled = vm.showSetup && !vm.setupDone && vm.setupPage == 0) { finishAffinity() }

                when {
                    vm.showSetup -> SetupScreen(vm)
                    vm.showAutoFreeze -> AutoFreezeScreen(vm)
                    vm.showThemeLayout -> ThemeLayoutScreen(vm)
                    vm.showSettings -> SettingsScreen(vm)
                    vm.showPicker -> PickerScreen(vm)
                }
            }
        }
    }

    private fun applyBars(setup: Boolean, settings: Boolean, c: CeaseColors) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        window.statusBarColor = when {
            setup -> SetupBlue.toArgb()
            settings -> c.settingsBg.toArgb()
            else -> c.bar.toArgb()
        }
        controller.isAppearanceLightStatusBars = !setup && !c.isDark
        val pageBg = when {
            setup -> SetupBlue
            settings -> c.settingsBg
            else -> c.row
        }
        window.navigationBarColor = pageBg.toArgb()
        controller.isAppearanceLightNavigationBars = !setup && !c.isDark
        window.setBackgroundDrawable(ColorDrawable(pageBg.toArgb()))
    }

    override fun onResume() {
        super.onResume()
        vm.onActivityResumed()
    }

    override fun onDestroy() {
        if (isFinishing) vm.pageActivityOpen = false
        super.onDestroy()
    }
}
