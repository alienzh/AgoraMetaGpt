package io.agora.gpt.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import io.agora.aigc.sdk.constants.Constants
import io.agora.aigc.sdk.constants.Language
import io.agora.aigc.sdk.model.AIRole
import io.agora.gpt.R
import io.agora.gpt.utils.AppUtils
import io.agora.gpt.utils.Constant
import io.agora.gpt.utils.KeyCenter
import io.agora.gpt.utils.dp

class RoleSelectAdapter constructor(
    private val mContext: Context,
    var selectIndex: Int,
    val dataList: List<AIRole>
) :
    RecyclerView.Adapter<RoleSelectAdapter.MyViewHolder>() {
    private var onClickListener: View.OnClickListener? = null
    fun setOnSelectItemClickListener(onClickListener: View.OnClickListener?) {
        this.onClickListener = onClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.item_ai_roles, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val chatRoleModel = dataList[position]
        if (Constants.GENDER_MALE == chatRoleModel.gender) {
            holder.ivGender.setBackgroundResource(R.drawable.ic_gender_male)
            holder.ivSelect.setBackgroundResource(R.drawable.bg_button_76a_corners_male)
        } else {
            holder.ivGender.setBackgroundResource(R.drawable.ic_gender_female)
            holder.ivSelect.setBackgroundResource(R.drawable.bg_button_e25_corners_female)
        }
        val drawableStr :String= KeyCenter.getAvatarDrawableStr(chatRoleModel)

        var drawableId = AppUtils.getDrawableRes(mContext, drawableStr)
        if (drawableId == 0) drawableId = R.drawable.ai_avatar1
        val drawable = ContextCompat.getDrawable(mContext, drawableId)

        Glide.with(mContext).load(drawable)
            .apply(RequestOptions.circleCropTransform())
            .into(holder.ivRolePortrait)
        if (selectIndex == position) {
            holder.ivRolePortrait.setBackgroundResource(R.drawable.bg_button_role_select)
            holder.ivGender.visibility = View.INVISIBLE
            holder.ivSelect.visibility = View.VISIBLE
        } else {
            holder.ivRolePortrait.setBackgroundResource(R.drawable.bg_button_role_unselect)
            holder.ivGender.visibility = View.VISIBLE
            holder.ivSelect.visibility = View.INVISIBLE
        }
        holder.itemView.setOnClickListener { v: View ->
            if (selectIndex == position) return@setOnClickListener
            selectIndex = position
            notifyDataSetChanged()
            onClickListener?.let {
                v.tag = selectIndex
                it.onClick(v)
            }
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivRolePortrait: ImageView
        val ivGender: ImageView
        val ivSelect: ImageView

        init {
            ivRolePortrait = itemView.findViewById(R.id.ivRolePortrait)
            ivGender = itemView.findViewById(R.id.ivGender)
            ivSelect = itemView.findViewById(R.id.ivSelect)
        }
    }
}