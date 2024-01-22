package io.agora.gpt.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.agora.gpt.R
import io.agora.gpt.ui.main.ChatMessageModel

class ChatMessageAdapter constructor(private val mContext: Context, val dataList: List<ChatMessageModel>) :
    RecyclerView.Adapter<MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.item_chat_message, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val chatMessageModel = dataList[position]
        val ssb = SpannableStringBuilder()
        val name = "${chatMessageModel.name} : "
        ssb.append(name)
        ssb.append(chatMessageModel.message)
        //设置字体颜色
        ssb.setSpan(
            ForegroundColorSpan(Color.parseColor("#A6C4FF")),
            0,
            name.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        ssb.setSpan(StyleSpan(Typeface.BOLD), 0, name.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        holder.tvMessage.text = ssb
    }
}


class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val tvMessage: TextView

    init {
        tvMessage = itemView.findViewById(R.id.tvMessage)
    }
}