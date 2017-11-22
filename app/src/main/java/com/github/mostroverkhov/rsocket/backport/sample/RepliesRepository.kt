package com.github.mostroverkhov.rsocket.backport.sample

import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.processors.PublishProcessor
import io.reactivex.processors.ReplayProcessor
import io.rsocket.android.Payload

/**
 * Created by Maksym Ostroverkhov
 */
class RepliesRepository {
    private val replies: ReplayProcessor<String> = ReplayProcessor.create(42)
    private val errors: PublishProcessor<Throwable> = PublishProcessor.create()

    fun replies(): Flowable<String> = replies.observeOn(AndroidSchedulers.mainThread())

    fun errors(): Flowable<Throwable> = errors.observeOn(AndroidSchedulers.mainThread())

    fun onError(err: Throwable) = errors.onNext(err)

    fun onReply(reply: Payload) = replies.onNext(reply.dataUtf8)

}