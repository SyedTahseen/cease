package com.freezer.io

import android.content.Context
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

private const val ICON_PX = 144

object IconCache {
    private val cache = LruCache<String, ImageBitmap>(150)

    fun get(pkg: String): ImageBitmap? = cache.get(pkg)

    fun load(context: Context, pkg: String): ImageBitmap? {
        cache.get(pkg)?.let { return it }
        return runCatching {
            context.packageManager.getApplicationIcon(pkg).toBitmap(ICON_PX, ICON_PX).asImageBitmap()
        }.getOrNull()?.also { cache.put(pkg, it) }
    }
}
