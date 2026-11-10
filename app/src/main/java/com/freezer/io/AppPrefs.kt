package com.freezer.io

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PREFS_NAME = "freezer"
private const val BACKUP_APP = "Freezer"
private const val BACKUP_VERSION = 1
fun backupFileName(): String =
    "freezer_" + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) + ".json"

private object Key {
    const val AUTO_MODE = "auto_mode"
    const val AUTO_DELAY = "auto_delay"
    const val EXCEPT_CHARGING = "ex_charging"
    const val EXCEPT_NOTIFICATIONS = "ex_notif"
    const val EXCEPT_FOREGROUND = "ex_foreground"
    const val ICON_SIZE = "icon_size"
    const val DPI_SCALE = "dpi_scale"
    const val SETUP_DONE = "setup_done"
    const val HIDE_SYSTEM_WARNING = "hide_system_warning"
    const val DEFAULT_MODE = "default_mode"
    const val THEME_MODE = "theme_mode"
    const val SLIDER_STYLE = "slider_style"
    const val TABS_ON = "tabs_on"
    const val DEFAULT_TAB_NAME = "default_tab_name"
    const val TABS = "tabs"
    const val TAB_ORDER = "tab_order"
    const val APP_TABS = "app_tabs"
    const val LIST = "list"
}

private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
    enumValues<E>().firstOrNull { it.name == name } ?: default

class AppPrefs(context: Context) {
    private val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private inline fun <reified E : Enum<E>> getEnum(key: String, default: E): E =
        enumOr(p.getString(key, null), default)

    private fun putEnum(key: String, value: Enum<*>) = p.edit { putString(key, value.name) }

    var autoMode: AutoMode
        get() = getEnum(Key.AUTO_MODE, AutoMode.NEVER)
        set(v) { putEnum(Key.AUTO_MODE, v) }

    var screenOffDelaySec: Int
        get() = p.getInt(Key.AUTO_DELAY, 0)
        set(v) { p.edit { putInt(Key.AUTO_DELAY, v) } }

    var exceptCharging: Boolean
        get() = p.getBoolean(Key.EXCEPT_CHARGING, false)
        set(v) { p.edit { putBoolean(Key.EXCEPT_CHARGING, v) } }

    var exceptNotifications: Boolean
        get() = p.getBoolean(Key.EXCEPT_NOTIFICATIONS, false)
        set(v) { p.edit { putBoolean(Key.EXCEPT_NOTIFICATIONS, v) } }

    var exceptForeground: Boolean
        get() = p.getBoolean(Key.EXCEPT_FOREGROUND, false)
        set(v) { p.edit { putBoolean(Key.EXCEPT_FOREGROUND, v) } }

    var iconSize: IconSizeOption
        get() = getEnum(Key.ICON_SIZE, IconSizeOption.NORMAL)
        set(v) { putEnum(Key.ICON_SIZE, v) }

    var dpiScale: Float
        get() = p.getFloat(Key.DPI_SCALE, 1f)
        set(v) { p.edit { putFloat(Key.DPI_SCALE, v) } }

    var setupDone: Boolean
        get() = p.getBoolean(Key.SETUP_DONE, false)
        set(v) { p.edit { putBoolean(Key.SETUP_DONE, v) } }

    var hideSystemWarning: Boolean
        get() = p.getBoolean(Key.HIDE_SYSTEM_WARNING, false)
        set(v) { p.edit { putBoolean(Key.HIDE_SYSTEM_WARNING, v) } }

    var defaultMode: Mode
        get() = getEnum(Key.DEFAULT_MODE, Mode.FREEZE)
        set(v) { putEnum(Key.DEFAULT_MODE, v) }

    var themeMode: ThemeMode
        get() = getEnum(Key.THEME_MODE, ThemeMode.SYSTEM)
        set(v) { putEnum(Key.THEME_MODE, v) }

    var sliderStyle: SliderStyle
        get() = getEnum(Key.SLIDER_STYLE, SliderStyle.LEFT)
        set(v) { putEnum(Key.SLIDER_STYLE, v) }

    var tabsEnabled: Boolean
        get() = p.getBoolean(Key.TABS_ON, false)
        set(v) { p.edit { putBoolean(Key.TABS_ON, v) } }

    var defaultTabName: String
        get() = p.getString(Key.DEFAULT_TAB_NAME, null) ?: "DEFAULT"
        set(v) { p.edit { putString(Key.DEFAULT_TAB_NAME, v) } }

    private fun loadExtraTabs(): List<TabItem> {
        val raw = p.getString(Key.TABS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                TabItem(o.getString("id"), o.getString("name"), enumOr(o.optString("mode"), Mode.FREEZE))
            }
        }.getOrDefault(emptyList())
    }

    fun saveExtraTabs(list: List<TabItem>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("id", it.id).put("name", it.name).put("mode", it.mode.name)) }
        p.edit { putString(Key.TABS, arr.toString()) }
    }

    fun saveTabOrder(ids: List<String>) {
        p.edit { putString(Key.TAB_ORDER, JSONArray(ids).toString()) }
    }

    private fun loadTabOrder(): List<String> {
        val raw = p.getString(Key.TAB_ORDER, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrDefault(emptyList())
    }

    fun loadTabs(): List<TabItem> {
        val all = listOf(TabItem(DEFAULT_TAB, defaultTabName, defaultMode)) + loadExtraTabs()
        val order = loadTabOrder()
        if (order.isEmpty()) return all
        return all.sortedBy { t -> order.indexOf(t.id).let { if (it < 0) Int.MAX_VALUE else it } }
    }

    fun loadAppTabs(): MutableMap<String, String> {
        val raw = p.getString(Key.APP_TABS, null) ?: return mutableMapOf()
        return runCatching {
            val o = JSONObject(raw)
            o.keys().asSequence().associateWith { o.getString(it) }.toMutableMap()
        }.getOrDefault(mutableMapOf())
    }

    fun saveAppTabs(map: Map<String, String>) {
        val o = JSONObject()
        map.forEach { (k, v) -> o.put(k, v) }
        p.edit { putString(Key.APP_TABS, o.toString()) }
    }

    fun modeResolver(): (String) -> Mode {
        if (!tabsEnabled) return { defaultMode }
        val tabs = loadTabs()
        val map = loadAppTabs()
        return { pkg -> tabs.firstOrNull { it.id == map[pkg] }?.mode ?: defaultMode }
    }

    fun listPkgs(): Set<String> = p.getStringSet(Key.LIST, emptySet()).orEmpty()

    fun saveListPkgs(pkgs: Collection<String>) {
        p.edit { putStringSet(Key.LIST, pkgs.toSet()) }
    }

    fun exportJson(): String {
        val tabs = loadTabs()
        val map = loadAppTabs()
        val byTab = listPkgs().sorted().groupBy { pkg ->
            map[pkg]?.takeIf { id -> tabs.any { it.id == id } } ?: DEFAULT_TAB
        }
        val tabsArr = JSONArray()
        tabs.forEach { t ->
            tabsArr.put(
                JSONObject()
                    .put("id", t.id)
                    .put("name", t.name)
                    .put("mode", t.mode.name)
                    .put("apps", JSONArray(byTab[t.id].orEmpty())),
            )
        }
        val settings = JSONObject()
            .put("theme", themeMode.name)
            .put("slider_style", sliderStyle.name)
            .put("icon_size", iconSize.name)
            .put("dpi_scale", dpiScale.toDouble())
            .put("tabs_enabled", tabsEnabled)
            .put("default_mode", defaultMode.name)
            .put("auto_mode", autoMode.name)
            .put("auto_delay_sec", screenOffDelaySec)
            .put("except_charging", exceptCharging)
            .put("except_notifications", exceptNotifications)
            .put("except_foreground", exceptForeground)
            .put("hide_system_warning", hideSystemWarning)
        return JSONObject()
            .put("app", BACKUP_APP)
            .put("backup_version", BACKUP_VERSION)
            .put("exported_at", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(Date()))
            .put("settings", settings)
            .put("tabs", tabsArr)
            .toString(2)
    }

    fun importJson(text: String): Boolean {
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return false
        if (root.optString("app") != BACKUP_APP) return false
        val s = root.optJSONObject("settings") ?: return false
        val tabsArr = root.optJSONArray("tabs") ?: return false

        val extra = mutableListOf<TabItem>()
        val order = mutableListOf<String>()
        val appTabs = mutableMapOf<String, String>()
        val list = linkedSetOf<String>()
        var defaultName = defaultTabName
        for (i in 0 until tabsArr.length()) {
            val o = tabsArr.optJSONObject(i) ?: continue
            val id = o.optString("id")
            if (id.isEmpty() || id in order) continue
            val name = o.optString("name").ifEmpty { "TAB ${i + 1}" }
            order += id
            if (id == DEFAULT_TAB) defaultName = name
            else extra += TabItem(id, name, enumOr(o.optString("mode"), Mode.FREEZE))
            val apps = o.optJSONArray("apps")
            if (apps != null) for (j in 0 until apps.length()) {
                val pkg = apps.optString(j)
                if (pkg.isEmpty()) continue
                list += pkg
                appTabs[pkg] = id
            }
        }
        if (DEFAULT_TAB !in order) order.add(0, DEFAULT_TAB)

        p.edit {
            putString(Key.THEME_MODE, enumOr(s.optString("theme"), themeMode).name)
            putString(Key.SLIDER_STYLE, enumOr(s.optString("slider_style"), sliderStyle).name)
            putString(Key.ICON_SIZE, enumOr(s.optString("icon_size"), iconSize).name)
            putFloat(Key.DPI_SCALE, s.optDouble("dpi_scale", dpiScale.toDouble()).toFloat())
            putBoolean(Key.TABS_ON, s.optBoolean("tabs_enabled", tabsEnabled))
            putString(Key.DEFAULT_MODE, enumOr(s.optString("default_mode"), defaultMode).name)
            putString(Key.AUTO_MODE, enumOr(s.optString("auto_mode"), autoMode).name)
            putInt(Key.AUTO_DELAY, s.optInt("auto_delay_sec", screenOffDelaySec))
            putBoolean(Key.EXCEPT_CHARGING, s.optBoolean("except_charging", exceptCharging))
            putBoolean(Key.EXCEPT_NOTIFICATIONS, s.optBoolean("except_notifications", exceptNotifications))
            putBoolean(Key.EXCEPT_FOREGROUND, s.optBoolean("except_foreground", exceptForeground))
            putBoolean(Key.HIDE_SYSTEM_WARNING, s.optBoolean("hide_system_warning", hideSystemWarning))
            putString(Key.DEFAULT_TAB_NAME, defaultName)
        }
        saveExtraTabs(extra)
        saveTabOrder(order)
        saveAppTabs(appTabs)
        saveListPkgs(list)
        return true
    }
}
