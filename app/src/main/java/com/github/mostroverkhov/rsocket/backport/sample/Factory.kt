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
                    val protocol = stringRes(R.string.rs_protocol)
                    val host = stringRes(R.string.rs_host)
                    val port = intRes(R.integer.rs_port)
                    rSocketClient = RSocketClient(
                            protocol = protocol,
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