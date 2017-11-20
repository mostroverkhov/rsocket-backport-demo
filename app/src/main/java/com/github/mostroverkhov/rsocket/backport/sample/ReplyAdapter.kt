package com.github.mostroverkhov.rsocket.backport.sample

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.util.*

/**
 * Created by Maksym Ostroverkhov on 30.10.17.
 */
class ReplyAdapter(private val c: Context, private val replyLimit: Int) : RecyclerView.Adapter<Holder>() {
    private val replies: MutableList<String> = LinkedList()
    private var recycler: RecyclerView? = null
    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.replyText.text = replies[position]
    }

    override fun getItemCount(): Int = replies.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val root = LayoutInflater.from(c).inflate(R.layout.reply_item, parent, false)
        return Holder(root)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView?) {
        recycler = recyclerView
    }

    fun newReply(reply: String) {
        replies += reply
        if (replies.size > replyLimit) {
            replies.removeAt(0)
        }
        notifyDataSetChanged()
        recycler?.post {
            recycler?.smoothScrollToPosition(replies.lastIndex)
        }
    }
}

class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val replyText: TextView = itemView.findViewById(R.id.reply_item_view)
}