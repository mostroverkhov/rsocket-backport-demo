package com.github.mostroverkhov.rsocket.backport.sample

import android.app.Application
import android.support.annotation.IntegerRes
import android.support.annotation.StringRes
import android.util.Log
import io.reactivex.plugins.RxJavaPlugins

/**
 * Created by Maksym Ostroverkhov
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        context = this
        RxJavaPlugins.setErrorHandler { Log.e(javaClass.simpleName, "uncaught error: ${it.message}") }
    }

    companion object {
        lateinit var context: Application
    }
}