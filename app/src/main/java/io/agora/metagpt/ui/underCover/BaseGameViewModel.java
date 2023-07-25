package io.agora.metagpt.ui.underCover;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.ArrayRes;
import androidx.annotation.StringRes;
import androidx.lifecycle.ViewModel;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.elvishew.xlog.XLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.agora.metagpt.MainApplication;
import io.agora.metagpt.R;
import io.agora.metagpt.ui.adapter.IMessageView;
import io.agora.metagpt.context.MetaContext;
import io.agora.metagpt.models.DataStreamModel;
import io.agora.metagpt.models.DisplayUserInfo;
import io.agora.metagpt.models.VoteInfo;
import io.agora.metagpt.models.wiu.GamerInfo;
import io.agora.metagpt.utils.Constants;

public class BaseGameViewModel extends ViewModel {

    private Handler mHandler = new Handler(Looper.getMainLooper());

    protected ExecutorService mExecutorService;
    protected List<DisplayUserInfo> mUserInfoList = new ArrayList<>();
    private Map<Integer, StringBuilder> mPendingDataStreamMessageMap = new HashMap<>();
    protected int mGameRounds;
    protected List<GamerInfo> mGamerInfoList = new ArrayList<>();

    protected Map<Integer, List<VoteInfo>> mAllRoundsVoteInfoMap = new HashMap<>();

    protected IMessageView iMessageView;
    protected IRecyclerViewListener iMessageViewListener;

    protected void initData() {
        mUserInfoList.clear();
        mPendingDataStreamMessageMap.clear();
        mGameRounds = 1;
        mAllRoundsVoteInfoMap.clear();
        if (null == mExecutorService) {
            mExecutorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                    60L, TimeUnit.SECONDS,
                    new SynchronousQueue<>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        }

    }

    protected void addUserInfo(DisplayUserInfo userInfo) {
        for (DisplayUserInfo tempUserInfo : mUserInfoList) {
            if (tempUserInfo.getUid() == userInfo.getUid()) {
                return;
            }
        }
        if (userInfo.isAi()) {
            userInfo.setNumber(Constants.MAX_GAMER_NUM);
            mUserInfoList.add(userInfo);
        } else {
            if (mUserInfoList.size() > 0) {
                userInfo.setNumber(mUserInfoList.size());
                mUserInfoList.add(mUserInfoList.size() - 1, userInfo);
            } else {
                userInfo.setNumber(1);
                mUserInfoList.add(userInfo);
            }
        }
    }

    protected void removeUserInfo(DisplayUserInfo userInfo) {
        mUserInfoList.remove(userInfo);
    }

    protected DisplayUserInfo getUserInfo(int uid) {
        for (DisplayUserInfo userInfo : mUserInfoList) {
            if (uid == userInfo.getUid()) {
                return userInfo;
            }
        }
        return null;
    }

    protected DisplayUserInfo getUserByNumber(int no) {
        for (DisplayUserInfo userInfo : mUserInfoList) {
            if (no == userInfo.getNumber()) {
                return userInfo;
            }
        }
        return null;
    }

    protected void addVoteInfo(VoteInfo voteInfo) {
        List<VoteInfo> voteInfoList = mAllRoundsVoteInfoMap.get(mGameRounds - 1);
        if (voteInfoList == null) {
            voteInfoList = new ArrayList<>();
            mAllRoundsVoteInfoMap.put(mGameRounds - 1, voteInfoList);
        }
        voteInfoList.add(voteInfo);
    }

    protected String getLastVoteInfoMsg() {
        StringBuilder voteMsg = new StringBuilder();
        List<VoteInfo> voteInfoList = mAllRoundsVoteInfoMap.get(mGameRounds - 1);
        if (voteInfoList != null) {
            if (voteInfoList.size() == 1) {
                voteMsg.append("第").append((mGameRounds)).append("轮投票信息如下：").append("\n");
            }
            VoteInfo voteInfo = voteInfoList.get(voteInfoList.size() - 1);
            voteMsg.append(String.format(getString(R.string.vote_info), voteInfo.getGamerNumber(), voteInfo.getUndercoverNumber()));
            voteMsg.append("\n");
        }
        return voteMsg.toString();
    }

    protected String getAllVoteInfoMsg() {
        StringBuilder voteMsg = new StringBuilder();
        for (Map.Entry<Integer, List<VoteInfo>> entry : mAllRoundsVoteInfoMap.entrySet()) {
            voteMsg.append("第").append((entry.getKey() + 1)).append("轮投票信息如下：").append("\n");
            List<VoteInfo> voteList = entry.getValue();
            for (VoteInfo voteInfo : voteList) {
                voteMsg.append(String.format(MainApplication.mGlobalApplication.getApplicationContext().getResources().getString(R.string.vote_info), voteInfo.getGamerNumber(), voteInfo.getUndercoverNumber()));
                voteMsg.append("\n");
            }
        }
        return voteMsg.toString();
    }

    public void onStreamMessage(int uid, int streamId, byte[] data) {
        XLog.d(Constants.TAG, "onStreamMessage uid:" + uid + " data=" + new String(data));
        final DataStreamModel model = JSONObject.parseObject(new String(data), DataStreamModel.class);
        if (null != model) {
            if (!mPendingDataStreamMessageMap.containsKey(uid)) {
                mPendingDataStreamMessageMap.put(uid, new StringBuilder());
            }
            if (model.getIsEnd()) {
                mPendingDataStreamMessageMap.get(uid).append(model.getMessage());
                model.setMessage(mPendingDataStreamMessageMap.get(uid).toString());
                handleDataStream(uid, model);
                mPendingDataStreamMessageMap.remove(uid);
            } else {
                mPendingDataStreamMessageMap.get(uid).append(model.getMessage());
            }
        }
    }

    protected void handleDataStream(int uid, DataStreamModel model) {
        if (null == model) {
            return;
        }
        if (Constants.DATA_STREAM_CMD_START_GAME.equals(model.getCmd())) {
            runOnUiThread(() -> startGame());
        } else if (Constants.DATA_STREAM_CMD_END_GAME.equals(model.getCmd())) {
            runOnUiThread(() -> endGame(model.getMessage()));
        } else if (Constants.DATA_STREAM_CMD_START_SPEAK.equals(model.getCmd())) {
            try {
                mGameRounds = Integer.parseInt(model.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (Constants.DATA_STREAM_CMD_START_VOTE.equals(model.getCmd())) {
            try {
                mGameRounds = Integer.parseInt(model.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (Constants.DATA_STREAM_CMD_VOTE_INFO.equals(model.getCmd())) {
            VoteInfo voteInfo = JSON.parseObject(model.getMessage(), VoteInfo.class);
            addVoteInfo(voteInfo);
            runOnUiThread(() -> updateVoteInfo(voteInfo.isAiVote()));
        } else if (Constants.DATA_STREAM_CMD_SYNC_GAMER_INFO.equals(model.getCmd())) {
            mGamerInfoList = new ArrayList<>(Arrays.asList(JSON.parseObject(model.getMessage(), GamerInfo[].class)));
            runOnUiThread(() -> updateGamerInfo());
        }
    }

    protected void startGame() {
        if (mAllRoundsVoteInfoMap != null) {
            mAllRoundsVoteInfoMap.clear();
        }
    }

    protected void endGame(String message) {
        mGameRounds = 1;
        mAllRoundsVoteInfoMap.clear();
        mGamerInfoList.clear();
        mPendingDataStreamMessageMap.clear();
    }

    protected void updateVoteInfo(boolean isAiVote) {

    }

    protected void updateGamerInfo() {
    }

    protected void updatePublishCustomAudioTrackChannelOptions() {
    }

    protected void exit() {
        MetaContext.getInstance().leaveRtcChannel();
    }

    protected final void runOnUiThread(Runnable action) {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            mHandler.post(action);
        } else {
            action.run();
        }
    }

    protected final String getString(@StringRes int resId) {
        return MainApplication.mGlobalApplication.getString(resId);
    }

    protected final String getString(@StringRes int resId, Object... formatArgs) {
        return MainApplication.mGlobalApplication.getString(resId, formatArgs);
    }

    protected final int[] getIntArray(@ArrayRes int id) {
        return MainApplication.mGlobalApplication.getResources().getIntArray(id);
    }

    protected final String[] getStringArray(@ArrayRes int id) {
        return MainApplication.mGlobalApplication.getResources().getStringArray(id);
    }
}