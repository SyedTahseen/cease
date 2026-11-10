package com.freezer.io

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager

object AutoFreezer {
    private val notificationPkg = Regex("""NotificationRecord\(\S+\s+pkg=([A-Za-z0-9_.]+)""")
    private val resumedPkg = Regex("""ActivityRecord\{\S+ u\d+ ([A-Za-z0-9_.]+)/""")

    fun run(ctx: Context) {
        val prefs = AppPrefs(ctx)
        if (prefs.exceptCharging && isCharging(ctx)) return

        val pm = ctx.packageManager
        val modeOf = prefs.modeResolver()
        val candidates = prefs.listPkgs().filter {
            it != ctx.packageName && needsAction(pm, it, modeOf(it))
        }
        if (candidates.isEmpty()) return

        val skip = mutableSetOf<String>()
        if (prefs.exceptNotifications) {
            skip += scanPackages("dumpsys notification --noredact | grep 'NotificationRecord('", notificationPkg) ?: return
        }
        if (prefs.exceptForeground) {
            skip += scanPackages("dumpsys activity activities | grep -E 'ResumedActivity'", resumedPkg) ?: return
        }
        val targets = candidates.filter { it !in skip }
        if (targets.isEmpty()) return

        RootShell.freezeAndStop(
            toFreeze = targets.filter { modeOf(it) == Mode.FREEZE },
            toStop = targets.filter { modeOf(it) == Mode.FORCE_STOP },
        )
    }

    private fun scanPackages(command: String, pattern: Regex): Set<String>? {
        val result = RootShell.run("$command ; true")
        if (!result.success) return null
        return pattern.findAll(result.output).map { it.groupValues[1] }.toSet()
    }

    private fun needsAction(pm: PackageManager, pkg: String, mode: Mode): Boolean {
        val info = try {
            pm.getApplicationInfo(pkg, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }
        return if (mode == Mode.FREEZE) info.enabled else !info.isStopped
    }

    private fun isCharging(ctx: Context): Boolean {
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return (i?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
    }
}
