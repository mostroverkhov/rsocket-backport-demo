package com.github.mostroverkhov.rsocket.backport.sample

import android.support.annotation.IntegerRes
import android.support.annotation.StringRes
import io.rsocket.android.RSocket

/**
 * Created by Maksym Ostroverkhov
 */
class Factory {
    companion object {
        private var rSocketClient: RSocketClient? = null
        fun client(responder: (RSocket) -> RSocket): RSocketClient {
            return synchronized(this) {
                if (rSocketClient == null) {
                    val scheme = stringRes(R.string.ws_scheme)
                    val host = stringRes(R.string.ws_host)
                    val port = intRes(R.integer.ws_port)
                    rSocketClient = RSocketClient(
                            scheme = scheme,
                            host = host,
                            port = port,
                            responder = responder)
                }
                rSocketClient!!
            }
        }

        private fun stringRes(@StringRes id: Int): String = App.context.resources.getString(id)

        private fun intRes(@IntegerRes id: Int): Int = App.context.resources.getInteger(id)

        private var repliesRepository: RepliesRepository? = null
        fun repo(): RepliesRepository {
            return synchronized(this) {
                if (repliesRepository == null) {
                    repliesRepository = RepliesRepository()
                }
                repliesRepository!!
            }
        }

    }

}