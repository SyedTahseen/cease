package com.freezer.io

import android.app.ActivityManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

private const val BYTES_PER_GB = 1_000_000_000.0
private const val RAM_REFRESH_MS = 2000L

private data class RamStat(val used: Long, val total: Long) {
    val free: Long get() = total - used
    val percent: Int get() = if (total > 0) (used * 100 / total).toInt() else 0
}

private fun readMeminfo(): RamStat? = try {
    var total = 0L
    var available = 0L
    File("/proc/meminfo").useLines { lines ->
        for (line in lines) {
            when {
                line.startsWith("MemTotal:") -> total = kbOf(line)
                line.startsWith("MemAvailable:") -> available = kbOf(line)
            }
            if (total > 0 && available > 0) break
        }
    }
    if (total > 0 && available > 0) RamStat(used = total - available, total = total) else null
} catch (_: Exception) {
    null
}

private fun kbOf(line: String): Long =
    (line.substringAfter(':').trim().substringBefore(' ').toLongOrNull() ?: 0L) * 1024L

private fun readRam(context: Context): RamStat {
    readMeminfo()?.let { return it }
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val info = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
    return RamStat(used = info.totalMem - info.availMem, total = info.totalMem)
}

private fun gb(bytes: Long) = String.format(Locale.US, "%.1f GB", bytes / BYTES_PER_GB)

@Composable
fun StatsBar(apps: List<AppItem>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = FZ.c
    var ram by remember { mutableStateOf(readRam(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(RAM_REFRESH_MS)
            ram = withContext(Dispatchers.IO) { readRam(context) }
        }
    }

    val frozen = apps.count { it.isFrozen }
    val closed = apps.count { !it.isFrozen && it.isStopped }
    val running = apps.size - frozen - closed

    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "RAM  ${gb(ram.used)} used · ${gb(ram.free)} free",
                color = colors.rowSubtitle,
                fontSize = 12.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Text("${ram.percent}%", color = colors.rowTitle, fontSize = 12.sp, maxLines = 1)
        }
        LinearProgressIndicator(
            progress = { ram.percent / 100f },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(3.dp),
            color = colors.accent,
            trackColor = colors.rowSeparator,
            drawStopIndicator = {},
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            StatChip("Running", running, StatusGreen, Modifier.weight(1f))
            StatChip("Frozen", frozen, StatusAmber, Modifier.weight(1f))
            StatChip("Closed", closed, colors.rowSubtitle, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatChip(label: String, count: Int, dot: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(dot, CircleShape))
        Text(
            "$label $count",
            color = FZ.c.rowSubtitle,
            fontSize = 12.sp,
            maxLines = 1,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}
