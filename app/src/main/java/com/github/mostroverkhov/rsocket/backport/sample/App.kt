package com.github.mostroverkhov.rsocket.backport.sample

import android.app.Application

/**
 * Created by Maksym Ostroverkhov
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        context = this
    }

    companion object {
        lateinit var context: Application
    }
}