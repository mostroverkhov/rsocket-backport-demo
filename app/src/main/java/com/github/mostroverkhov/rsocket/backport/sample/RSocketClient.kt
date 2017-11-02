package com.github.mostroverkhov.rsocket.backport.sample

import com.github.mostroverkhov.rsocket.backport.transport.okhttp.client.OkhttpWebsocketClientTransport
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.processors.AsyncProcessor
import io.reactivex.schedulers.Schedulers
import io.rsocket.DuplexConnection
import io.rsocket.Frame
import io.rsocket.RSocket
import io.rsocket.RSocketFactory
import io.rsocket.plugins.DuplexConnectionInterceptor
import org.reactivestreams.Publisher
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
            .addConnectionPlugin(SourceConnectionInterceptor { ResetRSocketOnError(it) })
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
    }.subscribeOn(Schedulers.io())

    private fun memoizeSuccess(): Single<RSocket> {
        val rSocket = tryRSocket.get()
        this.rSocket.subscribe(
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

    private inner class ResetRSocketOnError(private val conn: DuplexConnection) : DuplexConnection {
        override fun close(): Completable = conn.close()

        override fun availability(): Double = conn.availability()

        override fun onClose(): Completable = conn.onClose()

        override fun receive(): Flowable<Frame> = conn.receive().doOnError { _ -> tryRSocket.set(null) }

        override fun send(frame: Publisher<Frame>): Completable = conn.send(frame).doOnError { tryRSocket.set(null) }

    }

    private class SourceConnectionInterceptor(
            private val interceptor: (DuplexConnection) -> DuplexConnection)
        : DuplexConnectionInterceptor {
        override fun invoke(type: DuplexConnectionInterceptor.Type,
                            conn: DuplexConnection): DuplexConnection =
                if (type == DuplexConnectionInterceptor.Type.SOURCE) interceptor(conn) else conn
    }
}

