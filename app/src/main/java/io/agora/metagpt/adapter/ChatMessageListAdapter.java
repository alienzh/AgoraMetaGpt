package io.agora.metagpt.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import io.agora.metagpt.R;
import io.agora.metagpt.models.ChatMessageModel;


public class ChatMessageListAdapter extends RecyclerView.Adapter<ChatMessageListAdapter.MyViewHolder> {
    private final Context mContext;
    private final List<ChatMessageModel> mDataList;

    public ChatMessageListAdapter(Context context, List<ChatMessageModel> list) {
        this.mContext = context;
        this.mDataList = list;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_chat_message_list, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int position) {
        if (null != mDataList) {
            ChatMessageModel chatMessage = mDataList.get(position);
            if (chatMessage.isAiMessage()) {
                holder.layoutLeftMessage.setVisibility(View.GONE);
                holder.layoutRightMessage.setVisibility(View.VISIBLE);
                holder.rightUsername.setText(chatMessage.getUsername());
                holder.rightMessage.setText(chatMessage.getMessage());
            } else {
                holder.layoutLeftMessage.setVisibility(View.VISIBLE);
                holder.layoutRightMessage.setVisibility(View.GONE);
                holder.leftUsername.setText(chatMessage.getUsername());
                holder.leftMessage.setText(chatMessage.getMessage());
            }
        }
    }

    @Override
    public int getItemCount() {
        if (null == mDataList) {
            return 0;
        }
        return mDataList.size();
    }

    public List<ChatMessageModel> getDataList() {
        return mDataList;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        private final ViewGroup layoutLeftMessage;
        private final TextView leftUsername;
        private final TextView leftMessage;

        private final ViewGroup layoutRightMessage;
        private final TextView rightUsername;
        private final TextView rightMessage;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutLeftMessage = (ViewGroup) itemView.findViewById(R.id.layout_left_message);
            leftUsername = (TextView) itemView.findViewById(R.id.tv_left_username);
            leftMessage = (TextView) itemView.findViewById(R.id.tv_left_message);

            layoutRightMessage = (ViewGroup) itemView.findViewById(R.id.layout_right_message);
            rightUsername = (TextView) itemView.findViewById(R.id.tv_right_username);
            rightMessage = (TextView) itemView.findViewById(R.id.tv_right_message);
        }
    }


    public static class SpacesItemDecoration extends RecyclerView.ItemDecoration {
        private final int space;

        public SpacesItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, @NonNull View view,
                                   RecyclerView parent, @NonNull RecyclerView.State state) {
            outRect.left = 0;
            outRect.right = 0;
            outRect.bottom = space;

            // Add top margin only for the first item to avoid double space between items
            if (parent.getChildAdapterPosition(view) == 0)
                outRect.top = space;
        }
    }
}
