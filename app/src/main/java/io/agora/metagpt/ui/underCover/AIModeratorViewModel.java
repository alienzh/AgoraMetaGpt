package io.agora.metagpt.ui.underCover;

import android.os.Process;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.agora.metagpt.MainApplication;
import io.agora.metagpt.R;
import io.agora.metagpt.chat.gpt.GptRetrofitManager;
import io.agora.metagpt.context.GameContext;
import io.agora.metagpt.context.MetaContext;
import io.agora.metagpt.inf.TtsCallback;
import io.agora.metagpt.models.DataStreamModel;
import io.agora.metagpt.models.DisplayUserInfo;
import io.agora.metagpt.models.VoteInfo;
import io.agora.metagpt.models.chat.ChatRobotMessage;
import io.agora.metagpt.models.chat.gpt.Gpt4RequestBody;
import io.agora.metagpt.models.chat.gpt.GptResponse;
import io.agora.metagpt.models.wiu.GamePrompt;
import io.agora.metagpt.models.wiu.GamerInfo;
import io.agora.metagpt.models.wiu.UserSpeakInfoModel;
import io.agora.metagpt.tts.TtsRobotManager;
import io.agora.metagpt.utils.Constants;
import io.agora.metagpt.utils.KeyCenter;
import io.agora.metagpt.utils.Utils;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class AIModeratorViewModel extends ModeratorViewModel implements TtsCallback {

    private final static String TAG = Constants.TAG + "-" + AIModeratorViewModel.class.getSimpleName();

    // ==================== ai start========================================
    private String mTempTtsPcmFilePath;

    private InputStream mInputStream;

    private String[] aiMsVoiceNameArray;
    private String mAiMsVoiceName;

    private List<ChatRobotMessage> mGptChatMessageList;

    private List<GamePrompt> mGamePromptList;

    private int mCurrentGamePromptIndex;

    private Map<Integer, UserSpeakInfoModel> mUserSpeakInfoMap;

    private TtsRobotManager mTtsRobotManager;

    @Override
    protected void initData() {
        super.initData();
        mTempTtsPcmFilePath = MainApplication.mGlobalApplication.getExternalCacheDir().getPath() + "/temp/tempTts.pcm";
        File tempTtsFile = new File(mTempTtsPcmFilePath);
        if (!tempTtsFile.exists()) {
            try {
                tempTtsFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (null == mTtsRobotManager) {
            mTtsRobotManager = new TtsRobotManager();
            mTtsRobotManager.setTtsCallback(this);
        }
        mTtsRobotManager.init(MainApplication.mGlobalApplication, mTempTtsPcmFilePath);
        String[] chatTipMessageArray = MainApplication.mGlobalApplication.getApplicationContext().getResources().getStringArray(R.array.chat_tip_message_array);
        mTtsRobotManager.setChatTipMessages(chatTipMessageArray);
        mTtsRobotManager.setAiTtsPlatformIndex(Constants.TTS_PLATFORM_MS);

        aiMsVoiceNameArray = getStringArray(R.array.ms_voice_value);
        mAiMsVoiceName = aiMsVoiceNameArray[0];

        mCurrentGamePromptIndex = 0;

        if (null == mUserSpeakInfoMap) {
            mUserSpeakInfoMap = new HashMap<>();
        } else {
            mUserSpeakInfoMap.clear();
        }

        if (null == mGptChatMessageList) {
            mGptChatMessageList = new ArrayList<>();
        } else {
            mGptChatMessageList.clear();
        }
    }

    @Override
    protected void updatePublishCustomAudioTrackChannelOptions() {
        super.updatePublishCustomAudioTrackChannelOptions();
        MetaContext.getInstance().updatePublishCustomAudioTrackChannelOptions(true,
                Constants.STT_SAMPLE_RATE,
                Constants.STT_SAMPLE_NUM_OF_CHANNEL, Constants.STT_SAMPLE_NUM_OF_CHANNEL, false, true);
    }

    @Override
    protected void onJoinSuccess(int streamId) {
        super.onJoinSuccess(streamId);
        mGamerInfoList.clear();
        sendRequestSyncUser();
        sendSelfUserInfo();
    }

    @Override
    protected void sendSelfUserInfo() {
        DisplayUserInfo displayUserInfo = new DisplayUserInfo(KeyCenter.getAiUid(), getString(R.string.ai), true);
        MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_USER_JOIN, JSONObject.toJSON(displayUserInfo).toString());
    }

    @Override
    protected boolean startWhoIsUndercover() {
        boolean result = super.startWhoIsUndercover();
        if (result) {
            if (null == mGamePromptList) {
                mGamePromptList = Arrays.asList(JSONObject.parseObject(JSON.parseObject(GameContext.getInstance().getCurrentGame().getGameExtraInfo()).getString("gamePrompt"), GamePrompt[].class));
            }
            mCurrentGamePromptIndex = 0;
            String message = "You are a helpful assistant.";
            mGptChatMessageList.add(new ChatRobotMessage(Constants.GPT_ROLE_SYSTEM, message));
            updateHistoryList(getString(R.string.moderator), "请求GPT：" + getLastChatMessage());
            mGptChatMessageList.add(new ChatRobotMessage(Constants.GPT_ROLE_USER, mGamePromptList.get(mCurrentGamePromptIndex).getContent()));
            updateHistoryList(getString(R.string.moderator), "请求GPT：" + getLastChatMessage());
            requestChatGpt();
        }
        return result;
    }

    @Override
    protected void endGame(String message) {
        super.endGame(message);
        if (null != mUserSpeakInfoMap) {
            mUserSpeakInfoMap.clear();
        }
        mCurrentGamePromptIndex = -1;
    }

    @Override
    protected void handleDataStream(int uid, DataStreamModel model) {
        super.handleDataStream(uid, model);
        if (Constants.DATA_STREAM_CMD_USER_SPEAK_INFO.equals(model.getCmd())) {
            UserSpeakInfoModel userSpeakInfoModel = JSON.parseObject(model.getMessage(), UserSpeakInfoModel.class);
            mUserSpeakInfoMap.put(uid, userSpeakInfoModel);
        }
    }

    public String getLastChatMessage() {
        if (mGptChatMessageList.isEmpty()) {
            return "";
        }
        return mGptChatMessageList.get(mGptChatMessageList.size() - 1).getContent();
    }


    // AI 发言
    @Override
    protected void requestAiSpeak() {
        super.requestAiSpeak();
        Log.d(TAG, "requestAi");
        if (mCurrentGamePromptIndex <= 4) {
            mCurrentGamePromptIndex = 5;
            if (isAiOut()) {
                //跳过发言环节
                mCurrentGamePromptIndex++;
                sendAIAnswerOverForSpeak();
            } else {
                for (Map.Entry<Integer, UserSpeakInfoModel> entry : mUserSpeakInfoMap.entrySet()) {
                    mGptChatMessageList.add(new ChatRobotMessage(Constants.GPT_ROLE_USER,
                            entry.getValue().getGamerNumber() + "号玩家：" + entry.getValue().getMessage()));
                }
            }
        } else if (mCurrentGamePromptIndex <= 8) {
            mCurrentGamePromptIndex = 8;
            if (isAiOut()) {
                mCurrentGamePromptIndex++;
                sendAIAnswerOverForVote();
            }
        }
        requestGptPrompt();
    }

    @Override
    protected void newRoundSpeak() {
        super.newRoundSpeak();
        mUserSpeakInfoMap.clear();
        if (mGameRounds > 1) {
            mCurrentGamePromptIndex = 3;
            requestGptPrompt();
        }
    }

    private void requestGptPrompt() {
        if (-1 == mCurrentGamePromptIndex || mCurrentGamePromptIndex >= mGamePromptList.size()) {
            return;
        }
        switch (mCurrentGamePromptIndex) {
            case 2:
                GamerInfo gamerInfo = _aiGamerInfo.getValue();
                if (gamerInfo != null) {
                    mGptChatMessageList.add(new ChatRobotMessage(Constants.GPT_ROLE_USER, mGamePromptList.get(mCurrentGamePromptIndex).getContent()
                            .replace("GamerNumberList1", String.valueOf(mGamerInfoList.size()))
                            .replace("GamerNumberList2", String.valueOf(mGamerInfoList.size() - 1))
                            .replace("GamerNumber", String.valueOf(gamerInfo.getGamerNumber()))
                            .replace("GamerWord", gamerInfo.getWord())));
                    updateHistoryList(getString(R.string.moderator), "请求GPT：" + getLastChatMessage());
                }
                break;
            case 3:
                mGptChatMessageList.add(new ChatRobotMessage(Constants.GPT_ROLE_USER, mGamePromptList.get(mCurrentGamePromptIndex).getContent().replace("Rounds", String.valueOf(mGameRounds))));
                updateHistoryList(getString(R.string.moderator), "请求GPT：" + getLastChatMessage());
                break;
            case 9:
                StringBuilder content = new StringBuilder();
                List<VoteInfo> voteInfoList = mAllRoundsVoteInfoMap.get(mGameRounds - 1);
                if (null != voteInfoList) {
                    for (VoteInfo voteInfo : voteInfoList) {
                        content.append(String.format(MainApplication.mGlobalApplication.getApplicationContext().getResources().getString(R.string.vote_info), voteInfo.getGamerNumber(), voteInfo.getUndercoverNumber())).append("\n");
                    }
                    mGptChatMessageList.add(new ChatRobotMessage(Constants.GPT_ROLE_USER, mGamePromptList.get(mCurrentGamePromptIndex).getContent() + content));
                    updateHistoryList(getString(R.string.moderator), "请求GPT：" + getLastChatMessage());
                }
                break;
            default:
                mGptChatMessageList.add(new ChatRobotMessage(Constants.GPT_ROLE_USER, mGamePromptList.get(mCurrentGamePromptIndex).getContent()));
                updateHistoryList(getString(R.string.moderator), "请求GPT：" + getLastChatMessage());
                break;
        }
        requestChatGpt();
    }

    private void requestChatGpt() {
        if (mGptChatMessageList.size() > 0) {
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < mGptChatMessageList.size(); i++) {
                jsonArray.add(JSON.toJSON(mGptChatMessageList.get(i)));
            }
            requestGpt4(jsonArray);
        }
    }

    private void requestGpt4(JSONArray message) {
        Gpt4RequestBody body = new Gpt4RequestBody();
        body.setModel("gpt-4");
        body.setTemperature(0.2f);
        body.setMessages(message);
        Log.d(TAG, "请求GPT:" + message.toJSONString());
        GptRetrofitManager.getInstance().getGptRequest().getGpt4Response(body)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<JSONObject>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(JSONObject response) {
                        GptResponse gptResponse = JSONObject.toJavaObject(response, GptResponse.class);
                        runOnUiThread(() -> {
//                            updateHistoryList(getString(R.string.ai), gptResponse.getAnswer());
                            Log.d(TAG, "GPT回答:" + gptResponse.getAnswer());
                            //requestTts(gptResponse.getAnswer());
                            //sendAiAnswer(gptResponse.getAnswer());
                            handleChatGptAnswer(gptResponse.getAnswer());
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "on error=" + e);
                        if (Objects.equals(e.getMessage(), "timeout")) {
                            Log.e(TAG, "GPT请求超时");
                            requestChatGpt();
                        }
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void requestTts(String message) {
        if (mTtsRobotManager != null) {
            mTtsRobotManager.requestTts(message);
        }

    }

    private void sendAiAnswer(String message) {
        DisplayUserInfo aiInfo = getUserInfo(KeyCenter.getAiUid());
        if (aiInfo != null) {
            UserSpeakInfoModel userSpeakInfoModel = new UserSpeakInfoModel();
            userSpeakInfoModel.setUid(KeyCenter.getUserUid());
            userSpeakInfoModel.setUsername(GameContext.getInstance().getUserName());
            userSpeakInfoModel.setMessage(message);
            userSpeakInfoModel.setGamerNumber(aiInfo.getNumber());
            MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_USER_SPEAK_INFO, JSONObject.toJSON(userSpeakInfoModel).toString());
        }
        updateHistoryList(getString(R.string.ai), message);
    }

    private void handleChatGptAnswer(String answer) {
        if (-1 == mCurrentGamePromptIndex) {
            return;
        }
        if (mCurrentGamePromptIndex != 4) {
            ChatRobotMessage gptAnswer = null;
            try {
                gptAnswer = JSON.parseObject(answer, ChatRobotMessage.class);
                gptAnswer.setRole(Constants.GPT_ROLE_ASSISTANT);
                mGptChatMessageList.add(gptAnswer);
            } catch (Exception e) {
                gptAnswer = new ChatRobotMessage(Constants.GPT_ROLE_ASSISTANT, answer);
                mGptChatMessageList.add(gptAnswer);
            }
            String content = gptAnswer.getContent();
            if (mCurrentGamePromptIndex == 5) { // 针对发言
                if (!content.contains("我的发言")) {
                    return;
                }
                Log.d(TAG, content);
                if (content.contains(":")) {
                    requestTts(content.substring(content.indexOf(":") + 1));
                } else if (content.contains("：")) {
                    requestTts(content.substring(content.indexOf("：") + 1));
                } else {
                    requestTts(content.substring(content.indexOf("我的发言") + 1));
                }
            } else if (mCurrentGamePromptIndex == 8) { // 针对投票
                if (!content.contains("我认为")) {
                    return;
                }
                requestTts(content.substring(content.indexOf("我认为") + 1));
                GamerInfo aiGameInfo = _aiGamerInfo.getValue();
                if (aiGameInfo != null) {
                    VoteInfo voteInfo = new VoteInfo(aiGameInfo.getGamerNumber(), Utils.getNumberFromStr(content), true);
                    addVoteInfo(voteInfo);
                    updateVoteInfo(true);
                    MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_VOTE_INFO, JSON.toJSONString(voteInfo));
                }
            }
        } else {
            return;
        }
        mCurrentGamePromptIndex++;
        if (mCurrentGamePromptIndex >= mGamePromptList.size() || mCurrentGamePromptIndex == 4 || mCurrentGamePromptIndex == 8 || mCurrentGamePromptIndex <= 0) {
            return;
        }
        requestGptPrompt();
    }

    private void pushTtsToChannel() {
        if (mOnJoinSuccess) {
            parseAndPushTtsAudio();
        } else {
            Log.e(TAG, "not yet join channel success");
        }
    }


    private void parseAndPushTtsAudio() {
        mExecutorService.execute(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
            if (null == mInputStream) {
                try {
                    mInputStream = new FileInputStream(mTempTtsPcmFilePath);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                }
            }
            long startTime = System.currentTimeMillis();
            int sentAudioFrames = 0;
            byte[] buffer;
            while (true) {
                buffer = new byte[Constants.TTS_BUFFER_BYTE_SIZE];
                try {
                    if (mInputStream.read(buffer) <= 0) {
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                int ret = MetaContext.getInstance().pushExternalAudioFrame(buffer, System.currentTimeMillis());
                Log.d(TAG, "pushExternalAudioFrame tts data:ret = " + ret);

                ++sentAudioFrames;
                long nextFrameStartTime = sentAudioFrames * Constants.TTS_BUFFER_DURATION + startTime;
                long now = System.currentTimeMillis();

                if (nextFrameStartTime > now) {
                    long sleepDuration = nextFrameStartTime - now;
                    try {
                        Thread.sleep(sleepDuration);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (mInputStream != null) {
                try {
                    mInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    mInputStream = null;
                }
            }
            runOnUiThread(() -> {
                Log.d(TAG, "tts语音推流成功！");
                if (mAiSpeaking) {
                    sendAIAnswerOverForSpeak();
                } else if (mAiVoting) {
                    sendAIAnswerOverForVote();
                }
            });
        });
    }

    @Override
    public void onTtsStart(String text, boolean isOnSpeaking) {
        updateHistoryList(getString(R.string.ai), "讯飞tts 请求：" + text);
    }

    @Override
    public void onTtsFinish(boolean isOnSpeaking) {
        updateHistoryList(getString(R.string.ai), "讯飞tts返回结果");
        pushTtsToChannel();
    }

    @Override
    public void updateTtsHistoryInfo(String message) {
    }
}
