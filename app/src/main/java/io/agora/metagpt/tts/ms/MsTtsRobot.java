package io.agora.metagpt.tts.ms;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.agora.metagpt.BuildConfig;
import io.agora.metagpt.models.ResponsePendingData;
import io.agora.metagpt.net.HttpURLRequest;
import io.agora.metagpt.tts.TtsRobotBase;
import io.agora.metagpt.utils.Config;
import io.agora.metagpt.utils.Constants;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

public class MsTtsRobot extends TtsRobotBase {
    public MsTtsRobot() {
        mVoiceName = "zh-CN-XiaoxiaoNeural";
    }

    @Override
    public String getTtsPlatformName() {
        return "ms";
    }

    @Override
    public synchronized void tts(String message) {
        super.tts(message);
        if (!mIsSpeaking && mIsOnSpeaking) {
            Log.i(Constants.TAG, "not speaking return:" + message);
            return;
        }
        if (Config.TTS_RESPONSE_STREAM) {
            final ResponsePendingData pendingData = new ResponsePendingData();
            final int index = mRequestIndex++;
            mResponseDataList.add(pendingData);
            mExecutorCacheService.execute(new Runnable() {
                @Override
                public void run() {
                    mRequestThreadIds.add(Thread.currentThread().getId());
                    long curTime = System.currentTimeMillis();
                    if (null != mCallback) {
                        mCallback.onTtsStart(message, mIsOnSpeaking);
                    }

                    try {
                        File file = new File(mTempPcmPath + index + ".pcm");
                        OutputStream pcmOs = new FileOutputStream(file);

                        HttpURLRequest request = new HttpURLRequest();
                        request.setCallback(new HttpURLRequest.RequestCallback() {
                            @Override
                            public void updateResponseData(byte[] bytes, int len, boolean isFirstResponse) {
                                if (mIsOnSpeaking && (!mIsSpeaking || !mRequestThreadIds.contains(Thread.currentThread().getId()) || mIsCancelRequest)) {
                                    return;
                                }
                                if (isFirstResponse && mIsOnSpeaking) {
                                    mCallback.updateTtsHistoryInfo("微软tts第一条返回结果(" + (System.currentTimeMillis() - curTime) + "ms)");
                                }
                                handleResponsePendingData(pendingData, index, bytes, len, isFirstResponse);

                                try {
                                    pcmOs.write(bytes, 0, len);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void requestFail(int errorCode, String msg) {
                                try {
                                    pcmOs.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                if (mIsOnSpeaking && (!mIsSpeaking || !mRequestThreadIds.contains(Thread.currentThread().getId()) || mIsCancelRequest)) {
                                    return;
                                }
                                mRequestThreadIds.remove(Thread.currentThread().getId());
                                if (null != mCallback && mIsOnSpeaking) {
                                    mCallback.updateTtsHistoryInfo("微软请求错误：" + errorCode + "," + msg);
                                }
                                if (!mIsOnSpeaking) {
                                    mIsOnSpeaking = true;
                                }
                            }

                            @Override
                            public void requestFinish() {
                                try {
                                    pcmOs.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                if (mIsOnSpeaking && (!mIsSpeaking || !mRequestThreadIds.contains(Thread.currentThread().getId()) || mIsCancelRequest)) {
                                    return;
                                }
                                mRequestThreadIds.remove(Thread.currentThread().getId());

                                if (null != mCallback) {
                                    mCallback.onTtsFinish(mIsOnSpeaking);
                                }

                                if (!mIsOnSpeaking) {
                                    mIsOnSpeaking = true;
                                }
                                pendingData.setFinished(true);
                                dealResponseData(index);
                            }
                        });
                        Map<String, String> params = new HashMap<>(4);
                        params.put("Content-Type", "application/ssml+xml");
                        params.put("User-Agent", Constants.TAG);
                        params.put("Ocp-Apim-Subscription-Key", BuildConfig.MS_SPEECH_KEY);
                        params.put("X-Microsoft-OutputFormat", "raw-16khz-16bit-mono-pcm");

                        String requestStr = "<speak xmlns='http://www.w3.org/2001/10/synthesis' version='1.0' xml:lang='zh-CN'>" +
                                "<voice xml:lang='zh-CN' xml:gender='Female' name='" + mVoiceName + "' style='cheerful'>" +
                                message +
                                "</voice>" +
                                "</speak>";
                        request.requestPostUrl(BuildConfig.MS_SERVER_HOST + "cognitiveservices/v1", params, requestStr);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            long curTime = System.currentTimeMillis();
            String requestStr = "<speak version='1.0' xml:lang='zh-CN'>" +
                    "<voice xml:lang='zh-CN' xml:gender='Female' name='" + mVoiceName + "'  style='cheerful'>" +
                    message +
                    "</voice>" +
                    "</speak>";

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
                                File file = new File(mTempPcmPath);
                                OutputStream os = new FileOutputStream(file);
                                byte[] buffer = new byte[1024];
                                int read;
                                boolean isFirstLine = false;
                                while ((read = is.read(buffer)) != -1) {
                                    if (!isFirstLine && null != mCallback) {
                                        isFirstLine = true;
                                        mCallback.updateTtsHistoryInfo("微软tts第一条返回结果(" + (System.currentTimeMillis() - curTime) + "ms)");
                                    }
                                    if (Config.ENABLE_SHARE_CHAT) {
                                        for (byte b : buffer) {
                                            mRingBuffer.put(b);
                                        }
                                    }
                                    os.write(buffer, 0, read);
                                    Log.i(Constants.TAG, "微软tts");
                                }
                                os.close();
                                is.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            if (null != mCallback) {
                                mCallback.onTtsFinish(mIsOnSpeaking);
                            }

                            if (!mIsOnSpeaking) {
                                mIsOnSpeaking = true;
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            if (Objects.equals(e.getMessage(), "timeout")) {
                                if (null != mCallback) {
                                    mCallback.updateTtsHistoryInfo("微软tts请求超时");
                                }
                            }
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }
    }


    @Override
    public void requestTipTts(String tip, String path) {
        super.requestTipTts(tip, path);
        String requestStr = "<speak version='1.0' xml:lang='zh-CN'>" +
                "<voice xml:lang='zh-CN' xml:gender='Female' name='" + mVoiceName + "'  style='cheerful'>" +
                tip +
                "</voice>" +
                "</speak>";

        RequestBody requestBody = RequestBody.create(requestStr, MediaType.parse("application/ssml+xml"));
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
                            File file = new File(path);
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

                    }

                    @Override
                    public void onError(Throwable e) {
                        if (Objects.equals(e.getMessage(), "timeout")) {
                            Log.e(Constants.TAG, "微软tip tts请求超时");
                        }
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }
}
