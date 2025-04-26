package org.olgakhamzina.scientificlibrarythesis

import android.app.Application
import org.olgakhamzina.scientificlibrarythesis.DI.initKoin


class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin{
        }
        instance = this
    }

    companion object {
        lateinit var instance: MainApplication
            private set

        fun getContext() = instance.applicationContext
    }
}