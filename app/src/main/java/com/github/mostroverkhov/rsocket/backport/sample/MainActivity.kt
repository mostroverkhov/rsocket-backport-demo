package com.github.mostroverkhov.rsocket.backport.sample

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.rsocket.AbstractRSocket
import io.rsocket.Payload
import io.rsocket.RSocket
import io.rsocket.util.PayloadImpl
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var rsocket: Single<RSocket>
    private lateinit var repo: RepliesRepository
    private val d: CompositeDisposable = CompositeDisposable()

    override fun onDestroy() {
        d.dispose()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val repliesAdapter = ReplyAdapter(this)
        responses_view.adapter = repliesAdapter
        responses_view.layoutManager = LinearLayoutManager(this)
        val replyHandler = ReplyHandler(repliesAdapter)

        repo = Factory.repo()
        rsocket = Factory.client { clientResponder(repo) }.connect()

        d += whenClicked(req_rep_view).then { it.requestResponse(PayloadImpl("reqrep ping")).toFlowable() }

        d += whenClicked(req_stream_view).then { it.requestStream(PayloadImpl("reqstream ping")) }

        d += whenClicked(fnf_view).thenComplete { it.fireAndForget(PayloadImpl("client fnf: ${Date()}")) }

        d += repo.replies().subscribe({ replyHandler.onReply(it) })

        d += repo.errors().subscribe({ replyHandler.onError(it) })
    }

    private fun Flowable<Any>.then(f: (RSocket) -> Flowable<Payload>): Disposable {
        return flatMap { _ -> rsocket.flatMapPublisher { f(it) } }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ repo.onReply(it) },
                        { repo.onError(it) })
    }

    private fun Flowable<Any>.thenComplete(f: (RSocket) -> Completable): Disposable {
        return flatMapCompletable { _ -> rsocket.flatMapCompletable { f(it) } }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ }, { repo.onError(it) })
    }


    private fun clientResponder(replyRepository: RepliesRepository): RSocket {
        return object : AbstractRSocket() {
            override fun fireAndForget(payload: Payload): Completable {
                return Completable.complete()
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnComplete { replyRepository.onReply(payload) }
            }
        }
    }

    inner class ReplyHandler(private val adapter: ReplyAdapter) {

        fun onReply(rep: String) {
            adapter.newReply(rep)
        }

        fun onError(err: Throwable) {
            err.printStackTrace()
            Snackbar.make(responses_view, "Stream error: $err", Snackbar.LENGTH_LONG)
                    .show()
        }

    }

    private fun whenClicked(view: View): Flowable<Any> =
            RxView.clicks(view).toFlowable(BackpressureStrategy.LATEST)

    private operator fun CompositeDisposable.plusAssign(d: Disposable) {
        this.add(d)
    }
}
