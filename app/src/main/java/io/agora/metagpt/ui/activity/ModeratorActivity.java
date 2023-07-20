package io.agora.metagpt.ui.activity;


import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.elvishew.xlog.XLog;
import com.jakewharton.rxbinding2.view.RxView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;

import java.util.concurrent.TimeUnit;

import io.agora.metagpt.R;

import io.agora.metagpt.adapter.HistoryListAdapter;
import io.agora.metagpt.context.GameContext;
import io.agora.metagpt.context.MetaContext;
import io.agora.metagpt.databinding.ModeratorActivityBinding;
import io.agora.metagpt.inf.State;
import io.agora.metagpt.models.HistoryModel;
import io.agora.metagpt.models.DataStreamModel;
import io.agora.metagpt.models.UserInfo;
import io.agora.metagpt.models.VoteInfo;
import io.agora.metagpt.models.wiu.UserSpeakInfoModel;
import io.agora.metagpt.models.wiu.GamerInfo;
import io.agora.metagpt.ui.main.MainActivity;
import io.agora.metagpt.utils.Constants;
import io.agora.metagpt.utils.KeyCenter;
import io.agora.rtc2.DataStreamConfig;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.reactivex.disposables.Disposable;


public class ModeratorActivity extends BaseGameActivity {
    private final static String TAG = Constants.TAG + "-" + ModeratorActivity.class.getSimpleName();
    private ModeratorActivityBinding binding;
    private SimpleDateFormat mSdf;

    private boolean mJoinChannelSuccess;

    private ExecutorService mExecutorService;

    private int mStreamId;

    private StringBuilder mPendingDataStreamMessage;

    private List<HistoryModel> mHistoryDataList;
    private HistoryListAdapter mHistoryListAdapter;

    private int mGameUndercoverNumber;

    private State mCurrentState;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initRtc();
    }

    @Override
    protected void initContentView() {
        super.initContentView();
        binding = ModeratorActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @Override
    protected void initData() {
        super.initData();
        mSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

        mStreamId = -1;

        if (null == mPendingDataStreamMessage) {
            mPendingDataStreamMessage = new StringBuilder();
        } else {
            mPendingDataStreamMessage.delete(0, mPendingDataStreamMessage.length());
        }


        if (null == mHistoryDataList) {
            mHistoryDataList = new ArrayList<>();
        } else {
            mHistoryDataList.clear();
        }

        mCurrentState = mInitialState;
        mCurrentState.enter();

        mGameUndercoverNumber = -1;
    }

    @Override
    protected void initView() {
        binding.roomNameTv.setText(String.format(getApplicationContext().getResources().getString(R.string.room_name_label), MetaContext.getInstance().getRoomName()));
        binding.userNameTv.setText(String.format(getApplicationContext().getResources().getString(R.string.user_name_label), MetaContext.getInstance().getUserName(), KeyCenter.getModeratorUid()));

        addUserInfo(new UserInfo(KeyCenter.getModeratorUid(), getApplication().getResources().getString(R.string.moderator)));
        updateAllUserNames();

        if (mHistoryListAdapter == null) {
            mHistoryListAdapter = new HistoryListAdapter(getApplicationContext(), mHistoryDataList);
            binding.historyList.setAdapter(mHistoryListAdapter);
            binding.historyList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
            binding.historyList.addItemDecoration(new HistoryListAdapter.SpacesItemDecoration(10));
        } else {
            int itemCount = mHistoryDataList.size();
            mHistoryDataList.clear();
            mHistoryListAdapter.notifyItemRangeRemoved(0, itemCount);
        }

        binding.btnEndGame.setEnabled(false);
        binding.btnRequestAi.setEnabled(false);
        binding.btnSpeak.setEnabled(false);
        binding.btnVote.setEnabled(false);

        binding.gameContentTv.setMovementMethod(ScrollingMovementMethod.getInstance());
        binding.voteResultTv.setMovementMethod(ScrollingMovementMethod.getInstance());
    }


    @Override
    protected void initListener() {
        super.initListener();
    }

    @Override
    protected void initClickEvent() {
        super.initClickEvent();
        Disposable disposable;
        disposable = RxView.clicks(binding.btnExit).throttleFirst(1, TimeUnit.SECONDS).subscribe(o -> {
            exit();
        });
        compositeDisposable.add(disposable);

        disposable = RxView.clicks(binding.btnStartGame).throttleFirst(1, TimeUnit.SECONDS).subscribe(o -> {
            mCurrentState.transitionTo(mStartGameState);
        });
        compositeDisposable.add(disposable);
        disposable = RxView.clicks(binding.btnEndGame).throttleFirst(1, TimeUnit.SECONDS).subscribe(o -> {
            mCurrentState.transitionTo(mEndGameState);
        });
        compositeDisposable.add(disposable);

        disposable = RxView.clicks(binding.btnRequestAi).throttleFirst(1, TimeUnit.SECONDS).subscribe(o -> {
            if (mCurrentState == mSpeakState) {
                mCurrentState.transitionTo(mAiSpeakState);
            } else if (mCurrentState == mVoteState) {
                mCurrentState.transitionTo(mAiVoteState);
            }
        });
        compositeDisposable.add(disposable);

        disposable = RxView.clicks(binding.btnSpeak).throttleFirst(1, TimeUnit.SECONDS).subscribe(o -> {
            mCurrentState.transitionTo(mSpeakState);
        });
        compositeDisposable.add(disposable);

        disposable = RxView.clicks(binding.btnVote).throttleFirst(1, TimeUnit.SECONDS).subscribe(o -> {
            mCurrentState.transitionTo(mVoteState);
        });
        compositeDisposable.add(disposable);
    }

    private void initRtc() {
        mJoinChannelSuccess = false;
        if (MetaContext.getInstance().initializeRtc(this.getApplicationContext())) {
            registerRtc();
            MetaContext.getInstance().joinChannel(io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER);
        } else {
            Log.i(TAG, "initRtc fail");
        }
    }

    @Override
    public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
        mJoinChannelSuccess = true;
        if (-1 == mStreamId) {
            DataStreamConfig cfg = new DataStreamConfig();
            cfg.syncWithAudio = false;
            cfg.ordered = true;
            mStreamId = MetaContext.getInstance().createDataStream(cfg);
        }
        MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_REQUEST_SYNC_USER, "");
        sendSelfUserInfo();
        MetaContext.getInstance().updateRoleSpeak(true);
    }

    @Override
    public void onLeaveChannel(IRtcEngineEventHandler.RtcStats stats) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MetaContext.getInstance().destroy();
                goBackHome();
            }
        });
    }

    @Override
    public void onUserOffline(int uid, int reason) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                UserInfo userInfo = getUserInfo(uid);
                if (null != userInfo) {
                    updateHistoryList("玩家" + userInfo.getUsername() + "(" + userInfo.getUid() + ")" + "退出");
                    removeUserInfo(userInfo);
                    updateAllUserNames();
                }
            }
        });
    }

    @Override
    protected void handleDataStream(int uid, DataStreamModel model) {
        super.handleDataStream(uid, model);
        if (Constants.DATA_STREAM_CMD_USER_JOIN.equals(model.getCmd())) {
            UserInfo userInfo = JSONObject.parseObject(model.getMessage(), UserInfo.class);
            addUserInfo(userInfo);
            updateHistoryList("玩家" + userInfo.getUsername() + "(" + userInfo.getUid() + ")" + "加入");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateAllUserNames();
                }
            });
        } else if (Constants.DATA_STREAM_CMD_REQUEST_SYNC_USER.equals(model.getCmd())) {
            sendSelfUserInfo();
        } else if (Constants.DATA_STREAM_CMD_AI_ANSWER_OVER.equals(model.getCmd())) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mCurrentState == mAiSpeakState) {
                        mCurrentState.transitionTo(mAiSpeakOverState);
                    }else if (mCurrentState == mAiVoteState) {
                        mCurrentState.transitionTo(mVoteResultState);
                    }
                }
            });
        } else if (Constants.DATA_STREAM_CMD_USER_SPEAK_INFO.equals(model.getCmd())) {
            UserSpeakInfoModel userSpeakInfoModel = JSON.parseObject(model.getMessage(), UserSpeakInfoModel.class);
            updateHistoryList("收到" + userSpeakInfoModel.getUsername() + "的发言：" + userSpeakInfoModel.getMessage());
        } else if (Constants.DATA_STREAM_CMD_AI_ANSWER.equals(model.getCmd())) {
            updateHistoryList("收到AI的回答：" + model.getMessage());
        }
    }

    private void goBackHome() {
        Intent intent = new Intent(ModeratorActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        finish();
    }

    private void exit() {
        MetaContext.getInstance().leaveRtcChannel();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initRtc();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
    }

    private void sendSelfUserInfo() {
        MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_USER_JOIN, JSONObject.toJSON(new UserInfo(KeyCenter.getModeratorUid(), getApplication().getResources().getString(R.string.moderator))).toString());
    }

    private void updateAllUserNames() {
        binding.allUserTv.setText(String.format(getApplicationContext().getResources().getString(R.string.all_user_name_label), getAllUserInfoTxt()));
    }

    private void updateHistoryList(String newMessage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String newLine = "[" + mSdf.format(System.currentTimeMillis()) + "]  " + newMessage;
                XLog.i(newLine);

                HistoryModel historyModel = new HistoryModel();
                historyModel.setMessage(newLine);
                mHistoryDataList.add(historyModel);
                if (null != mHistoryListAdapter) {
                    mHistoryListAdapter.notifyItemInserted(mHistoryDataList.size() - 1);
                    binding.historyList.scrollToPosition(mHistoryListAdapter.getDataList().size() - 1);
                }
            }
        });
    }

    @Override
    protected void startGame() {
        super.startGame();
        updateTvText(binding.gameContentTv, "");
        updateTvText(binding.voteResultTv, "");
        switch (GameContext.getInstance().getCurrentGame().getGameId()) {
            case Constants.GAME_WHO_IS_UNDERCOVER:
                startWhoIsUndercover();
                break;
            default:
                break;
        }
    }

    @Override
    protected void endGame(String message) {
        super.endGame(message);
        mGameUndercoverNumber = -1;
        updateHistoryList("结束游戏：" + GameContext.getInstance().getCurrentGame().getGameName());
    }

    private void startWhoIsUndercover() {
        updateHistoryList("开始游戏：" + GameContext.getInstance().getCurrentGame().getGameName());
        JSONArray[] wordsJsonArray = GameContext.getInstance().getLocalGameWords();
        int wordIndex = new Random().nextInt(wordsJsonArray.length);
        JSONArray jsonWord = wordsJsonArray[wordIndex];
        String[] gameWords = jsonWord.toArray(new String[0]);
        if (gameWords.length == 2 && getUserInfoList().size() > 1) {
            int undercoverWordIndex = new Random().nextInt(gameWords.length);
            mGameUndercoverNumber = new Random().nextInt(getUserInfoList().size() - 1) + 1;
            int index = 0;
            GamerInfo gamerInfo;
            mGamerInfoList = new ArrayList<>(getUserInfoList().size());

            UserInfo aiUserInfo = null;
            for (UserInfo userInfo : getUserInfoList()) {
                if (userInfo.getUid() >= KeyCenter.AI_MAX_UID) {
                    continue;
                }
                gamerInfo = new GamerInfo();
                gamerInfo.setUid(userInfo.getUid());
                gamerInfo.setUserName(userInfo.getUsername());
                gamerInfo.setGameName(GameContext.getInstance().getCurrentGame().getGameName());
                gamerInfo.setGamerNumber(index + 1);
                gamerInfo.setOut(false);
                if (gamerInfo.getGamerNumber() == mGameUndercoverNumber) {
                    gamerInfo.setWord(gameWords[undercoverWordIndex]);
                } else {
                    gamerInfo.setWord(gameWords[Math.abs(undercoverWordIndex - 1)]);
                }
                mGamerInfoList.add(gamerInfo);
                index++;
            }
            updateGamerInfo();

        }
    }

    @Override
    protected void updateGamerInfo() {
        if (mGamerInfoList.size() > 0) {
            MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_SYNC_GAMER_INFO, JSON.toJSONString(mGamerInfoList));
            StringBuilder gameContent = new StringBuilder();
            gameContent.append(GameContext.getInstance().getCurrentGame().getGameName()).append("\n");
            for (GamerInfo gamerInfo : mGamerInfoList) {
                if (gamerInfo.isOut()) {
                    gameContent.append("[").append("已淘汰").append("]");
                }
                gameContent.append(gamerInfo.getGamerNumber()).append("号玩家").append("(").append(gamerInfo.getUserName()).append("-").append(gamerInfo.getUid()).append(")").append(":").append(gamerInfo.getWord()).append("\n");
            }
            updateTvText(binding.gameContentTv, gameContent.toString());
        }
    }

    @Override
    protected void updateVoteInfo(boolean isAiVote) {
        if (mAllRoundsVoteInfoMap.size() > 0) {
            binding.voteResultTv.setVisibility(View.VISIBLE);
            updateTvText(binding.voteResultTv, getAllVoteInfoMsg());
        } else {
            binding.voteResultTv.setVisibility(View.GONE);
        }
    }

    private final State mInitialState = new InitialState();
    private final State mStartGameState = new StartGameState();

    private final State mRoundsReadyState = new RoundsReadyState();
    private final State mSpeakState = new SpeakState();
    private final State mAiSpeakState = new AiSpeakState();
    private final State mAiSpeakOverState = new AiSpeakOverState();
    private final State mVoteState = new VoteState();
    private final State mAiVoteState = new AiVoteState();
    private final State mVoteResultState = new VoteResultState();
    private final State mEndGameState = new EndGameState();

    class InitialState implements State {
        @Override
        public void enter() {
            State.super.enter();
            mCurrentState = this;
            binding.btnSpeak.setText(String.format(getApplicationContext().getResources().getString(R.string.speak), mGameRounds));
            binding.btnVote.setText(String.format(getApplicationContext().getResources().getString(R.string.vote), mGameRounds));
        }
    }

    class StartGameState implements State {
        @Override
        public void enter() {
            mCurrentState = this;
            binding.btnStartGame.setEnabled(false);
            binding.btnEndGame.setEnabled(true);
            binding.btnRequestAi.setEnabled(false);
            binding.btnSpeak.setEnabled(true);
            binding.btnVote.setEnabled(false);

            binding.btnSpeak.setText(String.format(getApplicationContext().getResources().getString(R.string.speak), mGameRounds));
            binding.btnVote.setText(String.format(getApplicationContext().getResources().getString(R.string.vote), mGameRounds));

            startGame();
            MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_START_GAME, "");

            transitionTo(mRoundsReadyState);
        }
    }

    class RoundsReadyState implements State {
        @Override
        public void enter() {
            binding.btnSpeak.setEnabled(true);
            binding.btnRequestAi.setText(getApplicationContext().getResources().getString(R.string.request_ai_speak));
            binding.btnSpeak.setText(String.format(getApplicationContext().getResources().getString(R.string.speak), mGameRounds));
            binding.btnVote.setText(String.format(getApplicationContext().getResources().getString(R.string.vote), mGameRounds));
        }
    }

    class SpeakState implements State {
        @Override
        public void enter() {
            mCurrentState = this;
            binding.btnSpeak.setEnabled(false);
            binding.btnVote.setEnabled(false);
            binding.btnRequestAi.setEnabled(true);
            binding.btnRequestAi.setText(getApplicationContext().getResources().getString(R.string.request_ai_speak));
            MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_START_SPEAK, String.valueOf(mGameRounds));
        }
    }

    class AiSpeakState implements State {
        @Override
        public void enter() {
            mCurrentState = this;
            binding.btnRequestAi.setText(getApplicationContext().getResources().getString(R.string.request_ai_speak_ing));
            binding.btnRequestAi.setEnabled(false);
            MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_REQUEST_AI_SPEAK, "");
        }
    }

    class AiSpeakOverState implements State {
        @Override
        public void enter() {
            binding.btnRequestAi.setEnabled(false);
            binding.btnVote.setEnabled(true);
            binding.btnRequestAi.setText(getApplicationContext().getResources().getString(R.string.request_ai_vote));
        }
    }


    class VoteState implements State {
        @Override
        public void enter() {
            mCurrentState = this;
            binding.btnSpeak.setEnabled(false);
            binding.btnVote.setEnabled(false);
            binding.btnRequestAi.setEnabled(true);
            MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_START_VOTE, String.valueOf(mGameRounds));
        }
    }

    class AiVoteState implements State {
        @Override
        public void enter() {
            mCurrentState = this;
            binding.btnRequestAi.setEnabled(false);
            binding.btnRequestAi.setText(getApplicationContext().getResources().getString(R.string.request_ai_vote_ing));
            MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_REQUEST_AI_SPEAK, "");
        }
    }

    class VoteResultState implements State {
        @Override
        public void enter() {
            mCurrentState = this;
            HashMap<Integer, Integer> map = new HashMap<Integer, Integer>(mGamerInfoList.size());

            int maxVote = 0;
            int voteUnderCoverNumber = 0;
            List<VoteInfo> voteInfoList = mAllRoundsVoteInfoMap.get(mGameRounds - 1);
            for (int i = 0; i < voteInfoList.size(); i++) {
                int undercoverNumber = voteInfoList.get(i).getUndercoverNumber();
                if (map.containsKey(undercoverNumber)) {
                    map.put(undercoverNumber, map.get(undercoverNumber) + 1);
                } else {
                    map.put(undercoverNumber, 1);
                }
                if (map.get(undercoverNumber) > maxVote) {
                    maxVote = map.get(undercoverNumber);
                    voteUnderCoverNumber = undercoverNumber;
                }
            }
            boolean gameOver = voteUnderCoverNumber == mGameUndercoverNumber;

            if (gameOver) {
                updateTvText(binding.gameContentTv, String.format("%s\n平民胜利，卧底失败!\n", binding.gameContentTv.getText().toString()));
                transitionTo(mEndGameState);
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
                    transitionTo(mRoundsReadyState);
                } else {
                    updateTvText(binding.gameContentTv, String.format("%s\n卧底胜利，平民失败!\n", binding.gameContentTv.getText().toString()));

                    transitionTo(mEndGameState);
                }
            }
        }
    }

    class EndGameState implements State {
        @Override
        public void enter() {
            mCurrentState = this;

            updateTvText(binding.gameContentTv, String.format("%s\n游戏结束!", binding.gameContentTv.getText().toString()));

            binding.btnStartGame.setEnabled(true);
            binding.btnEndGame.setEnabled(false);
            binding.btnRequestAi.setEnabled(false);
            MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_END_GAME, binding.gameContentTv.getText().toString());
            endGame("");

            binding.btnSpeak.setText(String.format(getApplicationContext().getResources().getString(R.string.speak), mGameRounds));
            binding.btnVote.setText(String.format(getApplicationContext().getResources().getString(R.string.vote), mGameRounds));
        }
    }

}
