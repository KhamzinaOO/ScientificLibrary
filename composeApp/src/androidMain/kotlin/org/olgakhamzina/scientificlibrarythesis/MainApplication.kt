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
        // Global instance to be used for retrieving Context
        lateinit var instance: MainApplication
            private set

        fun getContext() = instance.applicationContext
    }
}