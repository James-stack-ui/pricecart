package com.example.pricecart

import android.app.Application

/**
 * Application class used to expose the app context to simple local repositories.
 */
class PriceCartApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: PriceCartApplication
            private set
    }
}

