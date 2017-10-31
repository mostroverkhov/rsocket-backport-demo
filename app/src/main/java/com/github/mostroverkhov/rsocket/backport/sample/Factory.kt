package com.github.mostroverkhov.rsocket.backport.sample

import io.rsocket.RSocket

/**
 * Created by Maksym Ostroverkhov
 */
class Factory {
    companion object {
        private var rSocketClient: RSocketClient? = null
        fun client(responder: (RSocket) -> RSocket): RSocketClient {
            return synchronized(this) {
                if (rSocketClient == null) {
                    val protocol = App.stringRes(R.string.rs_protocol)
                    val host = App.stringRes(R.string.rs_host)
                    val port = App.intRes(R.integer.rs_port)
                    rSocketClient = RSocketClient(
                            protocol = protocol,
                            host = host,
                            port = port,
                            responder = responder)
                }
                rSocketClient!!
            }
        }

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