package io.agora.metagpt.ui.activity;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.databinding.Observable;
import androidx.databinding.ObservableBoolean;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jakewharton.rxbinding2.view.RxView;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.agora.metachat.IMetachatScene;
import io.agora.metagpt.R;
import io.agora.metagpt.adapter.ChatMessageListAdapter;
import io.agora.metagpt.context.GameContext;
import io.agora.metagpt.databinding.UserActivityBinding;
import io.agora.metagpt.models.ChatMessageModel;
import io.agora.metagpt.models.DataStreamModel;
import io.agora.metagpt.models.UnityMessage;
import io.agora.metagpt.models.UserInfo;
import io.agora.metagpt.models.VoteInfo;
import io.agora.metagpt.models.wiu.UserSpeakInfoModel;
import io.agora.metagpt.models.wiu.GamerInfo;
import io.agora.metagpt.stt.xf.XFSttWsManager;
import io.agora.metagpt.ui.main.MainActivity;
import io.agora.metagpt.context.MetaContext;
import io.agora.metagpt.utils.KeyCenter;
import io.agora.metagpt.utils.Constants;
import io.agora.metagpt.utils.Utils;
import io.agora.metagpt.utils.WaveFile;
import io.agora.rtc2.DataStreamConfig;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.reactivex.disposables.Disposable;

public class UserActivity extends BaseGameActivity implements XFSttWsManager.SttCallback, View.OnClickListener {

    private final String TAG = Constants.TAG + "-" + UserActivity.class.getSimpleName();
    private UserActivityBinding binding;
    private TextureView mTextureView = null;
    private int mStreamId;

    private List<ChatMessageModel> mChatMessageDataList;

    private ChatMessageListAdapter mChatMessageListAdapter;

    private UnityMessage mUnityAudioVolumeIndicationMessage;

    private boolean mIsSpeaking;
    // for computing STT cost time;
    private boolean mFirstRecordAudio;
    private boolean mFirstSttResult;
    private long mSttStartTime;
    private WaveFile mRecordAudio;

    private static final Integer SAMPLE_RATE = 16000;
    private static final Integer SAMPLE_NUM_OF_CHANNEL = 1;
    private static final Integer SAMPLES = 640;

    private StringBuilder mSttResults;


    private boolean mSpeak;
    private boolean mVote;

    private final ObservableBoolean isEnterScene = new ObservableBoolean(false);
    private final Observable.OnPropertyChangedCallback callback =
            new Observable.OnPropertyChangedCallback() {
                @Override
                public void onPropertyChanged(Observable sender, int propertyId) {
                    if (sender == isEnterScene) {
                        binding.btnExit.setEnabled(isEnterScene.get());
                        binding.btnSpeak.setEnabled(isEnterScene.get());
                    }
                }
            };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUnityView();
        MetaContext.getInstance().setRecordingAudioFrameParameters(SAMPLE_RATE, SAMPLE_NUM_OF_CHANNEL, io.agora.rtc2.Constants.RAW_AUDIO_FRAME_OP_MODE_READ_ONLY, SAMPLES);
        MetaContext.getInstance().registerAudioFrameObserver(this);
        XFSttWsManager.getInstance().setCallback(this);
    }

    private void initUnityView() {
        mTextureView = new TextureView(this);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
                maybeCreateScene();
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
                Log.i(TAG, "onSurfaceTextureSizeChanged");
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

            }
        });
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        binding.unity.addView(mTextureView, 0, layoutParams);

    }

    private void maybeCreateScene() {
        Log.i(TAG, "maybeCreateScene");
        registerMetachat();
        registerRtc();
        createScene(mTextureView);

        resetViews();
    }

    private void resetViews() {
        binding.btnExit.setEnabled(false);
        binding.btnSpeak.setEnabled(false);
    }

    private void createScene(TextureView tv) {
        Log.i(TAG, "createScene");
        MetaContext.getInstance().createScene(this, tv);
    }


    @Override
    protected void initContentView() {
        super.initContentView();
        binding = UserActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @Override
    protected void initView() {
        binding.roomNameTv.setText(String.format(getApplicationContext().getResources().getString(R.string.room_name_label), MetaContext.getInstance().getRoomName()));
        binding.userNameTv.setText(String.format(getApplicationContext().getResources().getString(R.string.user_name_label), MetaContext.getInstance().getUserName(), KeyCenter.getUserUid()));

        addUserInfo(new UserInfo(KeyCenter.getUserUid(), MetaContext.getInstance().getUserName()));
        updateAllUserNames();

        if (mChatMessageListAdapter == null) {
            mChatMessageListAdapter = new ChatMessageListAdapter(getApplicationContext(), mChatMessageDataList);
            binding.chatMessageList.setAdapter(mChatMessageListAdapter);
            binding.chatMessageList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
            binding.chatMessageList.addItemDecoration(new ChatMessageListAdapter.SpacesItemDecoration(10));
        } else {
            int itemCount = mChatMessageDataList.size();
            mChatMessageDataList.clear();
            mChatMessageListAdapter.notifyItemRangeRemoved(0, itemCount);
        }

        binding.voteLayoutGroup.setVisibility(View.GONE);

        binding.endGameInfoTv.setMovementMethod(ScrollingMovementMethod.getInstance());
        binding.voteResultTv.setMovementMethod(ScrollingMovementMethod.getInstance());

        binding.btnSpeak.setEnabled(false);
    }

    @Override
    protected void initData() {
        super.initData();
        mStreamId = -1;
        if (null == mChatMessageDataList) {
            mChatMessageDataList = new ArrayList<>();
        } else {
            mChatMessageDataList.clear();
        }

        if (null == mUnityAudioVolumeIndicationMessage) {
            initUnityAudioMessage();
        }


        mIsSpeaking = false;
        mFirstRecordAudio = false;
        mFirstSttResult = false;
        mSttStartTime = 0;

        if (null == mSttResults) {
            mSttResults = new StringBuilder();
        } else {
            mSttResults.delete(0, mSttResults.length());
        }

    }

    private void initUnityAudioMessage() {
        mUnityAudioVolumeIndicationMessage = new UnityMessage();
        mUnityAudioVolumeIndicationMessage.setKey("talk");
        mUnityAudioVolumeIndicationMessage.setValue("");
    }

    @Override
    protected void initListener() {
        super.initListener();
        isEnterScene.addOnPropertyChangedCallback(callback);
    }

    @Override
    protected void initClickEvent() {
        super.initClickEvent();

        Disposable disposable;
        disposable = RxView.clicks(binding.btnExit).throttleFirst(1, TimeUnit.SECONDS).subscribe(o -> {
            exit();
        });
        compositeDisposable.add(disposable);

        disposable = RxView.clicks(binding.btnSpeak).throttleFirst(1, TimeUnit.SECONDS).subscribe(o -> {
            if (mIsSpeaking) {
                mIsSpeaking = false;
                XFSttWsManager.getInstance().stt("{\"end\": true}".getBytes());
                mRecordAudio.close();
                binding.btnSpeak.setText(R.string.start_speak);
                MetaContext.getInstance().updateRoleSpeak(false);
            } else {
                mIsSpeaking = true;
                mFirstRecordAudio = true;
                mSttResults.delete(0, mSttResults.length());
                binding.btnSpeak.setText(R.string.end_speak);
                MetaContext.getInstance().updateRoleSpeak(true);
            }
        });
        compositeDisposable.add(disposable);
    }

    private void exit() {
        MetaContext.getInstance().leaveScene();
        XFSttWsManager.getInstance().close();
    }

    @Override

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        maybeCreateScene();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        isEnterScene.removeOnPropertyChangedCallback(callback);
    }


    @Override
    public void onEnterSceneResult(int errorCode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                isEnterScene.set(true);
            }
        });
    }

    @Override
    public void onLeaveSceneResult(int errorCode) {
        runOnUiThread(() -> {
            isEnterScene.set(false);
        });
    }

    @Override
    public void onReleasedScene(int status) {
        if (status == 0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MetaContext.getInstance().destroy();
                    initData();
                }
            });
            unregister();

            Intent intent = new Intent(UserActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }
    }

    @Override
    public void onCreateSceneResult(IMetachatScene scene, int errorCode) {
        if (errorCode == 0) {
            //异步线程回调需在主线程处理
            runOnUiThread(() -> MetaContext.getInstance().enterScene());
        }
    }

    @Override
    public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
        if (-1 == mStreamId) {
            DataStreamConfig cfg = new DataStreamConfig();
            cfg.syncWithAudio = false;
            cfg.ordered = true;
            mStreamId = MetaContext.getInstance().createDataStream(cfg);
        }
        MetaContext.getInstance().updateAutoSubscribeAudio(true);
        MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_REQUEST_SYNC_USER, "");
        sendSelfUserInfo();
    }

    private void sendSelfUserInfo() {
        MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_USER_JOIN, JSONObject.toJSON(new UserInfo(KeyCenter.getUserUid(), GameContext.getInstance().getUserName())).toString());
    }

    @Override
    public void onLeaveChannel(IRtcEngineEventHandler.RtcStats stats) {
    }

    @Override
    public void onAudioVolumeIndication(IRtcEngineEventHandler.AudioVolumeInfo[] speakers, int totalVolume) {
        if (totalVolume > 0) {
            //Log.i(TAG, "onAudioVolumeIndication，totalVolume:" + totalVolume);
            //MetaContext.getInstance().sendSceneMessage(JSONObject.toJSONString(mUnityAudioVolumeIndicationMessage));
        }
    }

    @Override
    public void onUserJoined(int uid, int elapsed) {

    }

    @Override
    public void onUserOffline(int uid, int reason) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                UserInfo userInfo = getUserInfo(uid);
                if (null != userInfo) {
                    removeUserInfo(userInfo);
                    updateAllUserNames();
                }
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
    }

    @Override
    protected void handleDataStream(int uid, DataStreamModel model) {
        super.handleDataStream(uid, model);
        if (Constants.DATA_STREAM_CMD_REQUEST_SYNC_USER.equals(model.getCmd())) {
            sendSelfUserInfo();
        } else if (Constants.DATA_STREAM_CMD_USER_JOIN.equals(model.getCmd())) {
            UserInfo userInfo = JSONObject.parseObject(model.getMessage(), UserInfo.class);
            addUserInfo(userInfo);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateAllUserNames();
                }
            });
        } else if (Constants.DATA_STREAM_CMD_AI_ANSWER.equals(model.getCmd())) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateChatMessageList(getApplicationContext().getResources().getString(R.string.ai), uid, model.getMessage(), true);
                }
            });
        } else if (Constants.DATA_STREAM_CMD_START_SPEAK.equals(model.getCmd())) {
            mSpeak = true;
            mVote = false;
        } else if (Constants.DATA_STREAM_CMD_START_VOTE.equals(model.getCmd())) {
            if (!mSelfGamerInfo.isOut()) {
                mSpeak = false;
                mVote = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showVoteView();
                    }
                });
            }
        } else if (Constants.DATA_STREAM_CMD_USER_SPEAK_INFO.equals(model.getCmd())) {
            UserSpeakInfoModel userSpeakInfoModel = JSON.parseObject(model.getMessage(), UserSpeakInfoModel.class);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateChatMessageList(userSpeakInfoModel.getUsername(), userSpeakInfoModel.getUid(), userSpeakInfoModel.getMessage(), false);
                }
            });
        }
    }

    private void updateAllUserNames() {
        binding.allUserTv.setText(String.format(getApplicationContext().getResources().getString(R.string.all_user_name_label), getAllUserInfoTxt()));
    }

    @Override
    public boolean onRecordAudioFrame(String channelId, int type, int samplesPerChannel, int bytesPerSample, int channels, int samplesPerSec, ByteBuffer buffer, long renderTimeMs, int avsync_type) {
        int length = buffer.remaining();
        byte[] origin = new byte[length];
        buffer.get(origin);
        buffer.flip();
        Log.i(TAG, "onRecordAudioFrame, ");
        if (mFirstRecordAudio) {
            mFirstRecordAudio = false;
            mFirstSttResult = true;
            mSttStartTime = System.currentTimeMillis();
            String wavPath = getApplication().getExternalCacheDir().getPath() + "/recordAudio_" + Long.toString(mSttStartTime) + ".wav";
            mRecordAudio = new WaveFile(16000, (short) 16, (short)1, wavPath);
        }
        if (mIsSpeaking) {
            mRecordAudio.write(origin);
        }
        XFSttWsManager.getInstance().stt(origin);
        return false;
    }

    @Override
    public void onSttResult(String text, boolean isFinish) {
        final boolean isNewLine = mSttResults.length() == 0;
        boolean prefixFirstSttTime = false;
        long firstSttTime = 0;
        long sttEndTime = 0;
        if (mFirstSttResult) {
            mFirstSttResult = false;
            prefixFirstSttTime = true;
            firstSttTime = System.currentTimeMillis() - mSttStartTime;
        }
        if (isFinish) {
            sttEndTime = System.currentTimeMillis() - mSttStartTime;
        }
        final boolean cPrefixFirstSttTime = prefixFirstSttTime;
        final long cFirstSttTime = firstSttTime;
        final long cSttEndTime = sttEndTime;

        if (isFinish) {
            XFSttWsManager.getInstance().close();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSttResults.append(text);
                String displayMsg = text;
                if (cPrefixFirstSttTime) {
                    displayMsg = "(firstSttTime: " + Long.toString(cFirstSttTime) + "ms)" + displayMsg;
                }
                if (isFinish) {
                    displayMsg = displayMsg + "(" + Long.toString(cSttEndTime) + "ms)";
                }
                updateSelfSpeakChatMessage(displayMsg, isNewLine);
                if (isFinish) {
                    if (mSpeak && !mSelfGamerInfo.isOut()) {
                        sendUserSpeakInfo();
                    }
                    if (mVote && !mSelfGamerInfo.isOut()) {
                        int votedId = Utils.getNumberFromStr(mSttResults.toString());
                        sendUserVoteInfo(votedId);
                    }
                }
            }
        });
    }

    private synchronized void updateChatMessageList(String userName, int uid, String message, boolean isAiMessage) {
        if (null != mChatMessageListAdapter) {
            ChatMessageModel chatMessageModel = new ChatMessageModel();
            chatMessageModel.setUsername(userName + "(" + uid + ")");
            chatMessageModel.setMessage(message);
            chatMessageModel.setAiMessage(isAiMessage);

            mChatMessageDataList.add(chatMessageModel);
            mChatMessageListAdapter.notifyItemInserted(mChatMessageDataList.size() - 1);

            scrollChatMessageListToBottom();
        }
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
                chatMessageModel.setAiMessage(false);
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
            chatMessageModel.setAiMessage(false);
            mChatMessageDataList.add(chatMessageModel);
        }
        if (position != -1) {
            mChatMessageListAdapter.notifyItemChanged(position);

            scrollChatMessageListToBottom();
        }
    }

    private void scrollChatMessageListToBottom() {
        final int bottomPosition = mChatMessageListAdapter.getDataList().size() - 1;
        binding.chatMessageList.scrollToPosition(bottomPosition);
        binding.chatMessageList.post(new Runnable() {
            @Override
            public void run() {
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) binding.chatMessageList.getLayoutManager();
                if (null != linearLayoutManager) {
                    View target = linearLayoutManager.findViewByPosition(bottomPosition);
                    if (target != null) {
                        linearLayoutManager.scrollToPositionWithOffset(bottomPosition,
                                binding.chatMessageList.getMeasuredHeight() - target.getMeasuredHeight());
                    }
                }
            }
        });
    }

    public void sendUserSpeakInfo() {
        UserSpeakInfoModel userSpeakInfoModel = new UserSpeakInfoModel();
        userSpeakInfoModel.setUid(KeyCenter.getUserUid());
        userSpeakInfoModel.setUsername(GameContext.getInstance().getUserName());
        userSpeakInfoModel.setMessage(mSttResults.toString());
        userSpeakInfoModel.setGamerNumber(mSelfGamerInfo.getGamerNumber());
        MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_USER_SPEAK_INFO, JSONObject.toJSON(userSpeakInfoModel).toString());
    }

    public void sendUserVoteInfo(int votedId) {
        VoteInfo voteInfo = new VoteInfo();
        voteInfo.setAiVote(false);
        voteInfo.setGamerNumber(mSelfGamerInfo.getGamerNumber());
        voteInfo.setUndercoverNumber(votedId);
        addVoteInfo(voteInfo);
        updateVoteInfo(false);
        MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_VOTE_INFO, JSONObject.toJSON(voteInfo).toString());
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

    @Override
    protected void updateGamerInfo() {
        for (GamerInfo gamerInfo : mGamerInfoList) {
            if (gamerInfo.getUid() == KeyCenter.getUserUid()) {
                runOnUiThread(new Runnable() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void run() {
                        mSelfGamerInfo = gamerInfo;
                        StringBuilder gameInfo = new StringBuilder();
                        if (mSelfGamerInfo.isOut()) {
                            gameInfo.append("您已淘汰").append("\n");
                        }
                        gameInfo.append("游戏名称：").append(mSelfGamerInfo.getGameName()).append("，玩家编号:").append(mSelfGamerInfo.getGamerNumber()).append(",关键词是：").append(mSelfGamerInfo.getWord());
                        binding.gameInfoTv.setText(gameInfo.toString());
                        binding.btnSpeak.setEnabled(true);
                    }
                });
                break;
            }
        }
    }

    @Override
    protected void startGame() {
        updateTvText(binding.voteResultTv, "");
        binding.endGameInfoTv.setVisibility(View.GONE);
        updateTvText(binding.endGameInfoTv, "");
    }

    private void showVoteView() {
        binding.voteLayoutGroup.setVisibility(View.VISIBLE);
        Button button;
        for (GamerInfo gamerInfo : mGamerInfoList) {
            if (gamerInfo.getUid() == KeyCenter.getUserUid()) {
                continue;
            }
            button = new Button(this);
            button.setId(gamerInfo.getUid());
            button.setText(String.format("%d号玩家(%s)", gamerInfo.getGamerNumber(), gamerInfo.getUserName()));
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            button.setLayoutParams(layoutParams);
            button.setOnClickListener(this);
            binding.voteLayout.addView(button);
        }
    }

    @Override
    public void onClick(View v) {
        int voteUid = v.getId();
        for (GamerInfo gamerInfo : mGamerInfoList) {
            if (voteUid == gamerInfo.getUid()) {
                sendUserVoteInfo(gamerInfo.getGamerNumber());
                binding.voteLayout.removeAllViews();
                binding.voteLayoutGroup.setVisibility(View.GONE);
                return;
            }
        }
    }

    @Override
    protected void endGame(String message) {
        super.endGame(message);
        binding.endGameInfoTv.setVisibility(View.VISIBLE);
        updateTvText(binding.endGameInfoTv, message);
    }
}
