package io.agora.gpt.tts.xf;

import android.media.AudioFormat;
import android.os.Build;
import android.util.Log;

import com.google.gson.Gson;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.handshake.ServerHandshake;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import io.agora.gpt.BuildConfig;
import io.agora.gpt.tts.TtsRobotBase;
import io.agora.gpt.utils.AndroidPcmPlayer;
import io.agora.gpt.utils.Config;
import io.agora.gpt.utils.Constants;
import okhttp3.HttpUrl;

public class XfTtsRobot extends TtsRobotBase {
    private WebSocketClient mWebSocketClient;

    private static final String TTE = "UTF8";

    private final Gson gson;

    private OutputStream mOutputStream;

    public boolean wsCloseFlag;

    private long mStartTtsTime;
    private boolean mIsFirstResponse;

    private String mTipTtsPath;

    public XfTtsRobot() {
        super();
        gson = new Gson();
        mOutputStream = null;
        wsCloseFlag = true;
    }

    @Override
    public String getTtsPlatformName() {
        return Constants.PLATFORM_NAME_XF;
    }


    @Override
    public void init(final String path) {
        super.init(path);
        initWebSocketClient();
    }

    private void initWebSocketClient() {
        if (null == mAndroidPcmPlayer) {
            mAndroidPcmPlayer = new AndroidPcmPlayer(Constants.STT_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        }

        try {
            String wsUrl = getAuthUrl(BuildConfig.XF_TTS_HOST, BuildConfig.XF_API_KEY, BuildConfig.XF_API_SECRET).replace("https://", "wss://");
            mWebSocketClient = new WebSocketClient(new URI(wsUrl)) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    Log.i(Constants.TAG, "ws建立连接成功...");
                }

                @Override
                public void onMessage(String text) {
                    if (mTipTtsRequesting) {
                        JsonParse myJsonParse = gson.fromJson(text, JsonParse.class);
                        if (myJsonParse.code != 0) {
                            Log.e(Constants.TAG, "发生错误，错误码为：" + myJsonParse.code);
                            Log.e(Constants.TAG, "本次请求的sid为：" + myJsonParse.sid);
                        }

                        if (myJsonParse.data != null) {
                            byte[] textBase64Decode = null;
                            try {
                                if (null == mOutputStream) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        mOutputStream = Files.newOutputStream(Paths.get(mTipTtsPath));
                                    } else {
                                        mOutputStream = new FileOutputStream(mTipTtsPath);
                                    }
                                }
                                textBase64Decode = new byte[0];
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    textBase64Decode = Base64.getDecoder().decode(myJsonParse.data.audio);
                                }

                                mOutputStream.write(textBase64Decode);
                                mOutputStream.flush();

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (myJsonParse.data.status == 2) {
                                Log.e(Constants.TAG, "本次请求的sid==>" + myJsonParse.sid);
                                if (mOutputStream != null) {
                                    try {
                                        mOutputStream.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } finally {
                                        mOutputStream = null;
                                    }
                                }
                                mTipSttResultReturn = true;
                                if (mRequestTipSttPendingMap.size() == 0) {
                                    mTipTtsRequesting = false;
                                    wsCloseFlag = true;
                                } else {
                                    Map.Entry<String, String> firstEntry = mRequestTipSttPendingMap.entrySet().iterator().next();
                                    requestTipTts(firstEntry.getKey(), firstEntry.getValue());
                                    mRequestTipSttPendingMap.remove(firstEntry.getKey());
                                }
                            }
                        }
                    } else {
                        if (!mIsSpeaking) {
                            return;
                        }
                        JsonParse myJsonParse = gson.fromJson(text, JsonParse.class);
                        if (myJsonParse.code != 0) {
                            Log.e(Constants.TAG, "发生错误，错误码为：" + myJsonParse.code);
                            Log.e(Constants.TAG, "本次请求的sid为：" + myJsonParse.sid);
                        }

                        if (myJsonParse.data != null) {
                            byte[] textBase64Decode = null;
                            try {
                                if (null == mOutputStream) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        mOutputStream = Files.newOutputStream(Paths.get(mTempPcmPath));
                                    } else {
                                        mOutputStream = new FileOutputStream(mTempPcmPath);
                                    }
                                }
                                textBase64Decode = new byte[0];
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    textBase64Decode = Base64.getDecoder().decode(myJsonParse.data.audio);
                                }
                                if (Config.ENABLE_SHARE_CHAT) {
                                    for (byte b : textBase64Decode) {
                                        mRingBuffer.put(b);
                                    }
                                }
                                if (!mIsFirstResponse && null != mCallback) {
                                    mIsFirstResponse = true;
                                    mCallback.updateTtsHistoryInfo("讯飞tts第一条返回结果(" + (System.currentTimeMillis() - mStartTtsTime) + "ms)");
                                }
                                mOutputStream.write(textBase64Decode);
                                mOutputStream.flush();

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (myJsonParse.data.status == 2) {
                                Log.e(Constants.TAG, "本次请求的sid==>" + myJsonParse.sid);
                                if (mOutputStream != null) {
                                    try {
                                        mOutputStream.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } finally {
                                        mOutputStream = null;
                                    }
                                }

//                            if (null != mAndroidPcmPlayer) {
//                                mAndroidPcmPlayer.stop();
//                                mAudioTrackExecutorService.execute(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        mAndroidPcmPlayer.play(path);
//                                    }
//                                });
//                            }
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
                                wsCloseFlag = true;
                            }
                        }
                    }
                }

                @Override
                public void onClose(int i, String s, boolean b) {
                    Log.e(Constants.TAG, "ws链接已关闭，本次请求完成...");
                    wsCloseFlag = true;
                }

                @Override
                public void onError(Exception e) {
                    Log.e(Constants.TAG, "发生错误 " + e.getMessage());
                }
            };

            mWebSocketClient.connect();

            while (!mWebSocketClient.getReadyState().equals(ReadyState.OPEN)) {
                Log.e(Constants.TAG, "正在连接...");
                Thread.sleep(100);
            }
            wsCloseFlag = false;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(Constants.TAG, "exception:" + e.getMessage());
        }
    }


    @Override
    public void tts(final String msg) {
        if (!mIsSpeaking) {
            Log.i(Constants.TAG, "not speaking return:" + msg);
            return;
        }

        if (mRequestTipSttPendingMap.size() != 0) {
            mRequestTipSttPendingMap.clear();
            mTipTtsRequesting = false;
        }

        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                if (!mSttResultReturn) {
                    Log.i(Constants.TAG, "tts pending msg:" + msg);
                    mRequestSttPendingList.add(msg);
                    return;
                }

                if (wsCloseFlag && mIsSpeaking) {
                    try {
                        if (null == mWebSocketClient) {
                            initWebSocketClient();
                        }
                        mWebSocketClient.reconnect();
                        int waitCount = 0;
                        while (!mWebSocketClient.getReadyState().equals(ReadyState.OPEN)) {
                            if (waitCount > 50) {
                                initWebSocketClient();
                            }
                            Log.e(Constants.TAG, "正在连接...");
                            Thread.sleep(100);
                            waitCount++;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    wsCloseFlag = false;
                }


                //请求参数json串
                String requestJson = "";
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        requestJson = "{\n" +
                                "  \"common\": {\n" +
                                "    \"app_id\": \"" + BuildConfig.XF_APP_ID + "\"\n" +
                                "  },\n" +
                                "  \"business\": {\n" +
                                "    \"aue\": \"raw\",\n" +
                                "    \"tte\": \"" + TTE + "\",\n" +
                                "    \"ent\": \"intp65\",\n" +
                                "    \"vcn\": \"" + mVoiceNameValue + "\",\n" +
                                "    \"pitch\": 50,\n" +
                                "    \"speed\": 50\n" +
                                "  },\n" +
                                "  \"data\": {\n" +
                                "    \"status\": 2,\n" +
                                "    \"text\": \"" + Base64.getEncoder().encodeToString(msg.getBytes(StandardCharsets.UTF_8)) + "\"\n" +
                                "  }\n" +
                                "}";
                    }
                    if (mIsOnSpeaking && !mIsSpeaking) {
                        return;
                    }
                    if (null != mCallback) {
                        mCallback.onTtsStart(msg, mIsOnSpeaking);
                    }
                    mStartTtsTime = System.currentTimeMillis();
                    mIsFirstResponse = false;

                    mSttResultReturn = false;
                    mWebSocketClient.send(requestJson);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(Constants.TAG, "xf tts error:" + e);
                }
            }
        });
    }

    @Override
    protected void requestTipTts(String tip, String path) {
        super.requestTipTts(tip, path);
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                if (!mTipSttResultReturn) {
                    Log.i(Constants.TAG, "tip tts pending msg:" + tip);
                    mRequestTipSttPendingMap.put(tip, path);
                    return;
                }
                if (wsCloseFlag) {
                    try {
                        if (null == mWebSocketClient) {
                            initWebSocketClient();
                        }
                        mWebSocketClient.reconnect();
                        int waitCount = 0;
                        while (!mWebSocketClient.getReadyState().equals(ReadyState.OPEN)) {
                            if (waitCount > 50) {
                                initWebSocketClient();
                            }
                            Log.e(Constants.TAG, "正在连接...");
                            Thread.sleep(100);
                            waitCount++;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    wsCloseFlag = false;
                }


                //请求参数json串
                String requestJson = "";
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        requestJson = "{\n" +
                                "  \"common\": {\n" +
                                "    \"app_id\": \"" + BuildConfig.XF_APP_ID + "\"\n" +
                                "  },\n" +
                                "  \"business\": {\n" +
                                "    \"aue\": \"raw\",\n" +
                                "    \"tte\": \"" + TTE + "\",\n" +
                                "    \"ent\": \"intp65\",\n" +
                                "    \"vcn\": \"" + mVoiceNameValue + "\",\n" +
                                "    \"pitch\": 50,\n" +
                                "    \"speed\": 50\n" +
                                "  },\n" +
                                "  \"data\": {\n" +
                                "    \"status\": 2,\n" +
                                "    \"text\": \"" + Base64.getEncoder().encodeToString(tip.getBytes(StandardCharsets.UTF_8)) + "\"\n" +
                                "  }\n" +
                                "}";
                    }
                    mTipTtsRequesting = true;
                    mTipSttResultReturn = false;
                    mTipTtsPath = path;
                    mWebSocketClient.send(requestJson);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(Constants.TAG, "xf tts error:" + e);
                }
            }
        });
    }

    @Override
    public void close() {
        super.close();

        if (!wsCloseFlag) {
            if (null != mWebSocketClient) {
                mWebSocketClient.close();
            }
        }
        mWebSocketClient = null;

        if (null != mOutputStream) {
            try {
                mOutputStream.close();
                mOutputStream = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    // 鉴权方法
    public static String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(hostUrl);
        // 时间
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());
        // 拼接
        String preStr = "host: " + url.getHost() + "\n" +
                "date: " + date + "\n" +
                "GET " + url.getPath() + " HTTP/1.1";
        // SHA256加密
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(preStr.getBytes(StandardCharsets.UTF_8));
        // Base64加密
        String sha = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sha = Base64.getEncoder().encodeToString(hexDigits);
        }
        // 拼接
        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", apiKey, "hmac-sha256", "host date request-line", sha);
        // 拼接地址
        HttpUrl httpUrl = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            httpUrl = Objects.requireNonNull(HttpUrl.parse("https://" + url.getHost() + url.getPath())).newBuilder().//
                    addQueryParameter("authorization", Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8))).//
                    addQueryParameter("date", date).//
                    addQueryParameter("host", url.getHost()).//
                    build();
        }

        return httpUrl.toString();
    }


    class JsonParse {
        int code;
        String sid;
        Data data;
    }

    static class Data {
        int status;
        String audio;
    }
}
