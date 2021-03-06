package com.github.mostroverkhov.rsocket.backport.sample

import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.processors.AsyncProcessor
import io.reactivex.schedulers.Schedulers
import io.rsocket.android.DuplexConnection
import io.rsocket.android.Frame
import io.rsocket.android.RSocket
import io.rsocket.android.RSocketFactory
import io.rsocket.android.plugins.DuplexConnectionInterceptor
import io.rsocket.transport.okhttp.client.OkhttpWebsocketClientTransport
import okhttp3.HttpUrl
import org.reactivestreams.Publisher
import java.util.concurrent.atomic.AtomicReference

/**
 * Created by Maksym Ostroverkhov
 */
class RSocketClient(scheme: String,
                    host: String,
                    port: Int,
                    responder: (RSocket) -> RSocket) {
    private val cachedRSocket: AtomicReference<AsyncProcessor<RSocket>> = AtomicReference()
    private val rSocketSupplier: Single<RSocket> = RSocketFactory
            .connect()
            .addConnectionPlugin(SourceConnectionInterceptor { ResetRSocketOnError(it) })
            .acceptor { -> responder }
            .transport(OkhttpWebsocketClientTransport.create(request(scheme, host, port)))
            .start()

    fun connect(): Single<RSocket> = Single.defer {
        val notCached = cachedRSocket
                .compareAndSet(null,
                        AsyncProcessor.create())
        val rSocket = cachedRSocket.get()
        if (notCached) {
            rSocketSupplier.subscribe(
                    { rs ->
                        rSocket.onNext(rs)
                        rSocket.onComplete()
                    },
                    { err ->
                        rSocket.onError(err)
                        cachedRSocket.set(null)
                    })
        }
        rSocket.firstOrError()
    }.subscribeOn(Schedulers.io())


    private fun request(scheme: String,
                        host: String,
                        port: Int): HttpUrl {
        return HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .port(port)
                .build()
    }

    private inner class ResetRSocketOnError(private val conn: DuplexConnection) : DuplexConnection {
        override fun close(): Completable = conn.close()

        override fun availability() = conn.availability()

        override fun onClose(): Completable = conn.onClose().doOnComplete { cachedRSocket.set(null) }

        override fun receive(): Flowable<Frame> = conn.receive()

        override fun send(frame: Publisher<Frame>): Completable = conn.send(frame)
    }

    private class SourceConnectionInterceptor(
            private val interceptor: (DuplexConnection) -> DuplexConnection)
        : DuplexConnectionInterceptor {
        override fun invoke(type: DuplexConnectionInterceptor.Type,
                            conn: DuplexConnection): DuplexConnection =
                if (type == DuplexConnectionInterceptor.Type.SOURCE) interceptor(conn) else conn
    }
}

