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
import io.agora.metagpt.models.HistoryModel;


public class HistoryListAdapter extends RecyclerView.Adapter<HistoryListAdapter.MyViewHolder> {
    private final Context mContext;
    private final List<HistoryModel> mDataList;

    public HistoryListAdapter(Context context, List<HistoryModel> list) {
        this.mContext = context;
        this.mDataList = list;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_history_list, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int position) {
        if (null != mDataList) {
            HistoryModel aiHistoryModel = mDataList.get(position);
            holder.mesasge.setText(aiHistoryModel.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        if (null == mDataList) {
            return 0;
        }
        return mDataList.size();
    }

    public List<HistoryModel> getDataList() {
        return mDataList;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private final TextView mesasge;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            mesasge = (TextView) itemView.findViewById(R.id.tv_message);
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
