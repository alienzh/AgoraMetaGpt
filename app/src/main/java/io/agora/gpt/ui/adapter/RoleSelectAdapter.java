package io.agora.gpt.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.List;

import io.agora.ai.sdk.AIRole;
import io.agora.ai.sdk.Constants;
import io.agora.gpt.R;
import io.agora.gpt.utils.AppUtils;

public class RoleSelectAdapter extends RecyclerView.Adapter<RoleSelectAdapter.MyViewHolder> implements IMessageView {
    private final Context mContext;
    private final AIRole[] mDataList;

    private int selectIndex;

    private View.OnClickListener onClickListener;

    public void setOnSelectItemClickListener(View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    public RoleSelectAdapter(Context context, int selectIndex, AIRole[] aiRoles) {
        this.mContext = context;
        this.selectIndex = selectIndex;
        this.mDataList = aiRoles;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_ai_roles, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int position) {
        if (null != mDataList) {
            AIRole chatRoleModel = mDataList[position];
            if (Constants.GENDER_MALE.equals(chatRoleModel.getGender())) {
                holder.ivGender.setBackgroundResource(R.drawable.ic_gender_male);
                holder.ivSelect.setBackgroundResource(R.drawable.bg_button_76a_corners_male);
            } else {
                holder.ivGender.setBackgroundResource(R.drawable.ic_gender_female);
                holder.ivSelect.setBackgroundResource(R.drawable.bg_button_e25_corners_female);
            }
            int drawableId = AppUtils.getDrawableRes(mContext, "ai_portrait_" + (position+1));
            if (drawableId == 0) drawableId = R.drawable.ai_portrait_1;
            holder.ivRolePortrait.setImageResource(drawableId);
            if (selectIndex == position) {
                holder.ivRolePortrait.setBackgroundResource(R.drawable.bg_button_role_select);
                holder.ivGender.setVisibility(View.INVISIBLE);
                holder.ivSelect.setVisibility(View.VISIBLE);
            } else {
                holder.ivRolePortrait.setBackgroundResource(R.drawable.bg_button_role_unselect);
                holder.ivGender.setVisibility(View.VISIBLE);
                holder.ivSelect.setVisibility(View.INVISIBLE);
            }
            holder.itemView.setOnClickListener(v -> {
                if (selectIndex == position) return;
                selectIndex = position;
                notifyDataSetChanged();
                if (onClickListener != null) {
                    v.setTag(selectIndex);
                    onClickListener.onClick(v);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        if (null == mDataList) {
            return 0;
        }
        return mDataList.length;
    }

    public AIRole[] getDataList() {
        return mDataList;
    }

    public int getSelectIndex() {
        if (selectIndex >= 0 && selectIndex < mDataList.length) {
            return selectIndex;
        }
        return 0;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivRolePortrait;
        private final ImageView ivGender;
        private final ImageView ivSelect;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRolePortrait = itemView.findViewById(R.id.ivRolePortrait);
            ivGender = itemView.findViewById(R.id.ivGender);
            ivSelect = itemView.findViewById(R.id.ivSelect);
        }
    }
}
