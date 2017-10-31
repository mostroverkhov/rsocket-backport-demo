package com.github.mostroverkhov.rsocket.backport.sample

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.os.PersistableBundle
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
    private lateinit var state: State
    private lateinit var replyHandler: ReplyHandler
    private val d: CompositeDisposable = CompositeDisposable()

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putParcelable(stateKey, state)
    }

    override fun onDestroy() {
        d.dispose()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        state = savedInstanceState?.getParcelable(stateKey) ?: State()

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val repliesAdapter = ReplyAdapter(this, state.replies)
        responses_view.adapter = repliesAdapter
        responses_view.layoutManager = LinearLayoutManager(this)
        replyHandler = ReplyHandler(repliesAdapter)

        rsocket = ClientFactory({
            clientResponder(replyHandler)
        }).connect()

        whenClicked(req_rep_view).thenHandleReply { it.requestResponse(PayloadImpl("reqrep ping")).toFlowable() }

        whenClicked(req_stream_view).thenHandleReply { it.requestStream(PayloadImpl("reqstream ping")) }

        whenClicked(fnf_view).thenHandle { it.fireAndForget(PayloadImpl("client fnf: ${Date()}")) }
    }

    private fun Flowable<Any>.thenHandleReply(f: (RSocket) -> Flowable<Payload>) {
        d += flatMap { _ -> rsocket.flatMapPublisher { f(it) } }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { replyHandler.onReply(it) },
                        { replyHandler.onError(it) })
    }

    private fun Flowable<Any>.thenHandle(f: (RSocket) -> Completable) {
        d += flatMapCompletable { _ -> rsocket.flatMapCompletable { f(it) } }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ }, { replyHandler.onError(it) })
    }


    private fun clientResponder(replyHandler: ReplyHandler): RSocket {
        return object : AbstractRSocket() {
            override fun fireAndForget(payload: Payload): Completable {
                return Completable.complete()
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnComplete { replyHandler.onReply(payload) }
            }
        }
    }

    inner class ReplyHandler(val adapter: ReplyAdapter) {

        fun onReply(rep: Payload) {
            state += rep
            adapter.notifyDataSetChanged()
            responses_view.post {
                responses_view.smoothScrollToPosition(state.replies.lastIndex)
            }
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

    companion object {
        private val stateKey = "save:state"
    }

    internal class State() : Parcelable {

        val replies: MutableList<String> = mutableListOf()

        constructor(parcel: Parcel) : this() {
            repeat(parcel.readInt(), { replies += parcel.readString() })
        }

        infix operator fun plus(reply: Payload): State {
            replies += reply.dataUtf8
            if (replies.size > repliesLimit) {
                replies.removeAt(0)
            }
            return this
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(replies.size)
            replies.forEach { parcel.writeString(it) }
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<State> {
            override fun createFromParcel(parcel: Parcel): State {
                return State(parcel)
            }

            override fun newArray(size: Int): Array<State?> {
                return arrayOfNulls(size)
            }

            private val repliesLimit = 42
        }
    }
}
