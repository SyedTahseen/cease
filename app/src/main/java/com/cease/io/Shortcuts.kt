package com.cease.io

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap

const val ACTION_LAUNCH = "com.cease.io.LAUNCH"
const val EXTRA_PKG = "pkg"

fun pinLaunchShortcut(ctx: Context, item: AppItem): Boolean {
    if (!ShortcutManagerCompat.isRequestPinShortcutSupported(ctx)) return false
    val intent = Intent(ctx, MainActivity::class.java)
        .setAction(ACTION_LAUNCH)
        .putExtra(EXTRA_PKG, item.packageName)
    val icon = runCatching {
        IconCompat.createWithBitmap(ctx.packageManager.getApplicationIcon(item.packageName).toBitmap(192, 192))
    }.getOrElse { IconCompat.createWithResource(ctx, android.R.drawable.sym_def_app_icon) }
    val info = ShortcutInfoCompat.Builder(ctx, "launch_${item.packageName}")
        .setShortLabel(item.label)
        .setIcon(icon)
        .setIntent(intent)
        .build()
    return ShortcutManagerCompat.requestPinShortcut(ctx, info, null)
}
