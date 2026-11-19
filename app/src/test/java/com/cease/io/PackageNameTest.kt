package com.cease.io

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageNameTest {
    @Test
    fun acceptsRealPackageNames() {
        listOf("android", "com.cease.io", "com.google.android.apps.maps", "org.example.app_2", "a.b1.c_d")
            .forEach { assertTrue(it, isSafePackageName(it)) }
    }

    @Test
    fun rejectsAnythingThatCouldReachTheShell() {
        listOf(
            "", " ", "com.foo; reboot", "com.foo && id", "com.foo\nid", "com.foo|id", "$(id)", "`id`",
            "com..foo", ".com.foo", "com.foo.", "1com.foo", "com.foo bar", "com.foo'", "com.foo\"",
        ).forEach { assertFalse(it, isSafePackageName(it)) }
    }
}
