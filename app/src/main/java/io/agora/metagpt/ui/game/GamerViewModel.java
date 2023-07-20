package io.agora.metagpt.ui.game;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.elvishew.xlog.XLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.agora.metagpt.R;
import io.agora.metagpt.context.GameContext;
import io.agora.metagpt.context.MetaContext;
import io.agora.metagpt.models.ChatMessageModel;
import io.agora.metagpt.models.DataStreamModel;
import io.agora.metagpt.models.DisplayUserInfo;
import io.agora.metagpt.models.VoteInfo;
import io.agora.metagpt.models.wiu.GamerInfo;
import io.agora.metagpt.models.wiu.UserSpeakInfoModel;
import io.agora.metagpt.stt.xf.SttCallback;
import io.agora.metagpt.stt.xf.XFSttWsManager;
import io.agora.metagpt.utils.Constants;
import io.agora.metagpt.utils.KeyCenter;
import io.agora.metagpt.utils.Utils;
import io.agora.rtc2.IAudioFrameObserver;

public class GamerViewModel extends BaseGameViewModel {

    private final MutableLiveData<GamerViewStatus> _viewStatus = new MutableLiveData<>();

    @NonNull
    public LiveData<GamerViewStatus> viewStatus() {
        return _viewStatus;
    }

    private final MutableLiveData<List<GamerInfo>> _gameInfoList = new MutableLiveData<>();

    @NonNull
    public LiveData<List<GamerInfo>> gameInfoList() {
        return _gameInfoList;
    }

    protected final MutableLiveData<List<DisplayUserInfo>> _displayUserList = new MutableLiveData<>();

    @NonNull
    public LiveData<List<DisplayUserInfo>> displayUserInfo() {
        return _displayUserList;
    }

    protected final MutableLiveData<DisplayUserInfo> _removeDisplayUser = new MutableLiveData<>();

    @NonNull
    public LiveData<DisplayUserInfo> removeDisplayUser() {
        return _removeDisplayUser;
    }

    protected final MutableLiveData<GamerInfo> _selfGamerInfo = new MutableLiveData<>();

    @NonNull
    public LiveData<GamerInfo> selfGamerInfo() {
        return _selfGamerInfo;
    }

    public final MutableLiveData<DisplayUserInfo> _gameSpeakInfo = new MutableLiveData<>();

    @NonNull
    public final LiveData<DisplayUserInfo> gameSpeakInfo() {
        return _gameSpeakInfo;
    }

    protected List<ChatMessageModel> mChatMessageDataList = new ArrayList<>();

    private static final Integer SAMPLE_RATE = 16000;
    private static final Integer SAMPLE_NUM_OF_CHANNEL = 1;
    private static final Integer SAMPLES = 1024;

    private StringBuilder mSttResults;

    private boolean mIsSpanking;
    private boolean mSpeak; // 发言环节
    private boolean mVote;  // 投票环节

    protected int mStreamId = -1;

    protected boolean mOnJoinSuccess = false;

    @Override
    protected void initData() {
        super.initData();
        mChatMessageDataList.clear();
        if (null == mSttResults) {
            mSttResults = new StringBuilder();
        } else {
            mSttResults.delete(0, mSttResults.length());
        }
        MetaContext.getInstance().setRecordingAudioFrameParameters(SAMPLE_RATE, SAMPLE_NUM_OF_CHANNEL, io.agora.rtc2.Constants.RAW_AUDIO_FRAME_OP_MODE_READ_ONLY, SAMPLES);
        XFSttWsManager.getInstance().setCallback(new SttCallback() {
            @Override
            public void onSttResult(String text, boolean isFinish) {
                final boolean isNewLine = mSttResults.length() == 0;
                runOnUiThread(() -> {
                    mSttResults.append(text);
                    updateSelfSpeakChatMessage(text, isNewLine);
                    if (isFinish) {
                        if (mSpeak && !isOut()) {
                            sendUserSpeakInfo();
                        }
                        if (mVote && !isOut()) {
                            int votedId = Utils.getNumberFromStr(mSttResults.toString());
                            sendUserVoteInfo(votedId);
                        }
                    }
                });
            }

            @Override
            public void onSttFail(int errorCode, String message) {

            }
        });

        _viewStatus.postValue(new GamerViewStatus.Initial());
    }

    public void registerAudioFrameObserver(IAudioFrameObserver iAudioFrameObserver) {
        MetaContext.getInstance().setRecordingAudioFrameParameters(SAMPLE_RATE, SAMPLE_NUM_OF_CHANNEL, io.agora.rtc2.Constants.RAW_AUDIO_FRAME_OP_MODE_READ_ONLY, SAMPLES);
        MetaContext.getInstance().registerAudioFrameObserver(iAudioFrameObserver);
    }

    protected void onJoinSuccess(int streamId) {
        this.mStreamId = streamId;
        this.mOnJoinSuccess = true;
        MetaContext.getInstance().updateAutoSubscribeAudio(true);
        sendSelfUserInfo();
    }

    protected void sendSelfUserInfo() {
        DisplayUserInfo displayUserInfo = new DisplayUserInfo(KeyCenter.getUserUid(), MetaContext.getInstance().getUserName(), false);
        MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_USER_JOIN, JSONObject.toJSON(displayUserInfo).toString());
    }

    protected void onUserOffline(int uid) {
        DisplayUserInfo userInfo = getUserInfo(uid);
        if (null != userInfo) {

            userInfo.setUid(0);
            removeUserInfo(userInfo);
        }
    }

    @Override
    protected void addUserInfo(DisplayUserInfo userInfo) {
        // 玩家更新依赖主持人，这里只更新message
        updateHistoryList(getString(R.string.moderator), getString(R.string.gamer_join_room, userInfo.getUsername()));
    }

    @Override
    protected void removeUserInfo(DisplayUserInfo userInfo) {
        super.removeUserInfo(userInfo);
        _removeDisplayUser.postValue(userInfo);
        updateHistoryList(getString(R.string.moderator), getString(R.string.gamer_exit_room, userInfo.getUsername()));
    }

    @Override
    protected void handleDataStream(int uid, DataStreamModel model) {
        super.handleDataStream(uid, model);
        if (Constants.DATA_STREAM_CMD_REQUEST_SYNC_USER.equals(model.getCmd())) {
            sendSelfUserInfo();
        } else if (Constants.DATA_STREAM_CMD_USER_JOIN.equals(model.getCmd())) {
            DisplayUserInfo userInfo = JSONObject.parseObject(model.getMessage(), DisplayUserInfo.class);
            addUserInfo(userInfo);
        } else if (Constants.DATA_STREAM_CMD_START_SPEAK.equals(model.getCmd())) {
            mSpeak = true;
            mVote = false;
            _viewStatus.postValue(new GamerViewStatus.StartSpeak());
            updateHistoryList(getString(R.string.moderator), getString(R.string.start_speak_tips));
        } else if (Constants.DATA_STREAM_CMD_START_VOTE.equals(model.getCmd())) {
            mSpeak = false;
            mVote = true;
            _viewStatus.postValue(new GamerViewStatus.StartVote());
            updateHistoryList(getString(R.string.moderator), getString(R.string.start_vote_tips));
        } else if (Constants.DATA_STREAM_CMD_USER_SPEAK_INFO.equals(model.getCmd())) {
            UserSpeakInfoModel userSpeakInfoModel = JSON.parseObject(model.getMessage(), UserSpeakInfoModel.class);
            updateHistoryList(getString(R.string.user_no, userSpeakInfoModel.getGamerNumber()), userSpeakInfoModel.getMessage());
            // 其他人发言结束，可以发言
            _viewStatus.postValue(new GamerViewStatus.StartSpeak());
            DisplayUserInfo userInfo = getUserInfo(userSpeakInfoModel.getUid());
            if (userInfo != null) {
                userInfo.setSpeaking(false);
                _gameSpeakInfo.postValue(userInfo);
            }
        } else if (Constants.DATA_STREAM_CMD_SYNC_DISPLAY_USER.equals(model.getCmd())) {
            mUserInfoList = new ArrayList<>(Arrays.asList(JSON.parseObject(model.getMessage(), DisplayUserInfo[].class)));
            _displayUserList.postValue(mUserInfoList);
        } else if (Constants.DATA_STREAM_CMD_USER_SPEAK.equals(model.getCmd())) {
            DisplayUserInfo userInfo = JSON.parseObject(model.getMessage(), DisplayUserInfo.class);
            _gameSpeakInfo.postValue(userInfo);
            _viewStatus.postValue(new GamerViewStatus.OtherSpeak(userInfo.isSpeaking()));
        } else if (Constants.DATA_STREAM_CMD_VOTE_DRAW.equals(model.getCmd())) {
            updateHistoryList(getString(R.string.moderator), getString(R.string.vote_game_over_undercover_draw));
        }
    }

    @Override
    protected void updateVoteInfo(boolean isAiVote) {
        super.updateVoteInfo(isAiVote);
        if (mAllRoundsVoteInfoMap.size() > 0) {
            updateHistoryList(getString(R.string.moderator), getLastVoteInfoMsg());
        }
    }

    @Override
    protected void updateGamerInfo() {
        for (GamerInfo gamerInfo : mGamerInfoList) {
            if (gamerInfo.getUid() == KeyCenter.getUserUid()) {
                _selfGamerInfo.postValue(gamerInfo);
            }
            if (gamerInfo.isOut()) {
                DisplayUserInfo displayUserInfo = getUserInfo(gamerInfo.getUid());
                if (displayUserInfo != null) {
                    displayUserInfo.setOut(true);
                    _gameSpeakInfo.postValue(displayUserInfo);
                }
                String message = getString(R.string.vote_user_eliminated_game_continue, gamerInfo.getGamerNumber());
                updateHistoryList(getString(R.string.moderator), message);
            }
        }
    }

    protected boolean isOut() {
        GamerInfo selfGamerInfo = _selfGamerInfo.getValue();
        return selfGamerInfo != null && selfGamerInfo.isOut();
    }

    @Override
    protected void startGame() {
        super.startGame();
        _viewStatus.postValue(new GamerViewStatus.StartGame());
        updateHistoryList(getString(R.string.moderator), getString(R.string.start_game_tips));
    }

    @Override
    protected void endGame(String message) {
        super.endGame(message);
        _viewStatus.postValue(new GamerViewStatus.EndGame());
        mIsSpanking = false;
        mSpeak = false;
        mVote = false;
        updateHistoryList(getString(R.string.moderator), message);
    }

    // 开始发言/结束发言
    protected void startUserSpeak() {
        if (mIsSpanking) {
            mIsSpanking = false;
            XFSttWsManager.getInstance().stt("{\"end\": true}".getBytes());
            MetaContext.getInstance().updateRoleSpeak(false);
            _viewStatus.postValue(new GamerViewStatus.SelfSpeak(false));
            DisplayUserInfo userInfo = getUserInfo(KeyCenter.getUserUid());
            userInfo.setSpeaking(false);
            _gameSpeakInfo.postValue(userInfo);
            MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_USER_SPEAK, JSONObject.toJSON(userInfo).toString());
        } else {
            mIsSpanking = true;
            MetaContext.getInstance().updateRoleSpeak(true);
            mSttResults.delete(0, mSttResults.length());
            DisplayUserInfo userInfo = getUserInfo(KeyCenter.getUserUid());
            userInfo.setSpeaking(true);
            _viewStatus.postValue(new GamerViewStatus.SelfSpeak(true));
            _gameSpeakInfo.postValue(userInfo);
            MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_USER_SPEAK, JSONObject.toJSON(userInfo).toString());
        }
    }

    @Override
    protected void exit() {
        super.exit();
        XFSttWsManager.getInstance().close();
    }

    private void updateSelfSpeakChatMessage(String text, boolean isNewLine) {
        ChatMessageModel chatMessageModel = null;
        int position = -1;
        if (mChatMessageDataList.size() > 0) {
            ChatMessageModel tempChatMessageModel = mChatMessageDataList.get(mChatMessageDataList.size() - 1);
            if (tempChatMessageModel.getUid() != KeyCenter.getUserUid() || isNewLine) {
                position = mChatMessageDataList.size();
                chatMessageModel = new ChatMessageModel();
                chatMessageModel.setUid(KeyCenter.getUserUid());
                chatMessageModel.setUsername("我");
                chatMessageModel.setMessage(text);
                mChatMessageDataList.add(chatMessageModel);
            } else {
                chatMessageModel = tempChatMessageModel;
                chatMessageModel.setMessage(chatMessageModel.getMessage() + text);
                position = mChatMessageDataList.size() - 1;
            }
        } else {
            position = 0;
            chatMessageModel = new ChatMessageModel();
            chatMessageModel.setUid(KeyCenter.getUserUid());
            chatMessageModel.setUsername("我");
            chatMessageModel.setMessage(text);
        }

        if (position == -1) {
            return;
        }
        if (iMessageView instanceof RecyclerView.Adapter) {
            ((RecyclerView.Adapter<?>) iMessageView).notifyItemChanged(position);
        }
        if (iMessageViewListener != null) {
            iMessageViewListener.scrollChatMessageListToBottom();
        }
    }

    public void sendUserSpeakInfo() {
        GamerInfo selfGamerInfo = _selfGamerInfo.getValue();
        if (selfGamerInfo == null) return;
        UserSpeakInfoModel userSpeakInfoModel = new UserSpeakInfoModel();
        userSpeakInfoModel.setUid(KeyCenter.getUserUid());
        userSpeakInfoModel.setUsername(GameContext.getInstance().getUserName());
        userSpeakInfoModel.setMessage(mSttResults.toString());
        userSpeakInfoModel.setGamerNumber(selfGamerInfo.getGamerNumber());
        MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_USER_SPEAK_INFO, JSONObject.toJSON(userSpeakInfoModel).toString());
    }

    public void sendUserVoteInfo(int votedId) {
        GamerInfo selfGamerInfo = _selfGamerInfo.getValue();
        if (selfGamerInfo == null) return;
        VoteInfo voteInfo = new VoteInfo();
        voteInfo.setAiVote(false);
        voteInfo.setGamerNumber(selfGamerInfo.getGamerNumber());
        voteInfo.setUndercoverNumber(votedId);
        addVoteInfo(voteInfo);
        updateVoteInfo(false);
        MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_VOTE_INFO, JSONObject.toJSON(voteInfo).toString());
    }

    private void updateHistoryList(String userName, String message) {
        runOnUiThread(() -> {
            ChatMessageModel chatMessageModel = new ChatMessageModel();
            chatMessageModel.setUsername(userName);
            chatMessageModel.setMessage(message);

            XLog.i(chatMessageModel.toString());
            mChatMessageDataList.add(chatMessageModel);
            if (iMessageView instanceof RecyclerView.Adapter) {
                ((RecyclerView.Adapter<?>) iMessageView).notifyItemInserted(mChatMessageDataList.size() - 1);
            }
            if (iMessageViewListener != null) {
                iMessageViewListener.scrollChatMessageListToBottom();
            }
        });
    }
}
