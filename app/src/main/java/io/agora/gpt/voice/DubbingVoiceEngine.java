package io.agora.gpt.voice;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gerzz.dubbing.sdk.ThirdRTC;
import com.gerzz.dubbing.sdk.VCAction;
import com.gerzz.dubbing.sdk.VCEngine;
import com.gerzz.dubbing.sdk.VCEngineCallback;
import com.gerzz.dubbing.sdk.VCEngineCode;
import com.gerzz.dubbing.sdk.VCEngineStatus;
import com.gerzz.dubbing.sdk.VCVoice;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.agora.gpt.utils.Constants;
import io.agora.gpt.utils.RingBuffer;

/**
 * download dubbing voice from <a href="https://gerzz-interactive.feishu.cn/docx/PKi6d79YvodjrdxUMgRcUuoLnYg">...</a>
 */

public class DubbingVoiceEngine {
    private final static String TAG = Constants.TAG + "-" + DubbingVoiceEngine.class.getSimpleName();
    private VCEngine mEngine;

    private CallBack mCallBack;

    private final RingBuffer mDubbingVoiceRingBuffer;
    private final Object mRingBufferLock;
    private boolean mRingBufferReady;

    private final ExecutorService mExecutorService;

    public DubbingVoiceEngine() {
        mDubbingVoiceRingBuffer = new RingBuffer(1024 * 1024 * 5);
        mRingBufferLock = new Object();
        mExecutorService = new ThreadPoolExecutor(1, 1,
                0, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        mRingBufferReady = false;
    }

    public void initEngine(Activity activity, String token, int inputSampleRate, int outputSampleRate, int samplesPerCall, final boolean onlyDownload) {
        mExecutorService.execute(() -> {
            mEngine = new VCEngine.Builder(activity)
                    .log()
                    .transformLog()
                    .token(token)
                    .inputSampleRate(inputSampleRate)
                    .outputSampleRate(outputSampleRate)
                    .samplesPerCall(samplesPerCall)
                    .thirdRTC(ThirdRTC.AGORA)
                    .engineCallback(new VCEngineCallback() {
                        @Override
                        public void onDownload(int percent, int index, int count) {
                            Log.i(TAG, "onDownload percent:" + percent + ",index:" + index + ",count:" + count);
                        }

                        @Override
                        public void onActionResult(@NonNull VCAction vcAction, @NonNull VCEngineCode vcEngineCode, @Nullable String msg) {
                            Log.i(TAG, "onActionResult action:" + vcAction + ",code:" + vcEngineCode + ",msg:" + msg);
                            if (!onlyDownload) {
                                if (vcAction == VCAction.PREPARE && vcEngineCode == VCEngineCode.SUCCESS) {
                                    mEngine.initEngine();
                                } else if (vcAction == VCAction.INITIALIZE && vcEngineCode == VCEngineCode.SUCCESS) {
                                    List<VCVoice> vcVoices = mEngine.getVoiceList();
                                    for (VCVoice voice : vcVoices) {
                                        //192:芊芊,190:小奇
                                        if (voice.getId() == 192) {
                                            mEngine.setVoice(voice);
                                            break;
                                        }
                                    }

                                } else if (vcAction == VCAction.SET_VOICE && vcEngineCode == VCEngineCode.SUCCESS) {
                                    if (null != mCallBack) {
                                        mCallBack.onDubbingInitSuccess();
                                    }
                                }
                            }
                        }
                    })
                    .build();

            mEngine.prepare();
        });
    }

    public void setCallBack(CallBack callBack) {
        this.mCallBack = callBack;
    }

    private byte[] transform(byte[] data) {
        if (mEngine != null && null != data) {
            return mEngine.transform(data);
        }
        return null;
    }

    private byte[] transform(ByteBuffer byteBuffer) {
        if (mEngine != null && null != byteBuffer) {
            return mEngine.transform(byteBuffer);
        }
        return null;
    }

    public void start() {
        if (VCEngineStatus.STARTED == mEngine.getEngineStatus()) {
            return;
        }
        if (mEngine != null) {
            mEngine.start();
        }
    }

    public void stop() {
        if (VCEngineStatus.STOPPED == mEngine.getEngineStatus()) {
            return;
        }
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                if (mEngine != null) {
                    mEngine.stop();
                    clearData();
                }
            }
        });
    }

    public void release() {
        if (VCEngineStatus.RELEASED == mEngine.getEngineStatus()) {
            return;
        }
        if (mEngine != null) {
            mEngine.releaseEngine();
        }
    }

    public void putDubbingVoiceBuffer(byte[] bytes) {
        if (VCEngineStatus.STARTED != mEngine.getEngineStatus()) {
            start();
        }
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                byte[] dubbingVoice = transform(bytes);
                if (null != dubbingVoice) {
                    for (byte b : dubbingVoice) {
                        mDubbingVoiceRingBuffer.put(b);
                    }
                }
            }
        });
    }

    public byte[] getDubbingVoiceBuffer(int length) {
        synchronized (mRingBufferLock) {
            if (mRingBufferReady) {
                return getRingBuffer(length);
            } else {
                //wait length*WAIT_AUDIO_FRAME_COUNT data
                if (mDubbingVoiceRingBuffer.size() >= length * Constants.WAIT_AUDIO_FRAME_COUNT) {
                    mRingBufferReady = true;
                    return getRingBuffer(length);
                } else {
                    return null;
                }
            }
        }
    }

    private byte[] getRingBuffer(int length) {
        if (mDubbingVoiceRingBuffer.size() < length) {
            return null;
        }
        Object o;
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            o = mDubbingVoiceRingBuffer.take();
            if (null == o) {
                return null;
            }
            bytes[i] = (byte) o;
        }
        return bytes;
    }

    private void clearData() {
        synchronized (mRingBufferLock) {
            mRingBufferReady = false;
            mDubbingVoiceRingBuffer.clear();
        }
    }

    public interface CallBack {
        void onDubbingInitSuccess();
    }
}
