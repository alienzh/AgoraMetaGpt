package io.agora.metagpt.ui.underCover;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.elvishew.xlog.XLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import io.agora.metagpt.MainApplication;
import io.agora.metagpt.R;
import io.agora.metagpt.context.GameContext;
import io.agora.metagpt.context.MetaContext;
import io.agora.metagpt.models.ChatMessageModel;
import io.agora.metagpt.models.DataStreamModel;
import io.agora.metagpt.models.DisplayUserInfo;
import io.agora.metagpt.models.VoteInfo;
import io.agora.metagpt.models.wiu.GamerInfo;
import io.agora.metagpt.models.wiu.UserSpeakInfoModel;
import io.agora.metagpt.utils.Constants;
import io.agora.metagpt.utils.KeyCenter;

public class ModeratorViewModel extends BaseGameViewModel {

    private final static String TAG = Constants.TAG + "-" + ModeratorViewModel.class.getSimpleName();

    protected final MutableLiveData<ModeratorViewStatus> _viewStatus = new MutableLiveData<>();

    @NonNull
    public LiveData<ModeratorViewStatus> viewStatus() {
        return _viewStatus;
    }

    protected final MutableLiveData<DisplayUserInfo> _displayUser = new MutableLiveData<>();

    @NonNull
    public LiveData<DisplayUserInfo> displayUserInfo() {
        return _displayUser;
    }

    protected final MutableLiveData<DisplayUserInfo> _removeDisplayUser = new MutableLiveData<>();

    @NonNull
    public LiveData<DisplayUserInfo> removeDisplayUser() {
        return _removeDisplayUser;
    }

    protected final MutableLiveData<GamerInfo> _aiGamerInfo = new MutableLiveData<>();

    @NonNull
    public LiveData<GamerInfo> aiGamerInfo() {
        return _aiGamerInfo;
    }

    public final MutableLiveData<DisplayUserInfo> _gameSpeakInfo = new MutableLiveData<>();

    @NonNull
    public final LiveData<DisplayUserInfo> gameSpeakInfo() {
        return _gameSpeakInfo;
    }

    public final MutableLiveData<Boolean> _aiCanSpeak = new MutableLiveData<>();

    @NonNull
    public final LiveData<Boolean> aiCanSpeak() {
        return _aiCanSpeak;
    }

    public final MutableLiveData<Boolean> _aiCanVote = new MutableLiveData<>();

    @NonNull
    public final LiveData<Boolean> aiCanVote() {
        return _aiCanVote;
    }

    protected List<ChatMessageModel> mChatMessageDataList = new ArrayList<>();

    private StringBuilder mPendingDataStreamMessage = new StringBuilder();

    protected int mStreamId = -1;

    protected boolean mOnJoinSuccess = false;

    protected boolean mAiSpeaking; // AI 发言中
    protected boolean mAiVoting;  // AI 投票中

    protected int mGameUndercoverNumber;

    @Override
    protected void initData() {
        super.initData();
        if (mPendingDataStreamMessage.length() > 0) {
            mPendingDataStreamMessage.delete(0, mPendingDataStreamMessage.length());
        }
        if (null == mPendingDataStreamMessage) {
            mPendingDataStreamMessage = new StringBuilder();
        } else {
            mPendingDataStreamMessage.delete(0, mPendingDataStreamMessage.length());
        }
        _viewStatus.postValue(new ModeratorViewStatus.Initial());
    }

    protected void onJoinSuccess(int streamId) {
        this.mStreamId = streamId;
        this.mOnJoinSuccess = true;
    }

    protected void onUserOffline(int uid) {
        DisplayUserInfo userInfo = getUserInfo(uid);
        if (null != userInfo) {
            userInfo.setUid(0);
            removeUserInfo(userInfo);
        }
    }

    protected void sendRequestSyncUser() {
        MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_REQUEST_SYNC_USER, "");
    }

    protected void sendSelfUserInfo() {
    }

    @Override
    protected void handleDataStream(int uid, DataStreamModel model) {
        super.handleDataStream(uid, model);
        if (Constants.DATA_STREAM_CMD_USER_JOIN.equals(model.getCmd())) {
            DisplayUserInfo userInfo = JSONObject.parseObject(model.getMessage(), DisplayUserInfo.class);
            addUserInfo(userInfo);
        } else if (Constants.DATA_STREAM_CMD_REQUEST_SYNC_USER.equals(model.getCmd())) {
            sendSelfUserInfo();
        } else if (Constants.DATA_STREAM_CMD_USER_SPEAK_INFO.equals(model.getCmd())) {
            UserSpeakInfoModel userSpeakInfoModel = JSON.parseObject(model.getMessage(), UserSpeakInfoModel.class);
            updateHistoryList(getString(R.string.user_no, userSpeakInfoModel.getGamerNumber()), userSpeakInfoModel.getMessage());
        } else if (Constants.DATA_STREAM_CMD_USER_SPEAK.equals(model.getCmd())) {
            DisplayUserInfo userInfo = JSON.parseObject(model.getMessage(), DisplayUserInfo.class);
            _gameSpeakInfo.postValue(userInfo);
        }
    }

    @Override
    protected void addUserInfo(DisplayUserInfo userInfo) {
        super.addUserInfo(userInfo);
        _displayUser.postValue(userInfo);
        updateHistoryList(GameContext.getInstance().getUserName(), getString(R.string.gamer_join_room, userInfo.getUsername()));
        MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_SYNC_DISPLAY_USER, JSON.toJSONString(mUserInfoList));
    }

    @Override
    protected void removeUserInfo(DisplayUserInfo userInfo) {
        super.removeUserInfo(userInfo);
        _removeDisplayUser.postValue(userInfo);
        updateHistoryList(GameContext.getInstance().getUserName(), getString(R.string.gamer_exit_room, userInfo.getUsername()));
    }

    // 开始游戏
    protected void startGame() {
        super.startGame();
        boolean result = false;
        switch (GameContext.getInstance().getCurrentGame().getGameId()) {
            case Constants.GAME_WHO_IS_UNDERCOVER:
                result = startWhoIsUndercover();
                break;
            default:
                break;
        }
        if (result) {
            _viewStatus.postValue(new ModeratorViewStatus.StartGame());
            MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_START_GAME, "");
        } else {
            Toast.makeText(MainApplication.mGlobalApplication.getApplicationContext(), R.string.start_game_not_enough_players, Toast.LENGTH_SHORT).show();
        }
    }

    // 本轮发言
    // 开始新一轮发言→点击后→请求AI发言
    protected void roundsSpeak(boolean userSpeak) {
        if (userSpeak) {
            MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_START_SPEAK, "");
            updateHistoryList(GameContext.getInstance().getUserName(), getString(R.string.start_speak_tips));
            newRoundSpeak();
        } else {
            mAiSpeaking = true;
            DisplayUserInfo userInfo = getUserInfo(KeyCenter.getAiUid());
            if (userInfo != null) {
                userInfo.setSpeaking(true);
                MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_USER_SPEAK, JSONObject.toJSON(userInfo).toString());
                _gameSpeakInfo.postValue(userInfo);
            }
            requestAiSpeak();
        }
    }

    // 请求ai 发言
    protected void requestAiSpeak() {
    }

    // 新一轮发言
    protected void newRoundSpeak() {

    }

    // 本轮投票
    // 开始本轮投票→点击后→请求AI投票
    protected void roundsVote(boolean userVote) {
        if (userVote) {
            MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_START_VOTE, "");
            updateHistoryList(GameContext.getInstance().getUserName(), getString(R.string.start_vote_tips));
        } else {
            mAiVoting = true;
            requestAiSpeak();
        }
    }

    // ai 发言结束
    protected void sendAIAnswerOverForSpeak() {
        DisplayUserInfo userInfo = getUserInfo(KeyCenter.getAiUid());
        if (userInfo != null) {
            userInfo.setSpeaking(false);
            // ai 发言结束，恢复用户发言
            mAiSpeaking = false;
            _viewStatus.postValue(new ModeratorViewStatus.AISpeakOver());
            _gameSpeakInfo.postValue(userInfo);
            MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_USER_SPEAK, JSONObject.toJSON(userInfo).toString());
        }
    }

    // ai 投票结束
    protected void sendAIAnswerOverForVote() {
        // ai 投票结束，恢复用户投票
        mAiVoting = false;
        handleVoteResult();
    }

    // 结束游戏
    protected void endGame(String message) {
        super.endGame(message);
        _viewStatus.postValue(new ModeratorViewStatus.EndGame());
        mGameUndercoverNumber = -1;
        mAiSpeaking = false;
        mAiVoting = false;
        updateHistoryList(GameContext.getInstance().getUserName(), message);
        sendEndGame(message);
    }

    protected void sendEndGame(String message) {
        MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_END_GAME, message);
    }

    protected boolean startWhoIsUndercover() {
        JSONArray[] wordsJsonArray = GameContext.getInstance().getLocalGameWords();
        int wordIndex = new Random().nextInt(wordsJsonArray.length);
        JSONArray jsonWord = wordsJsonArray[wordIndex];
        String[] gameWords = jsonWord.toArray(new String[0]);
        // 至少三人才能开启游戏
        if (gameWords.length == 2 && mUserInfoList.size() > 2) {
            int undercoverWordIndex = new Random().nextInt(gameWords.length);
//            mGameUndercoverNumber = new Random().nextInt(mUserInfoList.size()) + 1;
            int undercoverIndex = new Random().nextInt(mUserInfoList.size());
            mGameUndercoverNumber = mUserInfoList.get(undercoverIndex).getNumber();
            Log.d(TAG, "mGameUndercoverNumber:" + mGameUndercoverNumber);
            int index = 0;
            GamerInfo gamerInfo;
            mGamerInfoList = new ArrayList<>(mUserInfoList.size());

            for (DisplayUserInfo userInfo : mUserInfoList) {
                if (userInfo.getUid() >= KeyCenter.AI_MAX_UID) {
                    continue;
                }
                gamerInfo = new GamerInfo();
                gamerInfo.setUid(userInfo.getUid());
                gamerInfo.setUserName(userInfo.getUsername());
                gamerInfo.setGameName(GameContext.getInstance().getCurrentGame().getGameName());
                if (userInfo.isAi()) {
                    gamerInfo.setGamerNumber(Constants.MAX_GAMER_NUM);
                } else {
                    gamerInfo.setGamerNumber(index + 1);
                }
                gamerInfo.setOut(false);
                if (gamerInfo.getGamerNumber() == mGameUndercoverNumber) {
                    gamerInfo.setWord(gameWords[undercoverWordIndex]);
                } else {
                    gamerInfo.setWord(gameWords[Math.abs(undercoverWordIndex - 1)]);
                }
                mGamerInfoList.add(gamerInfo);
                index++;
            }
            updateHistoryList(GameContext.getInstance().getUserName(), getString(R.string.start_game_tips));
            updateGamerInfo();
            return true;
        }
        return false;
    }

    @Override
    protected void updateGamerInfo() {
        if (mGamerInfoList.size() > 0) {
            MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_SYNC_GAMER_INFO, JSON.toJSONString(mGamerInfoList));
            for (GamerInfo gamerInfo : mGamerInfoList) {
                if (gamerInfo.getUid() == KeyCenter.getAiUid()) {
                    _aiGamerInfo.postValue(gamerInfo);
                    break;
                }
                if (gamerInfo.isOut()) {
                    DisplayUserInfo displayUserInfo = getUserInfo(gamerInfo.getUid());
                    if (displayUserInfo != null) {
                        displayUserInfo.setOut(true);
                        _displayUser.postValue(displayUserInfo);
                    }
                    String message = getString(R.string.vote_user_eliminated_game_continue, gamerInfo.getGamerNumber());
                    updateHistoryList(GameContext.getInstance().getUserName(), message);
                }
            }
        }
    }

    protected void updateVoteInfo(boolean isAiVote) {
        if (mAllRoundsVoteInfoMap.size() > 0) {
            updateHistoryList(getString(R.string.moderator), getLastVoteInfoMsg());
        }
    }

    protected boolean isAiOut() {
        GamerInfo aiGamerInfo = _aiGamerInfo.getValue();
        return aiGamerInfo != null && aiGamerInfo.isOut();
    }

    public void handleVoteResult() {
        // 投票 map key 玩家号,value 票数
        Map<Integer, Integer> voteMap = new HashMap<Integer, Integer>(mGamerInfoList.size());

//        int maxVote = 0;
        int voteUnderCoverNumber = -1;
        List<VoteInfo> voteInfoList = mAllRoundsVoteInfoMap.get(mGameRounds - 1);
        for (int i = 0; i < voteInfoList.size(); i++) {
            int undercoverNumber = voteInfoList.get(i).getUndercoverNumber(); // 投给谁
            if (voteMap.containsKey(undercoverNumber)) {
                voteMap.put(undercoverNumber, voteMap.get(undercoverNumber) + 1);
            } else {
                voteMap.put(undercoverNumber, 1);
            }
//            if (voteMap.get(undercoverNumber) > maxVote) {
//                maxVote = voteMap.get(undercoverNumber);
//                voteUnderCoverNumber = undercoverNumber;
//            }
        }
        boolean isDraw = false;
        List<Map.Entry<Integer, Integer>> list = new ArrayList<>(voteMap.entrySet()); //转换为list
        Collections.sort(list, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        if (list.size() >= 2) {
            if (list.get(0).getValue() == list.get(1).getValue()) {
                // 最高两位票数一样
                isDraw = true;
            }
        }
        voteUnderCoverNumber = list.get(0).getKey();

        boolean gameOver = voteUnderCoverNumber == mGameUndercoverNumber;

        if (isDraw) {
            String message = getString(R.string.vote_game_over_undercover_draw);
            updateHistoryList(getString(R.string.moderator), message);
            MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_VOTE_DRAW, message);
            mGameRounds++;
            newRoundSpeak();
            _viewStatus.postValue(new ModeratorViewStatus.RoundsReady());
        } else if (gameOver) {
            String message = getString(R.string.vote_game_over_good_person_win, voteUnderCoverNumber);
            endGame(message);
        } else {
            int leftGamers = 0;
            for (GamerInfo gamerInfo : mGamerInfoList) {
                if (voteUnderCoverNumber == gamerInfo.getGamerNumber()) {
                    gamerInfo.setOut(true);
                }
                if (!gamerInfo.isOut()) {
                    leftGamers++;
                }
            }
            if (leftGamers > 2) {
                updateGamerInfo();
                mGameRounds++;
                newRoundSpeak();
                _viewStatus.postValue(new ModeratorViewStatus.RoundsReady());
            } else {
                String message = getString(R.string.vote_game_over_undercover_win, voteUnderCoverNumber);
                endGame(message);
            }
        }
    }

    protected void updateHistoryList(String userName, String message) {
        runOnUiThread(() -> {
            ChatMessageModel chatMessageModel = new ChatMessageModel();
            chatMessageModel.setUsername(userName);
            chatMessageModel.setMessage(message);

            XLog.i(chatMessageModel.toString());

            mChatMessageDataList.add(chatMessageModel);
            if (iMessageView instanceof RecyclerView.Adapter) {
                ((RecyclerView.Adapter<?>) iMessageView).notifyItemInserted(mChatMessageDataList.size() - 1);
            }
            iMessageViewListener.scrollChatMessageListToBottom();
        });
    }
}
