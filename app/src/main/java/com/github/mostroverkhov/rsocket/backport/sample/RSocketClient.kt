package com.github.mostroverkhov.rsocket.backport.sample

import g.io.rsocket.transport.client.OkhttpWebsocketClientTransport
import io.reactivex.Single
import io.reactivex.processors.AsyncProcessor
import io.reactivex.schedulers.Schedulers
import io.rsocket.RSocket
import io.rsocket.RSocketFactory
import java.util.concurrent.atomic.AtomicReference

/**
 * Created by Maksym Ostroverkhov
 */
class RSocketClient(protocol: String,
                    host: String,
                    port: Int,
                    responder: (RSocket) -> RSocket) {
    private val tryRSocket: AtomicReference<AsyncProcessor<RSocket>> = AtomicReference()
    private val rSocket: Single<RSocket> = RSocketFactory
            .connect()
            .acceptor { -> responder }
            .transport(OkhttpWebsocketClientTransport
                    .create(protocol, host, port))
            .start()

    fun connect(): Single<RSocket> = Single.defer {
        val succ = tryRSocket
                .compareAndSet(null,
                        AsyncProcessor.create<RSocket>())
        if (succ) {
            memoizeSuccess()
        } else {
            tryRSocket.get().firstOrError()
        }
    }

    private fun memoizeSuccess(): Single<RSocket> {
        val rSocket = tryRSocket.get()
        this.rSocket.observeOn(Schedulers.io()).subscribe(
                { rs ->
                    rSocket.onNext(rs)
                    rSocket.onComplete()
                },
                { err ->
                    rSocket.onError(err)
                    tryRSocket.set(null)
                })
        return rSocket.firstOrError()
    }

    companion object {
        private var instance: RSocketClient? = null
        operator fun invoke(responder: (RSocket) -> RSocket): RSocketClient {
            return synchronized(this) {
                if (instance == null) {
                    instance = RSocketClient("http", "192.168.1.101", 8082, responder)
                }
                instance!!
            }
        }
    }
}

