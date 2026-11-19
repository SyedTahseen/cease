package com.cease.io

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

class AppActivity : ComponentActivity() {
    private val vm: MainViewModel by lazy { sharedViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpEdgeToEdge()
        vm.pageActivityOpen = true
        setContent {
            CeaseTheme(vm) {
                val anyPage = vm.anyPageOpen
                val colors = CeaseTheme.colors
                val settingsPage = !vm.showSetup && (vm.showAutoFreeze || vm.showThemeLayout || vm.showSettings)
                LaunchedEffect(vm.showSetup, settingsPage, colors) { applyBars(vm.showSetup, settingsPage, colors) }
                LaunchedEffect(anyPage) { if (!anyPage) finish() }

                BackHandler(enabled = vm.showSetup && !vm.setupDone && vm.setupPage == 0) { finishAffinity() }

                val bars = barColors(vm.showSetup, settingsPage, colors)

                Box(Modifier.fillMaxSize().imePadding()) {
                    when {
                        vm.showSetup -> SetupScreen(vm)
                        vm.showAutoFreeze -> AutoFreezeScreen(vm)
                        vm.showThemeLayout -> ThemeLayoutScreen(vm)
                        vm.showSettings -> SettingsScreen(vm)
                        vm.showPicker -> PickerScreen(vm)
                    }
                    SystemBarBackdrop(top = bars.top, bottom = bars.bottom)
                }
            }
        }
    }

    private class BarColors(val top: Color, val bottom: Color)

    private fun barColors(setup: Boolean, settings: Boolean, c: CeaseColors) = when {
        setup -> BarColors(SetupBlue, SetupBlue)
        settings -> BarColors(c.settingsBg, c.settingsBg)
        else -> BarColors(c.bar, c.row)
    }

    private fun applyBars(setup: Boolean, settings: Boolean, c: CeaseColors) {
        val lightBars = !setup && !c.isDark
        setBarIconAppearance(lightStatusBars = lightBars, lightNavigationBars = lightBars)
        window.setBackgroundDrawable(ColorDrawable(barColors(setup, settings, c).bottom.toArgb()))
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
