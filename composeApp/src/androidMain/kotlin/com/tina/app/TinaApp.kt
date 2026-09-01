package com.tina.app

import android.app.Application
import com.tina.app.di.androidModule
import com.tina.app.di.initKoin
import org.koin.android.ext.koin.androidContext

class TinaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(androidModule) { androidContext(this@TinaApp) }
    }
}
