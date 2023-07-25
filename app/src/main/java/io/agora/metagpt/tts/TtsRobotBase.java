package io.agora.metagpt.tts;

import android.media.AudioFormat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.agora.metagpt.inf.ITtsRobot;
import io.agora.metagpt.inf.TtsCallback;
import io.agora.metagpt.models.ResponsePendingData;
import io.agora.metagpt.net.HttpURLRequest;
import io.agora.metagpt.utils.AndroidPcmPlayer;
import io.agora.metagpt.utils.Config;
import io.agora.metagpt.utils.Constants;
import io.agora.metagpt.utils.RingBuffer;

public class TtsRobotBase implements ITtsRobot {
    protected String mVoiceName;
    protected TtsCallback mCallback;
    protected boolean mIsSpeaking;

    protected volatile boolean mSttResultReturn;
    protected final List<String> mRequestSttPendingList;

    protected String mTempPcmPath;

    protected final RingBuffer mRingBuffer;
    protected AndroidPcmPlayer mAndroidPcmPlayer;
    protected final ExecutorService mExecutorService;
    protected final ExecutorService mExecutorCacheService;

    protected final List<ResponsePendingData> mResponseDataList;
    protected int mRequestIndex;

    protected boolean mTipTtsRequesting;
    protected final Map<String, String> mRequestTipSttPendingMap;
    protected volatile boolean mTipSttResultReturn;
    protected final Object mRingBufferLock;

    protected List<Long> mRequestThreadIds;

    protected boolean mIsOnSpeaking;

    protected volatile List<HttpURLRequest> mHttpUrlRequests;

    protected volatile boolean mIsCancelRequest;

    protected TtsRobotBase() {
        mIsSpeaking = false;
        mSttResultReturn = true;
        mRequestSttPendingList = new ArrayList<>();
        mRingBuffer = new RingBuffer(1024 * 1024 * 5);
        mExecutorService = new ThreadPoolExecutor(1, 1,
                0, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());

        mExecutorCacheService = new ThreadPoolExecutor(Integer.MAX_VALUE, Integer.MAX_VALUE,
                0, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        mAndroidPcmPlayer = new AndroidPcmPlayer(Constants.STT_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        mResponseDataList = new ArrayList<>();
        mRequestIndex = 0;
        mTipTtsRequesting = false;
        mRingBufferLock = new Object();
        mRequestThreadIds = new ArrayList<>();
        mRequestTipSttPendingMap = new LinkedHashMap<>();
        mTipSttResultReturn = true;
        mIsOnSpeaking = true;

        mHttpUrlRequests = new ArrayList<>();

        mIsCancelRequest = false;
    }

    @Override
    public String getTtsPlatformName() {
        return null;
    }

    @Override
    public void setVoiceName(String voiceName) {
        mVoiceName = voiceName;
    }

    @Override
    public void setTtsCallback(TtsCallback callback) {
        mCallback = callback;
    }

    @Override
    public void init(String pcmFilePath) {
        mTempPcmPath = pcmFilePath;
    }

    @Override
    public void setIsSpeaking(boolean isSpeaking) {
        mIsSpeaking = isSpeaking;
    }

    @Override
    public synchronized void tts(String message) {
    }

    @Override
    public synchronized void tts(String message, boolean isOnSpeaking) {
        mIsOnSpeaking = isOnSpeaking;
        tts(message);
    }

    @Override
    public void tipTts(Map<String, String> tipTtsMessages) {
        for (Map.Entry<String, String> entry : tipTtsMessages.entrySet()) {
            mExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    requestTipTts(entry.getKey(), entry.getValue());
                }
            });
        }
    }

    @Override
    public void cancelTtsRequest(boolean cancel) {
        mIsCancelRequest = cancel;
        if (cancel) {
            if (null != mHttpUrlRequests && mHttpUrlRequests.size() > 0) {
                for (HttpURLRequest httpUrlRequest : mHttpUrlRequests) {
                    httpUrlRequest.setCancelled(true);
                }
                mHttpUrlRequests.clear();
            }
        }
    }

    @Override
    public void clearRingBuffer() {
        synchronized (mRingBufferLock) {
            mRingBuffer.clear();
            mResponseDataList.clear();
            mRequestThreadIds.clear();
            mRequestIndex = 0;
        }
    }

    @Override
    public byte[] getTtsBuffer(int length) {
        synchronized (mRingBufferLock) {
            if (mRingBuffer.size() < length) {
                return null;
            }
            Object o;
            byte[] bytes = new byte[length];
            for (int i = 0; i < length; i++) {
                o = mRingBuffer.take();
                if (null == o) {
                    return null;
                }
                bytes[i] = (byte) o;
            }
            return bytes;
        }
    }

    @Override
    public int getTtsBufferLength() {
        return mRingBuffer.size();
    }

    @Override
    public void close() {
        mRequestSttPendingList.clear();
        mSttResultReturn = true;
        clearRingBuffer();
        if (null != mAndroidPcmPlayer) {
            mAndroidPcmPlayer.stop();
        }
    }

    protected synchronized void handleResponsePendingData(ResponsePendingData pendingData, int index, byte[] bytes, int len, boolean isFirstResponse) {
        if (Config.ENABLE_SHARE_CHAT) {
            if (index == 0) {
                for (int i = 0; i < len; i++) {
                    mRingBuffer.put(bytes[i]);
                }
            } else {
                ResponsePendingData prePendingData = mResponseDataList.get(index - 1);
                if (null != prePendingData && prePendingData.isFinished()) {
                    if (prePendingData.getData() == null) {
                        byte[] pendingBytes = pendingData.getData();
                        if (null != pendingBytes) {
                            for (byte pendingByte : pendingBytes) {
                                mRingBuffer.put(pendingByte);
                            }
                            pendingData.clearData();
                        }
                        for (int i = 0; i < len; i++) {
                            mRingBuffer.put(bytes[i]);
                        }
                    } else {
                        pendingData.putData(bytes, len);
                    }
                } else {
                    pendingData.putData(bytes, len);
                }
            }
        }
    }


    protected synchronized void dealResponseData(int index) {
        for (int j = index - 1; j >= 0; j--) {
            if (null != mResponseDataList.get(j) && !mResponseDataList.get(j).isFinished()) {
                return;
            }
        }
        for (int i = index + 1; i < mResponseDataList.size(); i++) {
            if (null != mResponseDataList.get(i) && mResponseDataList.get(i).isFinished()) {
                byte[] pendingBytes = mResponseDataList.get(i).getData();
                if (null != pendingBytes) {
                    for (byte pendingByte : pendingBytes) {
                        mRingBuffer.put(pendingByte);
                    }
                    mResponseDataList.get(i).clearData();
                }
            } else {
                return;
            }
        }
    }

    protected void requestTipTts(String tip, String path) {
    }

    @Override
    public void addTtsBuffer(byte[] buffer) {
        if (null != buffer) {
            for (byte b : buffer) {
                mRingBuffer.put(b);
            }
        }
    }
}
