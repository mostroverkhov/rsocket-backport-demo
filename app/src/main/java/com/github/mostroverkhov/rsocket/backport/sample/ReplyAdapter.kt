package com.github.mostroverkhov.rsocket.backport.sample

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/**
 * Created by Maksym Ostroverkhov on 30.10.17.
 */
class ReplyAdapter(private val c: Context) : RecyclerView.Adapter<Holder>() {
    private val replies: MutableList<String> = mutableListOf()
    private var recycler: RecyclerView? = null
    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.reply_text.text = replies[position]
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
        notifyDataSetChanged()
        recycler?.post {
            recycler?.smoothScrollToPosition(replies.lastIndex)
        }
    }
}

class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val reply_text = itemView.findViewById<TextView>(R.id.reply_item_view)
}