package com.cease.io

import android.content.pm.ApplicationInfo
import androidx.compose.ui.text.TextStyle

const val DEFAULT_TAB = "default"

private const val FLAG_STOPPED = 1 shl 21

internal val ApplicationInfo.isSystemApp: Boolean get() = flags and ApplicationInfo.FLAG_SYSTEM != 0

internal val ApplicationInfo.isStopped: Boolean get() = flags and FLAG_STOPPED != 0

enum class AutoMode(val label: String) {
    SCREEN_OFF("Screen off"),
    HOME("Tap Home button"),
    DOUBLE_HOME("Double-tap Home"),
    NEVER("Never"),
}

enum class Mode(val label: String) {
    FREEZE("Freeze"),
    FORCE_STOP("Force stop"),
}

enum class ThemeMode(val label: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark"),
    SPARK("Spark"),
}

enum class SliderStyle(val label: String) {
    LEFT("Left"),
    RIGHT("Right"),
    CENTER("Center (tap button)"),
}

enum class IconSizeOption(val label: String, val sizeDp: Int, val columns: Int, val labelStyle: TextStyle, val starDp: Int) {
    LARGE("Large", 64, 4, Type.Caption, 14),
    NORMAL("Normal", 56, 4, Type.Caption, 13),
    SMALL("Small", 48, 5, Type.Caption, 12),
    TINY("Tiny", 40, 6, Type.Overline, 11),
}

data class TabItem(val id: String, val name: String, val mode: Mode)

data class AppItem(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val isFrozen: Boolean,
    val updateTime: Long = 0L,
    val tabId: String = DEFAULT_TAB,
    val mode: Mode = Mode.FREEZE,
    val isStopped: Boolean = false,
)
