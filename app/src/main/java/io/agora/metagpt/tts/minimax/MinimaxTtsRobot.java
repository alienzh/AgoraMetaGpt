package io.agora.metagpt.tts.minimax;

import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import io.agora.metagpt.BuildConfig;
import io.agora.metagpt.models.tts.minimax.MinimaxTtsRequestBody;
import io.agora.metagpt.net.HttpURLRequest;
import io.agora.metagpt.tts.TtsRobotBase;
import io.agora.metagpt.utils.Config;
import io.agora.metagpt.utils.Constants;
import io.agora.metagpt.utils.MediaTypeConverter;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;

public class MinimaxTtsRobot extends TtsRobotBase {
    private final MediaTypeConverter mMediaConverter;

    public MinimaxTtsRobot() {
        super();
        mVoiceName = "male-qn-qingse";
        mMediaConverter = new MediaTypeConverter();
    }

    @Override
    public String getTtsPlatformName() {
        return "minimax";
    }

    @Override
    public void tts(String message) {
        if (mIsOnSpeaking && !mIsSpeaking) {
            Log.i(Constants.TAG, "not speaking return:" + message);
            return;
        }
        final int index = mRequestIndex++;
        if (Config.TTS_RESPONSE_STREAM) {
            mExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    long curTime = System.currentTimeMillis();
                    if (null != mCallback) {
                        mCallback.onTtsStart(message, mIsOnSpeaking);
                    }

                    try {
                        HttpURLRequest request = new HttpURLRequest();

                        File tempPcmFile = new File(mTempPcmPath);
                        File file = new File(tempPcmFile.getParent() + "/minimax_output" + (index) + ".mp3");
                        OutputStream mp3Os = new FileOutputStream(file);

                        request.setCallback(new HttpURLRequest.RequestCallback() {
                            @Override
                            public void updateResponseData(byte[] bytes, int len, boolean isFirstResponse) {
                                if (!mIsSpeaking) {
                                    return;
                                }

                                String statusMsg = null;
                                try {
                                    String response = new String(bytes, 0, len);
                                    JSONObject jsonObject = JSON.parseObject(response);
                                    JSONObject baseResp = jsonObject.getJSONObject("base_resp");
                                    statusMsg = baseResp.getString("status_msg");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                if (!TextUtils.isEmpty(statusMsg)) {
                                    if (isFirstResponse && null != mCallback) {
                                        mCallback.updateTtsHistoryInfo("Minimax tts 请求返回：" + statusMsg);
                                    }
                                } else {
                                    if (isFirstResponse && null != mCallback) {
                                        mCallback.updateTtsHistoryInfo("Minimax tts第一条返回结果(" + (System.currentTimeMillis() - curTime) + "ms)");
                                    }
                                }
                                try {
                                    mp3Os.write(bytes, 0, len);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void requestFail(int errorCode, String msg) {
                                if (null != mCallback) {
                                    mCallback.updateTtsHistoryInfo("Minimaxtts请求错误：" + errorCode + "," + msg);
                                }
                            }

                            @Override
                            public void requestFinish() {
                                try {
                                    mp3Os.close();

                                    byte[] pcmData = mMediaConverter.convertMp3ToPcm(file.getPath(), mTempPcmPath);
                                    if (Config.ENABLE_SHARE_CHAT) {
                                        for (byte b : pcmData) {
                                            mRingBuffer.put(b);
                                        }
                                    }
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
                        });

                        Map<String, String> params = new HashMap<>();
                        params.put("Content-Type", "application/json");
                        params.put("Authorization", "Bearer " + BuildConfig.MINIMAX_AUTHORIZATION);

                        MinimaxTtsRequestBody body = new MinimaxTtsRequestBody();
                        body.setVoice_id(mVoiceName);
                        body.setText(message);
                        body.setModel("speech-01");

                        request.requestPostUrl(BuildConfig.MINIMAX_SERVER_HOST + "v1/text_to_speech?GroupId=" + BuildConfig.MINIMAX_GROUP_ID, params, JSON.toJSONString(body));

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

        } else {
            if (!mSttResultReturn) {
                Log.i(Constants.TAG, "tts pending msg:" + message);
                mRequestSttPendingList.add(message);
                return;
            }
            MinimaxTtsRequestBody body = new MinimaxTtsRequestBody();
            body.setVoice_id(mVoiceName);
            body.setText(message);
            body.setModel("speech-01");

            if (null != mCallback) {
                mCallback.onTtsStart(message, mIsOnSpeaking);
            }
            mSttResultReturn = false;
            MinimaxTtsRetrofitManager.getInstance().getMinimaxRequest().getTtsResult(BuildConfig.MINIMAX_GROUP_ID, body)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<ResponseBody>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onNext(ResponseBody response) {
                            File tempPcmFile = new File(mTempPcmPath);
                            try {
                                InputStream inputStream = response.byteStream();
                                File file = new File(tempPcmFile.getParent() + "/output" + (index) + ".mp3");
                                FileOutputStream fileOutputStream = new FileOutputStream(new File(tempPcmFile.getParent() + "/output.mp3"));
                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = inputStream.read(buffer)) != -1) {
                                    fileOutputStream.write(buffer, 0, bytesRead);
                                }
                                fileOutputStream.close();
                                inputStream.close();

                                byte[] pcmData = mMediaConverter.convertMp3ToPcm(file.getPath(), mTempPcmPath);
                                if (Config.ENABLE_SHARE_CHAT) {
                                    for (byte b : pcmData) {
                                        mRingBuffer.put(b);
                                    }
                                }


                                mSttResultReturn = true;

                                if (null != mCallback) {
                                    mCallback.onTtsFinish(mIsOnSpeaking);
                                }

                                if (!mIsOnSpeaking) {
                                    mIsOnSpeaking = true;
                                }

                                if (mRequestSttPendingList.size() > 0) {
                                    tts(mRequestSttPendingList.get(0));
                                    mRequestSttPendingList.remove(0);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.i(Constants.TAG, "getTtsResult error=" + e);
                            if (null != mCallback) {
                                mCallback.updateTtsHistoryInfo("Minimax TTS 请求出错");
                            }
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }
    }


    @Override
    protected void requestTipTts(final String tip, final String path) {
        super.requestTipTts(tip, path);

        MinimaxTtsRequestBody body = new MinimaxTtsRequestBody();
        body.setVoice_id(mVoiceName);
        body.setText(tip);
        body.setModel("speech-01");

        MinimaxTtsRetrofitManager.getInstance().getMinimaxRequest().getTtsResult(BuildConfig.MINIMAX_GROUP_ID, body)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(ResponseBody response) {
                        File tempPcmFile = new File(path);
                        try {
                            InputStream inputStream = response.byteStream();
                            File file = new File(tempPcmFile.getParent() + "/output_tip.mp3");
                            FileOutputStream fileOutputStream = new FileOutputStream(file);
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                fileOutputStream.write(buffer, 0, bytesRead);
                            }
                            fileOutputStream.close();
                            inputStream.close();

                            mMediaConverter.convertMp3ToPcm(file.getPath(), path);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i(Constants.TAG, "getTtsResult error=" + e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }
}
