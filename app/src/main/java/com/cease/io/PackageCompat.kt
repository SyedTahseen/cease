package com.cease.io

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

internal fun PackageManager.appInfo(pkg: String): ApplicationInfo =
    if (Build.VERSION.SDK_INT >= 33) {
        getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        getApplicationInfo(pkg, 0)
    }

internal fun PackageManager.packageInfo(pkg: String): PackageInfo =
    if (Build.VERSION.SDK_INT >= 33) {
        getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        getPackageInfo(pkg, 0)
    }

internal fun PackageManager.installedPackages(): List<PackageInfo> =
    if (Build.VERSION.SDK_INT >= 33) {
        getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        getInstalledPackages(0)
    }

internal fun PackageManager.homePackage(): String? {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val info = if (Build.VERSION.SDK_INT >= 33) {
        resolveActivity(intent, PackageManager.ResolveInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        resolveActivity(intent, 0)
    }
    return info?.activityInfo?.packageName
}
