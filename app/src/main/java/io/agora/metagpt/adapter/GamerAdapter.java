package io.agora.metagpt.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import io.agora.metagpt.R;
import io.agora.metagpt.models.DisplayUserInfo;
import io.agora.metagpt.utils.KeyCenter;

public class GamerAdapter extends RecyclerView.Adapter<GamerAdapter.MyViewHolder> {
    private final Context mContext;
    private final List<DisplayUserInfo> mDataList;

    public GamerAdapter(Context context, List<DisplayUserInfo> list) {
        this.mContext = context;
        this.mDataList = list;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_gamer_layout, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int position) {
        if (null != mDataList) {
            DisplayUserInfo displayUserInfo = mDataList.get(position);
            String text = mContext.getResources().getString(R.string.user_no, (position + 1));
            holder.tvGamerNo.setText(text);
            if (displayUserInfo.getUid() == 0) {
                holder.ivGamerView.setBackgroundColor(ResourcesCompat.getColor(mContext.getResources(), android.R.color.transparent, null));
            } else {
                int drawableId = 0;
                if (displayUserInfo.isAi()){
                    drawableId = getDrawableId("icon_ai");
                }else {
                    drawableId = getDrawableId("icon_" + (position + 1));
                }

                if (drawableId != 0) {
                    holder.ivGamerView.setBackgroundResource(drawableId);
                } else {
                    holder.ivGamerView.setBackgroundColor(ResourcesCompat.getColor(mContext.getResources(), android.R.color.transparent, null));
                }
            }
            if (displayUserInfo.isOut()) {
                holder.ivOut.setVisibility(View.VISIBLE);
            } else {
                holder.ivOut.setVisibility(View.GONE);
            }
            if (displayUserInfo.isSpeaking()) {
                holder.ivGamerSpeaking.setVisibility(View.VISIBLE);
            } else {
                holder.ivGamerSpeaking.setVisibility(View.GONE);
            }
            if (displayUserInfo.isOut()) {
                holder.ivOut.setVisibility(View.VISIBLE);
            } else {
                holder.ivOut.setVisibility(View.GONE);
            }
            if (displayUserInfo.getUid()==KeyCenter.getUserUid()){
                holder.ivMyselfTag.setVisibility(View.VISIBLE);
            }else {
                holder.ivMyselfTag.setVisibility(View.GONE);
            }
        }
    }

    private int getDrawableId(String name) {
        return mContext.getResources().getIdentifier(name, "drawable", mContext.getPackageName());
    }

    @Override
    public int getItemCount() {
        return mDataList.size();
    }

    public List<DisplayUserInfo> getDataList() {
        return mDataList;
    }

    public void update(DisplayUserInfo userInfo) {
        for (int i = 0; i < mDataList.size(); i++) {
            DisplayUserInfo info = mDataList.get(i);
            if (userInfo.getNumber() == info.getNumber()) {
                mDataList.set(i, userInfo);
                break;
            }
        }
        notifyDataSetChanged();
    }

    public void update(List<DisplayUserInfo> displayUserInfos) {
        for (int i = 0; i < mDataList.size(); i++) {
            DisplayUserInfo info = mDataList.get(i);
            for (int j = 0; j < displayUserInfos.size(); j++) {
                DisplayUserInfo infoP = displayUserInfos.get(j);
                if (info.getNumber() == infoP.getNumber()) {
                    mDataList.set(i, infoP);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void restore() {
        for (int i = 0; i < mDataList.size(); i++) {
            DisplayUserInfo userInfo = mDataList.get(i);
            userInfo.restore();
        }
        notifyDataSetChanged();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivGamerView;
        private final ImageView ivGamerSpeaking;
        private final ImageView ivOut;
        private final TextView tvGamerNo;
        private final ImageView ivMyselfTag;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            ivGamerView = itemView.findViewById(R.id.ivGamerView);
            ivGamerSpeaking = itemView.findViewById(R.id.ivGamerSpeaking);
            ivOut = itemView.findViewById(R.id.ivOut);
            tvGamerNo = itemView.findViewById(R.id.tvGamerNo);
            ivMyselfTag = itemView.findViewById(R.id.ivMyselfTag);
        }
    }
}
