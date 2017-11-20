@file:JvmName("EntryPoint")

package io.rsocket.transport.netty.sampleserver

import io.rsocket.AbstractRSocket
import io.rsocket.Payload
import io.rsocket.RSocketFactory
import io.rsocket.transport.netty.server.WebsocketServerTransport
import io.rsocket.util.PayloadImpl
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.UnicastProcessor
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Maksym Ostroverkhov
 */

class EntryPoint {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

            RSocketFactory.receive()
                    .acceptor { _, sendingSocket ->
                        Mono.just(object : AbstractRSocket() {
                            override fun requestResponse(payload: Payload): Mono<Payload> =
                                    Mono.just(PayloadImpl("reqrep pong!"))

                            override fun requestStream(payload: Payload): Flux<Payload> =
                                    Flux.interval(Duration.ZERO, Duration.ofSeconds(1))
                                            .take(5)
                                            .map { v -> PayloadImpl("reqstream pong! $v") }

                            override fun fireAndForget(payload: Payload?): Mono<Void> =
                                    sendingSocket.fireAndForget(PayloadImpl("server fnf: ${Date()}"))

                            override fun requestChannel(payloads: Publisher<Payload>): Flux<Payload> {
                                val first = AtomicBoolean(true)
                                val response = UnicastProcessor.create<Payload>()
                                Flux.from(payloads)
                                        .flatMap {
                                            if (first.compareAndSet(true, false))
                                                Flux.interval(Duration.ofMillis(1))
                                                        .map { PayloadImpl("req-channel response unbounded") }
                                            else Flux.empty()
                                        }.subscribe(response)
                                return response
                            }
                        })
                    }.transport(WebsocketServerTransport.create(8082))
                    .start()
                    .block()
            Flux.never<Void>().blockFirst()
        }
    }
}
