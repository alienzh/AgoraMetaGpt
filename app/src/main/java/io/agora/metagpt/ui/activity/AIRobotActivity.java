package io.agora.metagpt.ui.activity;


import android.annotation.SuppressLint;
import android.content.Intent;

import android.os.Bundle;
import android.os.Process;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.elvishew.xlog.XLog;
import com.jakewharton.rxbinding2.view.RxView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.agora.metagpt.BuildConfig;
import io.agora.metagpt.R;
import io.agora.metagpt.adapter.HistoryListAdapter;
import io.agora.metagpt.ai.ChatRobot;
import io.agora.metagpt.ai.gpt.Gpt4RequestBody;
import io.agora.metagpt.context.GameContext;
import io.agora.metagpt.databinding.AiRobotActivityBinding;
import io.agora.metagpt.context.MetaContext;
import io.agora.metagpt.models.DataStreamModel;
import io.agora.metagpt.models.GameInfo;
import io.agora.metagpt.models.HistoryModel;
import io.agora.metagpt.ai.gpt.GptRequestBody;
import io.agora.metagpt.ai.gpt.GptResponse;
import io.agora.metagpt.ai.minimax.Message;
import io.agora.metagpt.ai.minimax.MinimaxChatCompletionRequestBody;
import io.agora.metagpt.ai.minimax.MinimaxCompletionRequestBody;
import io.agora.metagpt.ai.minimax.RoleMeta;
import io.agora.metagpt.ai.gpt.GptRetrofitManager;
import io.agora.metagpt.ai.minimax.MiniMaxRetrofitManager;
import io.agora.metagpt.models.UserInfo;
import io.agora.metagpt.models.VoteInfo;
import io.agora.metagpt.models.gpt.ChatMessage;
import io.agora.metagpt.models.wiu.GamePrompt;
import io.agora.metagpt.models.wiu.GamerInfo;
import io.agora.metagpt.models.wiu.UserSpeakInfoModel;
import io.agora.metagpt.tts.ms.MsTtsRetrofitManager;
import io.agora.metagpt.tts.xf.XFTtsWsManager;
import io.agora.metagpt.ui.main.MainActivity;
import io.agora.metagpt.utils.KeyCenter;
import io.agora.metagpt.utils.Constants;
import io.agora.metagpt.utils.Utils;
import io.agora.metagpt.utils.WaveFile;
import io.agora.metagpt.utils.WaveHeader;
import io.agora.rtc2.DataStreamConfig;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;


public class AIRobotActivity extends BaseGameActivity implements XFTtsWsManager.TtsCallback, ChatRobot.ChatRobotCallback {
    private final static String TAG = Constants.TAG + "-" + AIRobotActivity.class.getSimpleName();
    private AiRobotActivityBinding binding;
    private SimpleDateFormat mSdf;

    private boolean mJoinChannelSuccess;

    private String mTempTtsPcmFilePath;
    private ExecutorService mExecutorService;

    private InputStream mInputStream;

    private int mStreamId;

    private List<HistoryModel> mHistoryDataList;

    private HistoryListAdapter mAiHistoryListAdapter;

    private int mAiPlatformIndex;

    private int mAiTtsPlatformIndex;
    private String mAiMsVoiceName;

    private String[] mTestTtsCases;
    private int mCurTtsCaseIndex;

    /**
     * 讯飞默认采样率为16000，如需修改同步修改讯飞在线语音合成参数
     */
    public static final int SAMPLE_RATE = 16000;
    public static final int SAMPLE_NUM_OF_CHANNEL = 1;
    public static final int BITS_PER_SAMPLE = 16;

    private static final float BYTE_PER_SAMPLE = 1.0f * BITS_PER_SAMPLE / 8 * SAMPLE_NUM_OF_CHANNEL;
    private static final float DURATION_PER_SAMPLE = 1000.0f / SAMPLE_RATE;
    private static final float SAMPLE_COUNT_PER_MS = SAMPLE_RATE * 1.0f / 1000;
    private static final int BUFFER_SAMPLE_COUNT = (int) (SAMPLE_COUNT_PER_MS * 20);
    private static final int BUFFER_BYTE_SIZE = (int) (BUFFER_SAMPLE_COUNT * BYTE_PER_SAMPLE);
    private static final long BUFFER_DURATION = (long) (BUFFER_SAMPLE_COUNT * DURATION_PER_SAMPLE);

    // private List<ChatMessage> mGptChatMessageList;
    private ChatRobot mChatRobot;

    private List<GamePrompt> mGamePromptList;

    private int mCurrentGamePromptIndex;

    private Map<Integer, UserSpeakInfoModel> mUserSpeakInfoMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initRtc();
    }

    @Override
    protected void initContentView() {
        super.initContentView();
        binding = AiRobotActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @Override
    protected void initData() {
        super.initData();
        mTempTtsPcmFilePath = getApplication().getExternalCacheDir().getPath() + "/tempTts.pcm";
        File tempTtsFile = new File(mTempTtsPcmFilePath);
        if (!tempTtsFile.exists()) {
            try {
                tempTtsFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (null == mSdf) {
            mSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        }
        XFTtsWsManager.getInstance().initWebSocketClient(mTempTtsPcmFilePath);
        XFTtsWsManager.getInstance().setCallback(this);

        try {
            String jsonStr = Utils.getFromAssets(this, "test_tts.json");
            JSONObject jsonObject = JSON.parseObject(jsonStr);
            mTestTtsCases = JSON.parseObject(jsonObject.getJSONArray("tests").toJSONString(), String[].class);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (null == mExecutorService) {
            mExecutorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                    60L, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        }
        mStreamId = -1;
        if (null == mHistoryDataList) {
            mHistoryDataList = new ArrayList<>();
        } else {
            mHistoryDataList.clear();
        }

        mAiPlatformIndex = this.getResources().getIntArray(R.array.ai_platform_index)[0];

        mAiTtsPlatformIndex = Constants.TTS_PLATFORM_MS;

        mAiMsVoiceName = this.getResources().getStringArray(R.array.ms_voice_value)[0];

        mCurrentGamePromptIndex = 0;

        if (null == mUserSpeakInfoMap) {
            mUserSpeakInfoMap = new HashMap<>();
        } else {
            mUserSpeakInfoMap.clear();
        }

        if (null == mChatRobot) {
            mChatRobot = new ChatRobot(mAiPlatformIndex, this);
        } else {
            mChatRobot.clearChatMessage();
        }
    }

    @Override
    protected void initView() {
//        binding.aiPlatformTv.setVisibility(View.GONE);
//        binding.aiPlatformSpinner.setVisibility(View.GONE);

        binding.roomNameTv.setText(String.format(getApplicationContext().getResources().getString(R.string.room_name_label), MetaContext.getInstance().getRoomName()));
        binding.userNameTv.setText(String.format(getApplicationContext().getResources().getString(R.string.user_name_label), MetaContext.getInstance().getUserName(), KeyCenter.getAiUid()));

        addUserInfo(new UserInfo(KeyCenter.getAiUid(), MetaContext.getInstance().getUserName()));
        updateAllUserNames();


        if (mAiHistoryListAdapter == null) {
            mAiHistoryListAdapter = new HistoryListAdapter(getApplicationContext(), mHistoryDataList);
            binding.aiHistoryList.setAdapter(mAiHistoryListAdapter);
            binding.aiHistoryList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
            binding.aiHistoryList.addItemDecoration(new HistoryListAdapter.SpacesItemDecoration(10));
        } else {
            int itemCount = mHistoryDataList.size();
            mHistoryDataList.clear();
            mAiHistoryListAdapter.notifyItemRangeRemoved(0, itemCount);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, this.getResources().getStringArray(R.array.ai_platform_name));
        binding.aiPlatformSpinner.setAdapter(adapter);
        binding.aiPlatformSpinner.setSelection(mAiPlatformIndex);
        binding.aiPlatformSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mAiPlatformIndex = AIRobotActivity.this.getResources().getIntArray(R.array.ai_platform_index)[position];
                if (mChatRobot != null) {
                    mChatRobot.setAiPlatform(mAiPlatformIndex);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ArrayAdapter<String> aiVoiceNameAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, this.getResources().getStringArray(R.array.ms_voice_name));
        binding.aiVoiceNameSpinner.setAdapter(aiVoiceNameAdapter);
        binding.aiVoiceNameSpinner.setSelection(0);
        binding.aiVoiceNameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mAiMsVoiceName = AIRobotActivity.this.getResources().getStringArray(R.array.ms_voice_value)[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        binding.endGameInfoTv.setMovementMethod(ScrollingMovementMethod.getInstance());
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
        disposable = RxView.clicks(binding.testTts).throttleFirst(1, TimeUnit.SECONDS).subscribe(o -> {
            testTts();
        });
        compositeDisposable.add(disposable);
    }

    private void initRtc() {
        mJoinChannelSuccess = false;
        if (MetaContext.getInstance().initializeRtc(this.getApplicationContext())) {
            registerRtc();
            MetaContext.getInstance().updatePublishCustomAudioTrackChannelOptions(true, SAMPLE_RATE, SAMPLE_NUM_OF_CHANNEL, SAMPLE_NUM_OF_CHANNEL, false, true);
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
    }

    private void sendSelfUserInfo() {
        MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_USER_JOIN, JSONObject.toJSON(new UserInfo(KeyCenter.getAiUid(), getApplication().getResources().getString(R.string.ai))).toString());
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
                    removeUserInfo(userInfo);
                    updateAllUserNames();
                }
            }
        });
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
        } else if (Constants.DATA_STREAM_CMD_REQUEST_AI_SPEAK.equals(model.getCmd())) {
            requestAiSpeak();
        } else if (Constants.DATA_STREAM_CMD_USER_SPEAK_INFO.equals(model.getCmd())) {
            UserSpeakInfoModel userSpeakInfoModel = JSON.parseObject(model.getMessage(), UserSpeakInfoModel.class);
            updateHistoryList("收到" + userSpeakInfoModel.getUsername() + "的发言：" + userSpeakInfoModel.getMessage());
            mUserSpeakInfoMap.put(uid, userSpeakInfoModel);

            mChatRobot.appendChatMessage(Constants.GPT_ROLE_USER, userSpeakInfoModel.getGamerNumber() + "号玩家：" + userSpeakInfoModel.getMessage());
            requestChatGpt();
        } else if (Constants.DATA_STREAM_CMD_START_SPEAK.equals(model.getCmd())) {
            //第一轮发送的的gpt请求可能会有影响
            mUserSpeakInfoMap.clear();
            if (mGameRounds > 1) {
                mCurrentGamePromptIndex = 3;
                requestGptPrompt();
            }
        }
    }

    private void requestChatGpt() {
        updateHistoryList("请求GPT：" + mChatRobot.getLastChatMessage());
        mChatRobot.requestChatGpt();
    }

    @Override
    public void onAiAnswer(int code, String answer, long costTime) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (code == 0) {
                    updateHistoryList("GPT回答("+ costTime +"ms):" + answer);
                    handleChatGptAnswer(answer);
                } else {
                    if (Objects.equals(answer, "timeout")) {
                        updateHistoryList("GPT请求超时("+ costTime +"ms)");
                        requestChatGpt();
                    }
                }
            }
        });
    }

    private void requestAiSpeak() {
        Log.i(TAG, "requestAi");
        if (mCurrentGamePromptIndex <= 4) {
            mCurrentGamePromptIndex = 5;
            if (mSelfGamerInfo.isOut()) {
                //跳过发言环节
                mCurrentGamePromptIndex++;
                MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_AI_ANSWER_OVER, "");
            } /*else {
                for (Map.Entry<Integer, UserSpeakInfoModel> entry : mUserSpeakInfoMap.entrySet()) {
                    mChatRobot.appendChatMessage(Constants.GPT_ROLE_USER, entry.getValue().getGamerNumber() + "号玩家：" + entry.getValue().getMessage());
                }
            }*/
        } else if (mCurrentGamePromptIndex <= 8) {
            mCurrentGamePromptIndex = 8;
            if (mSelfGamerInfo.isOut()) {
                mCurrentGamePromptIndex++;
                MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_AI_ANSWER_OVER, "");
            }
        }
        requestGptPrompt();
    }

//    private void requestAiPlatform(String message, String statement) {
//        switch (mAiPlatformIndex) {
//            case Constants.AI_PLATFORM_CHAT_GPT:
//                if (TextUtils.isEmpty(statement)) {
//                    requestGpt(new String[]{message});
//                } else {
//                    requestGpt(new String[]{statement + "\"" + message + "\""});
//                }
//                break;
//            case Constants.AI_PLATFORM_MINIMAX_COMPLETION5:
//                if (TextUtils.isEmpty(statement)) {
//                    requestMinimaxCompletion5(message);
//                } else {
//                    requestMinimaxCompletion5(statement + "\"" + message + "\"");
//                }
//
//                break;
//            case Constants.AI_PLATFORM_MINIMAX_CHAT_COMPLETION4:
//                if (TextUtils.isEmpty(statement)) {
//                    // requestMinimaxChatCompletion4(message);
//                } else {
//                    // requestMinimaxChatCompletion4(statement + "\"" + message + "\"");
//                }
//                break;
//            case Constants.AI_PLATFORM_MINIMAX_CHAT_COMPLETION5:
//                if (TextUtils.isEmpty(statement)) {
//                    // requestMinimaxChatCompletion5(message);
//                } else {
//                    // requestMinimaxChatCompletion5(statement + "\"" + message + "\"");
//                }
//                break;
//            default:
//                requestGpt(new String[]{message});
//                break;
//        }
//    }

    private void goBackHome() {
        Intent intent = new Intent(AIRobotActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
        finish();
    }

    private void exit() {
        XFTtsWsManager.getInstance().close();
        MetaContext.getInstance().leaveRtcChannel();
    }

    private void testTts() {
        requestMsTts1(0, mTestTtsCases[0]);
        mCurTtsCaseIndex = 0;
        XFTtsWsManager.getInstance().tts(mTestTtsCases[mCurTtsCaseIndex]);
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

    private void requestTts(String message) {
        switch (mAiTtsPlatformIndex) {
            case Constants.TTS_PLATFORM_XF:
                XFTtsWsManager.getInstance().tts(message);
                return;
            case Constants.TTS_PLATFORM_MS:
                requestMsTts(message);
                break;
            default:
                break;
        }

    }

    private void requestMsTts(String message) {
        String requestStr = "<speak version='1.0' xml:lang='zh-CN'>" +
                "<voice xml:lang='zh-CN' xml:gender='Female' name='" + mAiMsVoiceName + "'>" +
                message +
                "</voice>" +
                "</speak>";

        RequestBody requestBody = RequestBody.create(requestStr, MediaType.parse("application/ssml+xml"));

        long curTime = System.currentTimeMillis();
        updateHistoryList("微软tts请求：" + message);
        MsTtsRetrofitManager.getInstance().getTtsRequest().getTtsResponse(requestStr)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(ResponseBody response) {
                        try {
                            InputStream is = response.byteStream();
                            File file = new File(mTempTtsPcmFilePath);
                            OutputStream os = new FileOutputStream(file);
                            byte[] buffer = new byte[1024];
                            int read;
                            while ((read = is.read(buffer)) != -1) {
                                os.write(buffer, 0, read);
                            }
                            os.close();
                            is.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        updateHistoryList("微软tts返回结果(" + Long.toString(System.currentTimeMillis() - curTime) + "ms)");
                        pushTtsToChannel();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i(TAG, "on error=" + e);
                        if (Objects.equals(e.getMessage(), "timeout")) {
                            updateHistoryList("Minimax请求超时");
                        }
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void requestMsTts1(int caseNo, String message) {
        String requestStr = "<speak version='1.0' xml:lang='zh-CN'>" +
            "<voice xml:lang='zh-CN' xml:gender='Female' name='" + mAiMsVoiceName + "'>" +
            message +
            "</voice>" +
            "</speak>";

        RequestBody requestBody = RequestBody.create(requestStr, MediaType.parse("application/ssml+xml"));

        updateHistoryList("微软tts请求：" + message);
        MsTtsRetrofitManager.getInstance().getTtsRequest().getTtsResponse(requestStr)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<ResponseBody>() {
                @Override
                public void onSubscribe(Disposable d) {

                }

                @Override
                public void onNext(ResponseBody response) {
                    try {
                        String outWav = getApplication().getExternalCacheDir().getPath() + "/ms_" + Integer.toString(caseNo) + "_16khz_16bit_mono.wav";
                        InputStream is = response.byteStream();
                        WaveFile.savePcmToWav(is, outWav);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    updateHistoryList("微软tts返回结果");
                    if (caseNo + 1 < mTestTtsCases.length) {
                        requestMsTts1(caseNo + 1, mTestTtsCases[caseNo + 1]);
                    }
                }

                @Override
                public void onError(Throwable e) {
                    Log.i(TAG, "on error=" + e);
                    if (Objects.equals(e.getMessage(), "timeout")) {
                        updateHistoryList("微软tts请求超时");
                    }
                }

                @Override
                public void onComplete() {

                }
            });
    }

    private void sendAiAnswer(String message) {
        MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_AI_ANSWER, message);
    }

    private void updateHistoryList(String newMessage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String newLine = "[" + mSdf.format(System.currentTimeMillis()) + "]  " + newMessage;
                XLog.i(newLine);

                HistoryModel aiHistoryModel = new HistoryModel();
                aiHistoryModel.setMessage(newLine);
                mHistoryDataList.add(aiHistoryModel);
                if (null != mAiHistoryListAdapter) {
                    mAiHistoryListAdapter.notifyItemInserted(mHistoryDataList.size() - 1);
                    binding.aiHistoryList.scrollToPosition(mAiHistoryListAdapter.getDataList().size() - 1);
                }
            }
        });
    }

    @Override
    public void onTtsStart(String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateHistoryList("讯飞tts请求：" + text);
            }
        });
    }

    @Override
    public void onTtsFinish() {
        updateHistoryList("讯飞tts返回结果");
        // pushTtsToChannel();
        try {
            InputStream is = new FileInputStream(mTempTtsPcmFilePath);
            String outWav = getApplication().getExternalCacheDir().getPath() + "/xf_" + Integer.toString(mCurTtsCaseIndex) + "_16khz_16bit_mono.wav";
            WaveFile.savePcmToWav(is, outWav);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        mCurTtsCaseIndex++;
        if (mCurTtsCaseIndex < mTestTtsCases.length) {
            XFTtsWsManager.getInstance().tts(mTestTtsCases[mCurTtsCaseIndex]);
        }
    }

    private void pushTtsToChannel() {
        if (mJoinChannelSuccess) {
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
                buffer = new byte[BUFFER_BYTE_SIZE];
                try {
                    if (mInputStream.read(buffer) <= 0) {
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                int ret = MetaContext.getInstance().pushExternalAudioFrame(buffer, System.currentTimeMillis());
                Log.i(TAG, "pushExternalAudioFrame tts data:ret = " + ret);

                ++sentAudioFrames;
                long nextFrameStartTime = sentAudioFrames * BUFFER_DURATION + startTime;
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
                updateHistoryList("tts语音推流成功！");
                MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_AI_ANSWER_OVER, "");
            });

        });
    }

    private void updateAllUserNames() {
        binding.allUserTv.setText(String.format(getApplicationContext().getResources().getString(R.string.all_user_name_label), getAllUserInfoTxt()));
    }

    @Override
    protected void startGame() {
        super.startGame();
        updateTvText(binding.voteResultTv, "");
        binding.endGameInfoTv.setVisibility(View.GONE);
        updateTvText(binding.endGameInfoTv, "");
        switch (GameContext.getInstance().getCurrentGame().getGameId()) {
            case Constants.GAME_WHO_IS_UNDERCOVER:
                startWhoIsUndercover();
                break;
            default:
                break;
        }
    }

    private void startWhoIsUndercover() {
        if (null == mGamePromptList) {
            mGamePromptList = Arrays.asList(JSONObject.parseObject(JSON.parseObject(GameContext.getInstance().getCurrentGame().getGameExtraInfo()).getString("gamePrompt"), GamePrompt[].class));
        }
        mCurrentGamePromptIndex = 0;
        mChatRobot.appendChatMessage(Constants.GPT_ROLE_SYSTEM, "You are a helpful assistant.");
        mChatRobot.appendChatMessage(Constants.GPT_ROLE_USER, mGamePromptList.get(mCurrentGamePromptIndex).getContent());
        requestChatGpt();
    }

    private void handleChatGptAnswer(String answer) {
        if (-1 == mCurrentGamePromptIndex) {
            return;
        }
        if (mCurrentGamePromptIndex != 4) {
            mChatRobot.appendChatMessage(Constants.GPT_ROLE_ASSISTANT, answer);
            if (mCurrentGamePromptIndex == 5) {
                if (!answer.contains("我的发言")) {
                    return;
                }
                requestMsTts(answer.substring(answer.indexOf(":") + 1));
                sendAiAnswer(answer.substring(answer.indexOf(":") + 1));
            } else if (mCurrentGamePromptIndex == 8) {
                if (!answer.contains("我认为")) {
                    return;
                }
                requestMsTts(answer);
                VoteInfo voteInfo = new VoteInfo(mSelfGamerInfo.getGamerNumber(), Utils.getNumberFromStr(answer), true);
                addVoteInfo(voteInfo);
                updateVoteInfo(true);
                MetaContext.getInstance().sendDataStreamMessage(mStreamId, Constants.DATA_STREAM_CMD_VOTE_INFO, JSON.toJSONString(voteInfo));
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

    private void requestGptPrompt() {
        if (-1 == mCurrentGamePromptIndex) {
            return;
        }
        switch (mCurrentGamePromptIndex) {
            case 2:
                mChatRobot.appendChatMessage(Constants.GPT_ROLE_USER, mGamePromptList.get(mCurrentGamePromptIndex).getContent().replace("GamerNumber", String.valueOf(mSelfGamerInfo.getGamerNumber())).replace("GamerWord", mSelfGamerInfo.getWord()));
                break;
            case 3:
                mChatRobot.appendChatMessage(Constants.GPT_ROLE_USER, mGamePromptList.get(mCurrentGamePromptIndex).getContent().replace("Rounds", String.valueOf(mGameRounds)));
                break;
            case 9:
                StringBuilder content = new StringBuilder();
                List<VoteInfo> voteInfoList = mAllRoundsVoteInfoMap.get(mGameRounds - 1);
                if (null != voteInfoList) {
                    for (VoteInfo voteInfo : voteInfoList) {
                        content.append(String.format(getApplicationContext().getResources().getString(R.string.vote_info), voteInfo.getGamerNumber(), voteInfo.getUndercoverNumber())).append("\n");
                    }
                    mChatRobot.appendChatMessage(Constants.GPT_ROLE_USER, mGamePromptList.get(mCurrentGamePromptIndex).getContent() + content.toString());
                }
                break;
            default:
                mChatRobot.appendChatMessage(Constants.GPT_ROLE_USER, mGamePromptList.get(mCurrentGamePromptIndex).getContent());
                break;
        }
        requestChatGpt();
    }

    @Override
    protected void endGame(String message) {
        super.endGame(message);
        mCurrentGamePromptIndex = -1;
        mChatRobot.clearChatMessage();
        binding.endGameInfoTv.setVisibility(View.VISIBLE);
        updateTvText(binding.endGameInfoTv, message);
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
            if (gamerInfo.getUid() == KeyCenter.getAiUid()) {
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
                    }
                });
                break;
            }
        }
    }
}
