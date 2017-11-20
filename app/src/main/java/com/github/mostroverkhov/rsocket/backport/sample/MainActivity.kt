package com.github.mostroverkhov.rsocket.backport.sample

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.CompoundButton
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxCompoundButton
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.processors.PublishProcessor
import io.rsocket.AbstractRSocket
import io.rsocket.Payload
import io.rsocket.RSocket
import io.rsocket.util.PayloadImpl
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var rsocket: Single<RSocket>
    private lateinit var repo: RepliesRepository
    private lateinit var replyHandler: ReplyHandler
    private val d: CompositeDisposable = CompositeDisposable()
    private val cancelSignals = PublishProcessor.create<Any>()

    override fun onDestroy() {
        d.dispose()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val repliesAdapter = ReplyAdapter(this, replyLimit = 40)
        responses_view.adapter = repliesAdapter
        responses_view.layoutManager = LinearLayoutManager(this)
        replyHandler = ReplyHandler(repliesAdapter)

        repo = Factory.repo()
        rsocket = Factory.client { clientResponder(repo) }.connect()

        d += click { req_rep_view } starts { it.requestResponse(reqResponseRequest()).toFlowable() }

        d += click { req_stream_view } starts { it.requestStream(streamRequest()) }

        d += click { fnf_view } starts { it.fireAndForget(fnfRequest()).toFlowable() }

        d += click { request_channel_view } starts { it.requestChannel(channelRequest()) }

        d += click { request_cancel_all } calls { cancelRequests() }

        d += check { print_responses_view } calls { toggleNewRepliesVisibility(it) }

        d += repo.replies() calls { addNewReply(it) }

        d += repo.errors() calls { showReplyError(it) }
    }

    private fun reqResponseRequest() = PayloadImpl("req-rep ping")

    private fun streamRequest() = PayloadImpl("req-stream ping")

    private fun fnfRequest() = PayloadImpl("client fnf: ${Date()}")

    private fun channelRequest(): Flowable<Payload> =
            Flowable.interval(2, TimeUnit.MILLISECONDS)
                    .onBackpressureDrop()
                    .map { PayloadImpl("req-channel request unbounded") }

    private fun clientResponder(replyRepository: RepliesRepository): RSocket {
        return object : AbstractRSocket() {
            override fun fireAndForget(payload: Payload): Completable {
                return Completable.complete()
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnComplete { replyRepository.onReply(payload) }
            }
        }
    }

    private fun showReplyError(it: Throwable) {
        replyHandler.onError(it)
    }

    private fun addNewReply(it: String) {
        replyHandler.onReply(it)
    }

    private fun toggleNewRepliesVisibility(it: Boolean) {
        changeNewRepliesVisibility(replyHandler, it)
    }

    private fun changeNewRepliesVisibility(replyHandler: ReplyHandler, it: Boolean) {
        replyHandler.onNewRepliesVisibilityChanged(it)
    }

    private fun cancelRequests() {
        cancelSignals.onNext(this)
    }

    private inner class ReplyHandler(private val adapter: ReplyAdapter) {
        private var counter = 0
        private var addNewReplies = true

        fun onReply(rep: String) {
            counter++
            counter_view.text = counter.toString()
            if (addNewReplies) adapter.newReply(rep)
        }

        fun onError(err: Throwable) {
            err.printStackTrace()
            Snackbar.make(responses_view, "Stream error: $err", Snackbar.LENGTH_LONG)
                    .show()
        }

        fun onNewRepliesVisibilityChanged(vis: Boolean) {
            addNewReplies = vis
        }
    }

    private fun click(view: () -> View): Flowable<Any> =
            RxView.clicks(view()).toFlowable(BackpressureStrategy.LATEST)

    private fun check(view: () -> CompoundButton): Flowable<Boolean> =
            RxCompoundButton.checkedChanges(view()).toFlowable(BackpressureStrategy.LATEST)

    private infix fun <T> Flowable<T>.calls(consumer: (T) -> Unit): Disposable = subscribe(consumer)

    private operator fun CompositeDisposable.plusAssign(d: Disposable) {
        this.add(d)
    }

    private infix fun Flowable<Any>.starts(f: (RSocket) -> Flowable<Payload>): Disposable =
            flatMap {
                rsocket.flatMapPublisher { f(it) }
                        .doOnError { repo.onError(it) }
                        .onErrorResumeNext(Flowable.empty())
                        .takeUntil(cancelSignals)
            }.observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { repo.onReply(it) },
                            { repo.onError(it) })

}
