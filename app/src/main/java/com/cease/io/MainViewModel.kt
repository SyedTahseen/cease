package com.cease.io

import android.app.ActivityOptions
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

private fun RootShell.Result.failureText(prefix: String = "Failed") = "$prefix: ${output.take(200)}"

private const val LAUNCH_INTENT_ATTEMPTS = 6
private const val LAUNCH_INTENT_RETRY_MS = 150L

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = AppPrefs(app)

    val listPkgs = mutableStateListOf<String>().apply { addAll(prefs.listPkgs()) }
    private val appTabs: MutableMap<String, String> = prefs.loadAppTabs()

    var items by mutableStateOf<List<AppItem>>(emptyList()); private set
    var itemsLoaded by mutableStateOf(listPkgs.isEmpty()); private set
    var allApps by mutableStateOf<List<AppItem>>(emptyList()); private set
    var allLoading by mutableStateOf(false); private set

    var defaultMode by mutableStateOf(prefs.defaultMode); private set
    var tabsEnabled by mutableStateOf(prefs.tabsEnabled); private set
    var tabs by mutableStateOf(prefs.loadTabs()); private set
    var currentTabId by mutableStateOf(DEFAULT_TAB); private set

    var autoMode by mutableStateOf(prefs.autoMode); private set
    var screenOffDelaySec by mutableStateOf(prefs.screenOffDelaySec); private set
    var exceptCharging by mutableStateOf(prefs.exceptCharging); private set
    var exceptNotifications by mutableStateOf(prefs.exceptNotifications); private set
    var exceptForeground by mutableStateOf(prefs.exceptForeground); private set

    var sliderStyle by mutableStateOf(prefs.sliderStyle); private set
    var themeMode by mutableStateOf(prefs.themeMode); private set
    var iconSize by mutableStateOf(prefs.iconSize); private set
    var dpiScale by mutableStateOf(prefs.dpiScale); private set

    var query by mutableStateOf("")
    var showSystem by mutableStateOf(false)
    var sortSelectedFirst by mutableStateOf(false)
    var sortByUpdate by mutableStateOf(false)
    var onlyFrozen by mutableStateOf(false)
    var onlyNotFrozen by mutableStateOf(false)
    var showSystemWarning by mutableStateOf(false)
    var hideSystemWarning by mutableStateOf(prefs.hideSystemWarning); private set

    var showPicker by mutableStateOf(false); private set
    var showSettings by mutableStateOf(false); private set
    var showAutoFreeze by mutableStateOf(false); private set
    var showThemeLayout by mutableStateOf(false); private set
    var showSetup by mutableStateOf(!prefs.setupDone); private set
    var setupDone by mutableStateOf(prefs.setupDone); private set
    var setupPage by mutableIntStateOf(0); private set
    var resumeTick by mutableIntStateOf(0); private set
    var pageActivityOpen = false

    var working by mutableStateOf(false); private set
    var rootOk by mutableStateOf<Boolean?>(null); private set
    var message by mutableStateOf<String?>(null)

    private var launching = false

    val anyPageOpen: Boolean
        get() = showSetup || showAutoFreeze || showThemeLayout || showSettings || showPicker

    val visibleItems: List<AppItem>
        get() = if (tabsEnabled) items.filter { it.tabId == currentTabId } else items

    var gridSearching by mutableStateOf(false); private set
    var gridQuery by mutableStateOf("")

    val gridItems: List<AppItem>
        get() {
            val q = gridQuery.trim()
            return if (!gridSearching || q.isEmpty()) visibleItems
            else visibleItems.filter { it.matches(q) }
        }

    private fun AppItem.matches(q: String) = label.contains(q, true) || packageName.contains(q, true)

    fun revealSearchTab() {
        val q = gridQuery.trim()
        if (!gridSearching || q.isEmpty() || !tabsEnabled) return
        if (items.any { it.tabId == currentTabId && it.matches(q) }) return
        val target = tabs.firstOrNull { t -> items.any { it.tabId == t.id && it.matches(q) } } ?: return
        currentTabId = target.id
    }

    fun openGridSearch() { gridSearching = true }

    fun closeGridSearch() {
        gridSearching = false
        gridQuery = ""
    }

    val inForceStopTab: Boolean get() = tabsEnabled && tabMode(currentTabId) == Mode.FORCE_STOP

    private val protectedPackages: Set<String> by lazy {
        val pm = getApplication<Application>().packageManager
        val home = pm.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            ?.activityInfo?.packageName
        setOfNotNull(
            getApplication<Application>().packageName,
            home,
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.android.phone",
            "com.android.providers.telephony",
            "com.android.inputmethod.latin",
            "com.google.android.gms",
            "com.google.android.gsf",
        )
    }

    init {
        refreshList()
        checkRoot()
    }

    fun checkRoot(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { RootShell.isRootAvailable() }
            rootOk = ok
            onResult(ok)
        }
    }

    fun refreshList() {
        viewModelScope.launch {
            val pkgs = listPkgs.toList()
            val resolved = withContext(Dispatchers.IO) { pkgs.mapNotNull { resolve(it) } }
            items = resolved.sortedBy { it.label.lowercase() }
            itemsLoaded = true
            val alive = resolved.map { it.packageName }.toSet()
            if (listPkgs.removeAll { it !in alive }) saveList()
            if (appTabs.keys.retainAll(listPkgs.toSet())) saveAppTabs()
        }
    }

    private fun resolve(pkg: String): AppItem? {
        val pm = getApplication<Application>().packageManager
        return try {
            val info = pm.getApplicationInfo(pkg, 0)
            val tabId = if (!tabsEnabled) DEFAULT_TAB
                else appTabs[pkg]?.takeIf { id -> tabs.any { it.id == id } } ?: DEFAULT_TAB
            val mode = tabMode(tabId)
            AppItem(
                packageName = pkg,
                label = info.loadLabel(pm).toString(),
                isSystem = info.isSystemApp,
                isFrozen = !info.enabled,
                tabId = tabId,
                mode = mode,
                isStopped = mode == Mode.FORCE_STOP && info.isStopped,
            )
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun saveList() = prefs.saveListPkgs(listPkgs)

    private fun saveAppTabs() = prefs.saveAppTabs(appTabs)

    private fun saveTabs() {
        prefs.saveExtraTabs(tabs.filter { it.id != DEFAULT_TAB })
        prefs.saveTabOrder(tabs.map { it.id })
    }

    private fun persist() {
        saveList()
        saveAppTabs()
    }

    private fun addToList(pkg: String, tabId: String) {
        listPkgs.add(pkg)
        appTabs[pkg] = tabId
    }

    private fun removeFromList(pkg: String) {
        listPkgs.remove(pkg)
        appTabs.remove(pkg)
    }

    fun togglePick(app: AppItem) {
        val pkg = app.packageName
        val adding = pkg !in listPkgs
        val tab = if (adding) targetTabId() else (appTabs[pkg] ?: DEFAULT_TAB)
        val mode = tabMode(tab)
        if (adding) addToList(pkg, tab) else removeFromList(pkg)
        persist()
        viewModelScope.launch {
            val needsRoot = mode == Mode.FREEZE && (if (adding) !app.isFrozen else app.isFrozen)
            if (needsRoot) {
                val result = withContext(Dispatchers.IO) {
                    if (adding) RootShell.freeze(listOf(pkg)) else RootShell.unfreeze(listOf(pkg))
                }
                if (result.success) {
                    setFrozenLocal(pkg, adding)
                } else {
                    if (adding) removeFromList(pkg) else addToList(pkg, tab)
                    persist()
                    message = result.failureText()
                }
            }
            refreshList()
        }
    }

    fun removeAndUnfreeze(item: AppItem) = removeApps(listOf(item))

    private fun removeApps(targets: List<AppItem>, onRemoved: () -> Unit = {}) {
        val frozen = targets.filter { it.isFrozen }.map { it.packageName }
        viewModelScope.launch {
            if (frozen.isNotEmpty()) {
                working = true
                val result = withContext(Dispatchers.IO) { RootShell.unfreeze(frozen) }
                working = false
                if (!result.success) {
                    message = result.failureText("Failed to unfreeze")
                    refreshList()
                    return@launch
                }
            }
            targets.forEach { removeFromList(it.packageName) }
            persist()
            refreshList()
            onRemoved()
        }
    }

    private fun setFrozenLocal(pkg: String, frozen: Boolean) {
        allApps = allApps.map { if (it.packageName == pkg) it.copy(isFrozen = frozen) else it }
    }

    fun clearList() {
        val target = visibleItems
        if (working || target.isEmpty()) return
        removeApps(target) { message = "Removed ${target.size} app(s)" }
    }

    fun tabMode(id: String): Mode =
        if (!tabsEnabled) defaultMode else tabs.firstOrNull { it.id == id }?.mode ?: defaultMode

    private fun targetTabId(): String =
        if (tabsEnabled && tabs.any { it.id == currentTabId }) currentTabId else DEFAULT_TAB

    fun updateTabsEnabled(on: Boolean) {
        tabsEnabled = on
        prefs.tabsEnabled = on
        if (!on) currentTabId = DEFAULT_TAB
        refreshList()
    }

    fun updateDefaultMode(m: Mode) {
        defaultMode = m
        prefs.defaultMode = m
        tabs = prefs.loadTabs()
        refreshList()
    }

    fun selectGridTab(id: String) { currentTabId = id }

    fun itemsForTab(id: String): List<AppItem> =
        if (tabsEnabled) items.filter { it.tabId == id } else items

    fun gridItemsFor(id: String): List<AppItem> {
        val q = gridQuery.trim()
        val base = itemsForTab(id)
        return if (!gridSearching || q.isEmpty()) base else base.filter { it.matches(q) }
    }

    fun writeBackup(uri: Uri) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val json = prefs.exportJson()
                    getApplication<Application>().contentResolver.openOutputStream(uri, "wt")!!.use {
                        it.write(json.toByteArray(Charsets.UTF_8))
                    }
                }.isSuccess
            }
            message = if (ok) "Backup saved" else "Couldn't save backup"
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver.openInputStream(uri)!!.use { String(it.readBytes(), Charsets.UTF_8) }
                }.getOrNull()
            }
            if (text == null || !prefs.importJson(text)) {
                message = "Not a valid Cease backup file"
                return@launch
            }
            reloadFromPrefs()
            message = "Backup restored"
        }
    }

    private fun reloadFromPrefs() {
        listPkgs.clear(); listPkgs.addAll(prefs.listPkgs())
        appTabs.clear(); appTabs.putAll(prefs.loadAppTabs())
        defaultMode = prefs.defaultMode
        tabsEnabled = prefs.tabsEnabled
        tabs = prefs.loadTabs()
        currentTabId = DEFAULT_TAB
        autoMode = prefs.autoMode
        screenOffDelaySec = prefs.screenOffDelaySec
        exceptCharging = prefs.exceptCharging
        exceptNotifications = prefs.exceptNotifications
        exceptForeground = prefs.exceptForeground
        sliderStyle = prefs.sliderStyle
        themeMode = prefs.themeMode
        iconSize = prefs.iconSize
        dpiScale = prefs.dpiScale
        hideSystemWarning = prefs.hideSystemWarning
        AutoFreezeService.sync(getApplication())
        refreshList()
    }

    fun addTab(name: String, mode: Mode) {
        val clean = name.trim().ifEmpty { "TAB ${tabs.size + 1}" }
        val tab = TabItem(UUID.randomUUID().toString().take(8), clean, mode)
        tabs = tabs + tab
        saveTabs()
        currentTabId = tab.id
    }

    fun renameTab(id: String, name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        if (id == DEFAULT_TAB) {
            prefs.defaultTabName = clean
            tabs = prefs.loadTabs()
        } else {
            tabs = tabs.map { if (it.id == id) it.copy(name = clean) else it }
            saveTabs()
        }
    }

    fun deleteTab(id: String) {
        if (id == DEFAULT_TAB) return
        appTabs.entries.removeAll { it.value == id }
        saveAppTabs()
        tabs = tabs.filter { it.id != id }
        saveTabs()
        if (currentTabId == id) currentTabId = DEFAULT_TAB
        refreshList()
    }

    fun moveTab(id: String, dir: Int) {
        val list = tabs.toMutableList()
        val i = list.indexOfFirst { it.id == id }
        val j = i + dir
        if (i < 0 || j !in list.indices) return
        list.add(j, list.removeAt(i))
        tabs = list
        saveTabs()
    }

    fun moveApp(pkg: String, tabId: String) {
        appTabs[pkg] = tabId
        saveAppTabs()
        refreshList()
    }

    fun freezeTab(id: String) {
        val list = items.filter { it.tabId == id }
        if (tabMode(id) == Mode.FREEZE) freeze(list.filter { !it.isFrozen }.map { it.packageName })
        else runRootAction(list.filter { !it.isStopped }.map { it.packageName }, "Stopped") { RootShell.forceStop(it) }
    }

    fun unfreezeTab(id: String) =
        unfreeze(items.filter { it.tabId == id && it.isFrozen }.map { it.packageName })

    fun forceStop(item: AppItem) =
        runRootAction(listOf(item.packageName), "Stopped") { RootShell.forceStop(it) }

    fun toggleFreeze(item: AppItem) {
        if (item.isFrozen) unfreeze(listOf(item.packageName)) else freeze(listOf(item.packageName))
    }

    fun unfreezeAll() {
        val targets = visibleItems.filter { it.mode == Mode.FREEZE && it.isFrozen }.map { it.packageName }
        if (targets.isEmpty()) { message = "No frozen apps to unfreeze"; return }
        unfreeze(targets)
    }

    fun openSettings() { showSettings = true }
    fun closeSettings() { showSettings = false }

    fun openAutoFreeze() { showAutoFreeze = true }
    fun closeAutoFreeze() { showAutoFreeze = false }

    fun openThemeLayout() { showThemeLayout = true }
    fun closeThemeLayout() { showThemeLayout = false }

    fun setupNext() { setupPage = 1 }
    fun setupBack() { setupPage = 0 }
    fun openSetup() { setupPage = 1; showSetup = true }
    fun closeSetup() { showSetup = false }
    fun finishSetup() { prefs.setupDone = true; setupDone = true; showSetup = false }
    fun onActivityResumed() { resumeTick++ }

    fun updateAutoMode(mode: AutoMode) {
        autoMode = mode
        prefs.autoMode = mode
        AutoFreezeService.sync(getApplication())
    }

    fun updateScreenOffDelay(sec: Int) { screenOffDelaySec = sec; prefs.screenOffDelaySec = sec }
    fun updateExceptCharging(on: Boolean) { exceptCharging = on; prefs.exceptCharging = on }
    fun updateExceptNotifications(on: Boolean) { exceptNotifications = on; prefs.exceptNotifications = on }
    fun updateExceptForeground(on: Boolean) { exceptForeground = on; prefs.exceptForeground = on }

    fun updateSliderStyle(s: SliderStyle) { sliderStyle = s; prefs.sliderStyle = s }
    fun updateThemeMode(m: ThemeMode) { themeMode = m; prefs.themeMode = m }
    fun updateIconSize(size: IconSizeOption) { iconSize = size; prefs.iconSize = size }
    fun updateDpiScale(scale: Float) { dpiScale = scale; prefs.dpiScale = scale }

    fun dismissSystemWarning(dontShowAgain: Boolean) {
        showSystemWarning = false
        if (dontShowAgain) { hideSystemWarning = true; prefs.hideSystemWarning = true }
    }

    fun selectPickerTab(system: Boolean) {
        showSystem = system
        if (system && !hideSystemWarning) showSystemWarning = true
    }

    fun openPicker() {
        showPicker = true
        loadAll()
    }

    fun closePicker() {
        showPicker = false
        query = ""
        refreshList()
    }

    private fun loadAll() {
        viewModelScope.launch {
            allLoading = allApps.isEmpty()
            allApps = withContext(Dispatchers.IO) {
                val pm = getApplication<Application>().packageManager
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(0)
                    .filter { it.packageName !in protectedPackages && it.applicationInfo != null }
                    .map {
                        val ai = it.applicationInfo!!
                        AppItem(
                            packageName = it.packageName,
                            label = ai.loadLabel(pm).toString(),
                            isSystem = ai.isSystemApp,
                            isFrozen = !ai.enabled,
                            updateTime = it.lastUpdateTime,
                            isStopped = ai.isStopped,
                        )
                    }
                    .sortedBy { it.label.lowercase() }
            }
            allLoading = false
        }
    }

    fun pickerApps(): List<AppItem> {
        var list = allApps.filter {
            (it.isSystem == showSystem) &&
                (query.isBlank() || it.label.contains(query, true) || it.packageName.contains(query, true)) &&
                (!onlyFrozen || it.isFrozen) &&
                (!onlyNotFrozen || !it.isFrozen)
        }
        if (sortByUpdate) list = list.sortedByDescending { it.updateTime }
        return if (sortSelectedFirst) list.sortedByDescending { it.packageName in listPkgs } else list
    }

    fun slideAction(): Boolean {
        if (items.isEmpty()) { message = "No apps yet. Tap the bar below to choose apps."; return false }
        val list = visibleItems
        if (list.isEmpty()) { message = "No apps in this tab. Tap the bar below to add apps."; return false }
        if (working) return false
        val toFreeze = list.filter { it.mode == Mode.FREEZE && !it.isFrozen }.map { it.packageName }
        val toStop = list.filter { it.mode == Mode.FORCE_STOP && !it.isStopped }.map { it.packageName }
        if (toFreeze.isEmpty() && toStop.isEmpty()) {
            val msg = if (list.any { it.mode == Mode.FORCE_STOP }) "All apps are already frozen or stopped" else "All apps are already frozen"
            toast(msg)
            return true
        }
        batch(toFreeze, toStop) {
            toast(message ?: "Done")
            message = null
        }
        return true
    }

    private fun toast(text: String) =
        Toast.makeText(getApplication<Application>(), text, Toast.LENGTH_SHORT).show()

    private fun batch(toFreeze: List<String>, toStop: List<String>, onDone: () -> Unit) {
        viewModelScope.launch {
            working = true
            val result = withContext(Dispatchers.IO) { RootShell.freezeAndStop(toFreeze, toStop) }
            refreshList()
            message = if (result.success) {
                listOfNotNull(
                    if (toFreeze.isNotEmpty()) "Frozen ${toFreeze.size} app(s)" else null,
                    if (toStop.isNotEmpty()) "Stopped ${toStop.size} app(s)" else null,
                ).joinToString(", ")
            } else {
                result.failureText()
            }
            working = false
            onDone()
        }
    }

    fun launchApp(item: AppItem, onDone: (Boolean) -> Unit = {}) {
        if (working || launching) return
        launching = true
        viewModelScope.launch {
            if (item.isFrozen) {
                val result = withContext(Dispatchers.IO) { RootShell.unfreeze(listOf(item.packageName)) }
                if (!result.success) {
                    launching = false
                    onDone(false)
                    message = result.failureText()
                    return@launch
                }
            }
            val ctx = getApplication<Application>()
            val launch = awaitLaunchIntent(item.packageName)
            if (launch == null) {
                message = "${item.label} can't be launched"
                onDone(false)
            } else {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val noAnim = ActivityOptions.makeCustomAnimation(ctx, 0, 0).toBundle()
                ctx.startActivity(launch, noAnim)
                onDone(true)
            }
            launching = false
            refreshList()
        }
    }

    private suspend fun awaitLaunchIntent(pkg: String): Intent? {
        val pm = getApplication<Application>().packageManager
        var intent = pm.getLaunchIntentForPackage(pkg)
        var attempt = 1
        while (intent == null && attempt < LAUNCH_INTENT_ATTEMPTS) {
            delay(LAUNCH_INTENT_RETRY_MS)
            intent = pm.getLaunchIntentForPackage(pkg)
            attempt++
        }
        return intent
    }

    fun launchByPackage(pkg: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val item = withContext(Dispatchers.IO) { resolve(pkg) }
            if (item == null) {
                message = "App is not installed"
                onDone(false)
            } else {
                launchApp(item, onDone)
            }
        }
    }

    private fun freeze(pkgs: List<String>) = runRootAction(pkgs, "Frozen") { RootShell.freeze(it) }

    private fun unfreeze(pkgs: List<String>) = runRootAction(pkgs, "Unfrozen") { RootShell.unfreeze(it) }

    private fun runRootAction(
        pkgs: List<String>,
        verb: String,
        action: (List<String>) -> RootShell.Result,
    ) {
        if (pkgs.isEmpty() || working) return
        viewModelScope.launch {
            working = true
            val result = withContext(Dispatchers.IO) { action(pkgs) }
            refreshList()
            message = if (result.success) "$verb ${pkgs.size} app(s)" else result.failureText()
            working = false
        }
    }
}
