package io.agora.metagpt.ui.activity;

import android.util.Log;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.agora.metagpt.R;
import io.agora.metagpt.models.DataStreamModel;
import io.agora.metagpt.models.UserInfo;
import io.agora.metagpt.models.VoteInfo;
import io.agora.metagpt.models.wiu.GamerInfo;
import io.agora.metagpt.models.wiu.UserSpeakInfoModel;
import io.agora.metagpt.ui.base.BaseActivity;
import io.agora.metagpt.utils.Constants;
import io.agora.metagpt.utils.KeyCenter;

public class BaseGameActivity extends BaseActivity {
    private List<UserInfo> mUserInfoList;
    private Map<Integer, StringBuilder> mPendingDataStreamMessageMap;
    protected int mGameRounds;
    protected List<GamerInfo> mGamerInfoList;

    protected GamerInfo mSelfGamerInfo;

    protected Map<Integer, List<VoteInfo>> mAllRoundsVoteInfoMap;

    @Override
    protected void initData() {
        super.initData();
        if (null == mUserInfoList) {
            mUserInfoList = new ArrayList<>();
        } else {
            mUserInfoList.clear();
        }

        if (null == mPendingDataStreamMessageMap) {
            mPendingDataStreamMessageMap = new HashMap<>();
        } else {
            mPendingDataStreamMessageMap.clear();
        }

        mGameRounds = 1;
        if (null == mAllRoundsVoteInfoMap) {
            mAllRoundsVoteInfoMap = new LinkedHashMap<>();
        } else {
            mAllRoundsVoteInfoMap.clear();
        }
    }

    protected void addUserInfo(UserInfo userInfo) {
        for (UserInfo tempUserInfo : mUserInfoList) {
            if (tempUserInfo.getUid() == userInfo.getUid()) {
                return;
            }
        }
        if (userInfo.getUid() < KeyCenter.USER_MAX_UID) {
            if (mUserInfoList.size() > 0) {
                UserInfo lastUserInfo = mUserInfoList.get(mUserInfoList.size() - 1);
                if (lastUserInfo.getUid() >= KeyCenter.USER_MAX_UID) {
                    mUserInfoList.add(mUserInfoList.size() - 1, userInfo);
                } else {
                    mUserInfoList.add(userInfo);
                }
            } else {
                mUserInfoList.add(userInfo);
            }
        } else if (userInfo.getUid() < KeyCenter.AI_MAX_UID) {
            mUserInfoList.add(userInfo);
        } else {
            mUserInfoList.add(0, userInfo);
        }
    }

    protected void removeUserInfo(UserInfo userInfo) {
        mUserInfoList.remove(userInfo);
    }

    protected UserInfo getUserInfo(int uid) {
        for (UserInfo userInfo : mUserInfoList) {
            if (uid == userInfo.getUid()) {
                return userInfo;
            }
        }
        return null;
    }

    protected List<UserInfo> getUserInfoList() {
        return mUserInfoList;
    }

    protected String getAllUserInfoTxt() {
        StringBuilder sb = new StringBuilder();
        for (UserInfo userInfo : mUserInfoList) {
            sb.append(userInfo.getUsername()).append("(").append(userInfo.getUid()).append(")").append(",");
        }
        if (sb.length() > 0) {
            sb.delete(sb.length() - 1, sb.length());
        }
        return sb.toString();
    }

    @Override
    public void onStreamMessage(int uid, int streamId, byte[] data) {
        Log.i(Constants.TAG, "onStreamMessage uid:" + uid + " data=" + new String(data));
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
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startGame();
                }
            });
        } else if (Constants.DATA_STREAM_CMD_END_GAME.equals(model.getCmd())) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    endGame(model.getMessage());
                }
            });
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
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateVoteInfo(voteInfo.isAiVote());
                }
            });
        }
        if (Constants.DATA_STREAM_CMD_SYNC_GAMER_INFO.equals(model.getCmd())) {
            mGamerInfoList = new ArrayList<>(Arrays.asList(JSON.parseObject(model.getMessage(), GamerInfo[].class)));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateGamerInfo();
                }
            });
        }
    }

    protected void addVoteInfo(VoteInfo voteInfo) {
        List<VoteInfo> voteInfoList = mAllRoundsVoteInfoMap.get(mGameRounds - 1);
        if (voteInfoList == null) {
            voteInfoList = new ArrayList<>();
            mAllRoundsVoteInfoMap.put(mGameRounds - 1, voteInfoList);
        }
        voteInfoList.add(voteInfo);
    }

    protected String getAllVoteInfoMsg() {
        StringBuilder voteMsg = new StringBuilder();
        for (Map.Entry<Integer, List<VoteInfo>> entry : mAllRoundsVoteInfoMap.entrySet()) {
            voteMsg.append("第").append((entry.getKey() + 1)).append("轮投票信息如下：").append("\n");
            List<VoteInfo> voteList = entry.getValue();
            for (VoteInfo voteInfo : voteList) {
                voteMsg.append(String.format(getApplicationContext().getResources().getString(R.string.vote_info), voteInfo.getGamerNumber(), voteInfo.getUndercoverNumber()));
                voteMsg.append("\n");
            }
        }
        return voteMsg.toString();
    }

    protected void startGame() {
        if (mAllRoundsVoteInfoMap != null) {
            mAllRoundsVoteInfoMap.clear();
        }
    }

    protected void endGame(String message) {
        mGameRounds = 1;
        mAllRoundsVoteInfoMap.clear();
        if (null != mGamerInfoList) {
            mGamerInfoList.clear();
        }
    }

    protected void updateVoteInfo(boolean isAiVote) {

    }

    protected void updateGamerInfo() {

    }

    protected void updateTvText(TextView tv, String text) {
        tv.setText(text);
        int offset = tv.getLineCount() * tv.getLineHeight();
        if (offset > tv.getHeight()) {
            tv.scrollTo(0, offset - tv.getHeight());
        }
    }
}
