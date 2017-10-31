package com.github.mostroverkhov.rsocket.backport.sample

import android.app.Application
import android.support.annotation.IntegerRes
import android.support.annotation.StringRes

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

        fun stringRes(@StringRes id: Int): String = context.resources.getString(id)

        fun intRes(@IntegerRes id: Int): Int = context.resources.getInteger(id)
    }
}