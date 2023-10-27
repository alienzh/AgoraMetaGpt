package io.agora.gpt.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import io.agora.gpt.R
import io.agora.gpt.ui.main.ChatMessageModel
import io.agora.gpt.ui.view.CenterSpaceImageSpan
import io.agora.gpt.ui.view.IconTextSpan
import io.agora.gpt.ui.view.IconTextSpanConfig
import io.agora.gpt.utils.Constant
import io.agora.gpt.utils.Utils

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
        if (chatMessageModel.isAiMessage && chatMessageModel.costTime > 0) {
            val costTime = "${chatMessageModel.costTime}ms"
            ssb.append(costTime)
            val name = "     ${chatMessageModel.name} : "
            ssb.append(name)
            ssb.append(chatMessageModel.message)

            val costTimeIndex = costTime.length + 1
            val iconTextSpanConfig = IconTextSpanConfig().apply {
                imageResId = R.drawable.icon_lightning
                imageWidthPx = 18
                text = costTime
            }
            val iconTextSpan = IconTextSpan(mContext, iconTextSpanConfig)
            ssb.setSpan(iconTextSpan, 0, costTimeIndex, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            val nameEndIndex = costTime.length + name.length
            ssb.setSpan(
                ForegroundColorSpan(Color.parseColor("#A6C4FF")),
                costTimeIndex,
                nameEndIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            ssb.setSpan(StyleSpan(Typeface.BOLD), costTimeIndex, nameEndIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        } else {
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
        }
        holder.tvMessage.text = ssb
    }
}


class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val tvMessage: TextView

    init {
        tvMessage = itemView.findViewById(R.id.tvMessage)
    }
}

class SpacesItemDecoration(private val space: Int) : ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect, view: View,
        parent: RecyclerView, state: RecyclerView.State
    ) {
        outRect.left = 0
        outRect.right = 0
        outRect.bottom = space

        // Add top margin only for the first item to avoid double space between items
        if (parent.getChildAdapterPosition(view) == 0) outRect.top = space
    }
}