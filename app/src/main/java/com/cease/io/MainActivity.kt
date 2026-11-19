package com.cease.io

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by lazy { sharedViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpEdgeToEdge()
        AutoFreezeService.sync(this)
        setContent {
            var panelFull by remember { mutableStateOf(false) }

            val anyPage = vm.anyPageOpen
            LaunchedEffect(anyPage) {
                if (anyPage && !vm.pageActivityOpen) {
                    vm.pageActivityOpen = true
                    startActivity(Intent(this@MainActivity, AppActivity::class.java))
                }
            }

            CeaseTheme(vm) {
                val colors = FZ.c
                LaunchedEffect(panelFull, colors) { applyBars(panelFull, colors) }
                if (!(vm.showSetup && !vm.setupDone)) {
                    SplitPanel(onDismiss = { finish() }, onFullChange = { panelFull = it }) {
                        HomeScreen(vm, onClose = { finish() })
                    }
                }
            }
        }
        handleLaunchIntent(intent)
    }

    private fun applyBars(full: Boolean, c: CeaseColors) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = full && !c.isDark
        controller.isAppearanceLightNavigationBars = !c.isDark
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(i: Intent?) {
        if (i?.action != ACTION_LAUNCH) return
        val pkg = i.getStringExtra(EXTRA_PKG) ?: return
        i.action = null
        vm.launchByPackage(pkg) { ok ->
            if (ok) finish()
            else Toast.makeText(this, "Couldn't launch app", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStop() {
        super.onStop()

        if (!vm.anyPageOpen) vm.closeGridSearch()
    }

    override fun onResume() {
        super.onResume()
        vm.refreshList()
        vm.onActivityResumed()
    }
}
