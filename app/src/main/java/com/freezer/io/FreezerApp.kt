package com.freezer.io

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

class FreezerApp : Application(), ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}

fun ComponentActivity.sharedViewModel(): MainViewModel {
    val app = application as FreezerApp
    return ViewModelProvider(app, ViewModelProvider.AndroidViewModelFactory.getInstance(app))[MainViewModel::class.java]
}
