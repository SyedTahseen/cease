package com.cease.io

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp

enum class ThemeMode(val label: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark"),
    SPARK("Spark"),
}

val StatusGreen = Color(0xFF4CAF50)
val StatusAmber = Color(0xFFFFC107)
val StatusRed = Color(0xFFF44336)

object FZType {
    val display = 28.sp
    val headline = 20.sp
    val title = 18.sp
    val body = 16.sp
    val secondary = 14.sp
}

private val CeaseTypography = Typography(
    headlineSmall = TextStyle(fontSize = FZType.headline),
    bodyLarge = TextStyle(fontSize = FZType.body),
    bodyMedium = TextStyle(fontSize = FZType.secondary),
    labelLarge = TextStyle(fontSize = FZType.secondary, fontWeight = FontWeight.Medium),
)

@Immutable
class CeaseColors(
    val isDark: Boolean,
    val accent: Color,
    val tabAccent: Color,
    val thumb: Color,
    val thumbOff: Color,
    val track: Color,
    val panel: Color,
    val bar: Color,
    val dock: Color,
    val barInk: Color,
    val barIcon: Color,
    val tabIdle: Color,
    val appLabel: Color,
    val emptyHint: Color,
    val iconPlaceholder: Color,
    val searchField: Color,
    val searchIcon: Color,
    val searchHint: Color,
    val searchInk: Color,
    val pickerTabSelected: Color,
    val pickerTabIdle: Color,
    val row: Color,
    val rowFrozen: Color,
    val rowTitle: Color,
    val rowSubtitle: Color,
    val rowSeparator: Color,
    val checkBorder: Color,
    val checkFill: Color,
    val checkMark: Color,
    val menu: Color,
    val menuInk: Color,
    val divider: Color,
    val menuMuted: Color,
    val sheet: Color,
    val sheetInk: Color,
    val sheetTitle: Color,
    val sheetIconCircle: Color,
    val sheetIconGlyph: Color,
    val settingsBg: Color,
    val settingsBar: Color,
    val settingsBarInk: Color,
    val settingsInk: Color,
    val settingsSecondary: Color,
    val settingsDivider: Color,
    val settingsDialog: Color,
    val settingsDisabledInk: Color,
    val settingsDisabledSub: Color,
    val switchThumbOff: Color,
    val switchTrackOff: Color,
)

private val DarkCeaseColors = CeaseColors(
    isDark = true,
    accent = Color(0xFF1E88E5),
    tabAccent = Color(0xFF0056CD),
    thumb = Color(0xFF016EFF),
    thumbOff = Color(0xFF3B4A66),
    track = Color(0xFF002969),
    panel = Color(0xFF00163F),
    bar = Color(0xFF002969),
    dock = Color(0xFF000F2E),
    barInk = Color(0xFFB1C0D5),
    barIcon = Color.White,
    tabIdle = Color(0xFFB2BAC7),
    appLabel = Color(0xFFB2BAC7),
    emptyHint = Color(0xFF8794AD),
    iconPlaceholder = Color(0xFF1E345D),
    searchField = Color(0xFF525252),
    searchIcon = Color(0xFFCBCBCB),
    searchHint = Color(0xFFCBCBCB),
    searchInk = Color.White,
    pickerTabSelected = Color.White,
    pickerTabIdle = Color(0xFFB3C0D3),
    row = Color(0xFF001846),
    rowFrozen = Color(0xFF002B6F),
    rowTitle = Color.White,
    rowSubtitle = Color(0xFFB3BBC8),
    rowSeparator = Color(0xFF1E345D),
    checkBorder = Color(0xFF61708D),
    checkFill = Color(0xFF617CA7),
    checkMark = Color(0xFF002B6F),
    menu = Color(0xFF012257),
    menuInk = Color.White,
    divider = Color(0xFF223861),
    menuMuted = Color(0xFFB3BBC8),
    sheet = Color(0xFF011947),
    sheetInk = Color(0xFFB3BBC8),
    sheetTitle = Color.White,
    sheetIconCircle = Color(0xFF7F8BA1),
    sheetIconGlyph = Color.Black,
    settingsBg = Color(0xFF424242),
    settingsBar = Color(0xFF525252),
    settingsBarInk = Color.White,
    settingsInk = Color.White,
    settingsSecondary = Color(0xFFC7C7C7),
    settingsDivider = Color(0xFF595959),
    settingsDialog = Color(0xFF525252),
    settingsDisabledInk = Color(0xFF8A8A8A),
    settingsDisabledSub = Color(0xFF707070),
    switchThumbOff = Color(0xFFBDBDBD),
    switchTrackOff = Color(0xFF757575),
)

private val LightCeaseColors = CeaseColors(
    isDark = false,
    accent = Color(0xFF0091EA),
    tabAccent = Color(0xFF86A8FF),
    thumb = Color(0xFF598CFF),
    thumbOff = Color(0xFFB9BCC8),
    track = Color(0xFFF0F0F8),
    panel = Color(0xFFE1DFE2),
    bar = Color(0xFFF0F0F8),
    dock = Color(0xFFDCDAE0),
    barInk = Color(0xFF6F6F71),
    barIcon = Color(0xFF1F1F1F),
    tabIdle = Color(0xFF686669),
    appLabel = Color(0xFF686669),
    emptyHint = Color(0xFF8A8A90),
    iconPlaceholder = Color(0xFFCFCDD2),
    searchField = Color.White,
    searchIcon = Color(0xFF727272),
    searchHint = Color(0xFF666666),
    searchInk = Color(0xFF202020),
    pickerTabSelected = Color(0xFF1F1F1F),
    pickerTabIdle = Color(0xFF6E6E70),
    row = Color(0xFFFBF9FC),
    rowFrozen = Color(0xFFD6DFFE),
    rowTitle = Color(0xFF202020),
    rowSubtitle = Color(0xFF747275),
    rowSeparator = Color(0xFFDDDBDE),
    checkBorder = Color(0xFF9C9A9D),
    checkFill = Color(0xFF858A9D),
    checkMark = Color(0xFFD6DFFE),
    menu = Color(0xFFFDFBFE),
    menuInk = Color(0xFF202020),
    divider = Color(0xFFDCDADD),
    menuMuted = Color(0xFF747275),
    sheet = Color(0xFFFDFBFE),
    sheetInk = Color(0xFF757376),
    sheetTitle = Color(0xFF202020),
    sheetIconCircle = Color(0xFF757376),
    sheetIconGlyph = Color(0xFFFDFBFE),
    settingsBg = Color(0xFFFAFAFA),
    settingsBar = Color.White,
    settingsBarInk = Color(0xFF212121),
    settingsInk = Color(0xFF202020),
    settingsSecondary = Color(0xFF646464),
    settingsDivider = Color(0xFFDCDCDC),
    settingsDialog = Color(0xFFFDFBFE),
    settingsDisabledInk = Color(0xFFAAAAAA),
    settingsDisabledSub = Color(0xFFBBBBBB),
    switchThumbOff = Color(0xFFFAFAFA),
    switchTrackOff = Color(0xFFBDBDBD),
)

private val SparkCeaseColors = CeaseColors(
    isDark = true,
    accent = Color(0xFF3CBCFD),
    tabAccent = Color(0xFF3CBCFD),
    thumb = Color(0xFF0D86EE),
    thumbOff = Color(0xFF45464F),
    track = Color(0xFF16151A),
    panel = Color(0xFF1B1A1F),
    bar = Color(0xFF1B1A1F),
    dock = Color(0xFF1B1A1F),
    barInk = Color(0xFFC4C6D2),
    barIcon = Color(0xFFE0E2EF),
    tabIdle = Color(0xFFC4C6D2),
    appLabel = Color(0xFFC4C6D2),
    emptyHint = Color(0xFF8E909B),
    iconPlaceholder = Color(0xFF2E2D33),
    searchField = Color(0xFF2B2A30),
    searchIcon = Color(0xFFC4C6D2),
    searchHint = Color(0xFF9A9CA7),
    searchInk = Color(0xFFE0E2EF),
    pickerTabSelected = Color(0xFF3CBCFD),
    pickerTabIdle = Color(0xFFC4C6D2),
    row = Color(0xFF1B1A1F),
    rowFrozen = Color(0xFF1B3358),
    rowTitle = Color(0xFFE0E2EF),
    rowSubtitle = Color(0xFFC4C6D2),
    rowSeparator = Color(0xFF2E2D33),
    checkBorder = Color(0xFF8E909B),
    checkFill = Color(0xFF3CBCFD),
    checkMark = Color(0xFF042247),
    menu = Color(0xFF2B2A30),
    menuInk = Color(0xFFE0E2EF),
    divider = Color(0xFF3A3940),
    menuMuted = Color(0xFFC4C6D2),
    sheet = Color(0xFF242329),
    sheetInk = Color(0xFFC4C6D2),
    sheetTitle = Color(0xFFE0E2EF),
    sheetIconCircle = Color(0xFF3CBCFD),
    sheetIconGlyph = Color(0xFF042247),
    settingsBg = Color(0xFF1B1A1F),
    settingsBar = Color(0xFF1B1A1F),
    settingsBarInk = Color(0xFFE0E2EF),
    settingsInk = Color(0xFFE0E2EF),
    settingsSecondary = Color(0xFFC4C6D2),
    settingsDivider = Color(0xFF2E2D33),
    settingsDialog = Color(0xFF2B2A30),
    settingsDisabledInk = Color(0xFF6F717B),
    settingsDisabledSub = Color(0xFF5A5C65),
    switchThumbOff = Color(0xFF8E909B),
    switchTrackOff = Color(0xFF3A3940),
)

private val LocalCeaseColors = staticCompositionLocalOf { DarkCeaseColors }

object FZ {
    val c: CeaseColors
        @Composable @ReadOnlyComposable get() = LocalCeaseColors.current
}

@Composable
fun ceaseCheckboxColors(): CheckboxColors = CheckboxDefaults.colors(
    checkedColor = FZ.c.checkFill,
    uncheckedColor = FZ.c.checkBorder,
    checkmarkColor = FZ.c.checkMark,
)

@Composable
fun CeaseTheme(vm: MainViewModel, content: @Composable () -> Unit) {
    val baseDensity = LocalDensity.current
    val appDensity = remember(baseDensity, vm.dpiScale) {
        Density(baseDensity.density * vm.dpiScale, baseDensity.fontScale)
    }
    val c = when (vm.themeMode) {
        ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) DarkCeaseColors else LightCeaseColors
        ThemeMode.LIGHT -> LightCeaseColors
        ThemeMode.DARK -> DarkCeaseColors
        ThemeMode.SPARK -> SparkCeaseColors
    }
    val scheme = if (c.isDark) {
        darkColorScheme(primary = c.accent, background = c.panel, surface = c.menu, onSurface = c.menuInk)
    } else {
        lightColorScheme(primary = c.accent, background = c.panel, surface = c.menu, onSurface = c.menuInk)
    }
    MaterialTheme(colorScheme = scheme, typography = CeaseTypography) {
        CompositionLocalProvider(LocalCeaseColors provides c, LocalDensity provides appDensity) { content() }
    }
}
