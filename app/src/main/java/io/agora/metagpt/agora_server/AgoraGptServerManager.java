package io.agora.metagpt.agora_server;

import android.os.Build;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.agora.metagpt.BuildConfig;
import io.agora.metagpt.models.UserInfo;
import io.agora.metagpt.net.HttpURLRequest;
import io.agora.metagpt.utils.Constants;
import io.agora.metagpt.utils.EncryptUtil;
import io.agora.metagpt.utils.KeyCenter;
import io.agora.metagpt.utils.RingBuffer;

public class AgoraGptServerManager {
    private final static String TAG = Constants.TAG + "-" + AgoraGptServerManager.class.getSimpleName();
    private final RingBuffer mRingBuffer;
    private final Object mRingBufferLock;

    private final ExecutorService mExecutorService;
    private final ExecutorService mExecutorCacheService;
    private String mTempPcmPath;

    private OutputStream mPcmOutputStream;

    private final StringBuilder mServerResponse;

    private WebSocketClient mWebSocketClient;

    private boolean mWsCloseFlag;

    private UserInfo mUserInfo;

    private static final int FRAME_INDEX_FIRST = 0;
    private static final int FRAME_INDEX_CONTINUE = 1;
    private static final int FRAME_INDEX_LAST = 2;

    private int mFrameIndex;

    protected volatile long mLastSendTime;

    private String mSid;
    private final boolean mIsWsRequest = true;

    public AgoraGptServerManager() {
        mRingBuffer = new RingBuffer(1024 * 1024 * 5);
        mRingBufferLock = new Object();

        mExecutorService = new ThreadPoolExecutor(1, 1,
                0, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());

        mExecutorCacheService = new ThreadPoolExecutor(Integer.MAX_VALUE, Integer.MAX_VALUE,
                0, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());

        mServerResponse = new StringBuilder();
        mLastSendTime = 0;
    }

    public void init(String pcmPath, UserInfo userInfo) {
        mTempPcmPath = pcmPath;
        mUserInfo = userInfo;
        mFrameIndex = FRAME_INDEX_FIRST;
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                initWebSocketClient();
            }
        });
    }

    private void initWebSocketClient() {
        try {
            String wsUrl = EncryptUtil.assembleAgoraRequestUrl("ws://10.203.0.207:9999/ws", BuildConfig.APP_ID, KeyCenter.getRtmToken(mUserInfo.getUid()), mUserInfo.getUid());
            //String wsUrl = "ws://10.203.0.207:9999/ws";
            Log.i(TAG, "AgoraGptServer initWebSocketClient,wsUrl:" + wsUrl);
            mWebSocketClient = new WebSocketClient(new URI(wsUrl), new Draft_6455(), null, 5000) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    Log.i(TAG, "AgoraGptServer ws建立连接成功...");
                    mWsCloseFlag = false;
                    sendInitToWs();
                }

                @Override
                public void onMessage(String text) {
                    Log.i(TAG, "AgoraGptServer text:" + text);
                    handleResponseData(text);
                }

                @Override
                public void onMessage(ByteBuffer bytes) {
                    try {
                        Log.i(TAG, "AgoraGptServer 服务端返回：" + new String(bytes.array(), "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onClose(int i, String s, boolean b) {
                    Log.e(TAG, "AgoraGptServer ws链接已关闭，本次请求完成...");
                    mWsCloseFlag = true;
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "AgoraGptServer 发生错误 " + e.getMessage());
                    mWsCloseFlag = true;
                }
            };

            Log.e(TAG, "AgoraGptServer 正在连接...");
            mWebSocketClient.connectBlocking();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "exception:" + e.getMessage());
        }
    }

    private void sendInitToWs() {
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                if (!mWsCloseFlag && mWebSocketClient != null) {
                    JsonObject frame = new JsonObject();
                    JsonObject common = new JsonObject();
                    common.addProperty("type", "init");
                    JsonObject user = new JsonObject();
                    user.addProperty("userName", mUserInfo.getUsername());
                    common.add("user", user);
                    //填充frame
                    frame.add("common", common);
                    Log.i(TAG, "AgoraGptServer sendInitToWs:" + frame.toString());
                    mWebSocketClient.send(frame.toString());
                }
            }
        });
    }

    public void sendPcmData(byte[] bytes) {
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                if (mIsWsRequest) {
                    sendPcmToServerByWs(bytes);
                } else {
                    sendPcmToServerByHttp(bytes);
                }
            }
        });
    }

    private void sendPcmToServerByWs(byte[] bytes) {
        try {
            if (null == mWebSocketClient) {
                initWebSocketClient();
            }
            long curTime = System.nanoTime();
            if (!mWebSocketClient.isOpen()) {
                Log.i(Constants.TAG, "AgoraGptServer 正在连接...");
                mWebSocketClient.reconnectBlocking();
                curTime = System.nanoTime();
            }

            if (!mWebSocketClient.isOpen()) {
                mWebSocketClient.close();
                mWebSocketClient = null;
                Log.e(Constants.TAG, "AgoraGptServer STT connection is closed, discard audio frame!");
                return;
            }

            long diffMs = (curTime - mLastSendTime) / 1000 / 1000;
            if (diffMs < Constants.INTERVAL_XF_REQUEST) {
                Thread.sleep(Constants.INTERVAL_XF_REQUEST - diffMs);
            }

            if (null == bytes) {
                mFrameIndex = FRAME_INDEX_LAST;
            }
            switch (mFrameIndex) {
                case FRAME_INDEX_FIRST:
                    JsonObject frame = new JsonObject();
                    JsonObject business = new JsonObject();
                    JsonObject common = new JsonObject();
                    JsonObject data = new JsonObject();
                    // common
                    common.addProperty("type", "msg");
                    common.addProperty("sid", mSid);

                    //business
                    JsonObject strategy = new JsonObject();
                    strategy.addProperty("chat", 11);
                    JsonObject gpt = new JsonObject();
                    gpt.addProperty("temperature", 0.2f);
                    gpt.addProperty("max_tokens", 1024);
                    JsonObject stt = new JsonObject();
                    JsonObject tts = new JsonObject();
                    tts.addProperty("vcn", "xiaoyan");

                    business.add("strategy", strategy);
                    business.add("gpt", gpt);
                    business.add("stt", stt);
                    business.add("tts", tts);

                    //data
                    data.addProperty("status", FRAME_INDEX_FIRST);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        data.addProperty("inputFormat", "raw-16khz-16bit-mono-pcm");
                        data.addProperty("outFormat", "raw-16khz-16bit-mono-pcm");
                        data.addProperty("payload", Base64.getEncoder().encodeToString(Arrays.copyOf(bytes, bytes.length)));
                    }

                    //frame
                    frame.add("common", common);
                    frame.add("business", business);
                    frame.add("data", data);
                    Log.i(TAG, "AgoraGptServer sendPcmToServerByWs :" + frame.toString());
                    mWebSocketClient.send(frame.toString());
                    mFrameIndex = FRAME_INDEX_CONTINUE;
                    break;

                case FRAME_INDEX_CONTINUE:
                    JsonObject continueFrame = new JsonObject();
                    JsonObject common1 = new JsonObject();
                    // common
                    common1.addProperty("type", "msg");
                    common1.addProperty("sid", mSid);

                    JsonObject data1 = new JsonObject();
                    data1.addProperty("status", FRAME_INDEX_CONTINUE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        data1.addProperty("inputFormat", "raw-16khz-16bit-mono-pcm");
                        data1.addProperty("outFormat", "raw-16khz-16bit-mono-pcm");
                        data1.addProperty("payload", Base64.getEncoder().encodeToString(Arrays.copyOf(bytes, bytes.length)));
                    }
                    continueFrame.add("common", common1);
                    continueFrame.add("data", data1);
                    Log.i(TAG, "AgoraGptServer sendPcmToServerByWs :" + continueFrame.toString());
                    mWebSocketClient.send(continueFrame.toString());
                    break;

                case FRAME_INDEX_LAST:
                    JsonObject lastFrame = new JsonObject();
                    JsonObject common2 = new JsonObject();
                    // common
                    common2.addProperty("type", "msg");
                    common2.addProperty("sid", mSid);
                    JsonObject data2 = new JsonObject();
                    data2.addProperty("status", FRAME_INDEX_LAST);
                    lastFrame.add("common", common2);
                    lastFrame.add("data", data2);
                    Log.i(TAG, "AgoraGptServer sendPcmToServerByWs :" + lastFrame.toString());
                    mWebSocketClient.send(lastFrame.toString());
                    break;
                default:
                    break;
            }
            mLastSendTime = System.nanoTime();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendPcmToServerByHttp(byte[] bytes) {
        mServerResponse.delete(0, mServerResponse.length());
        try {
            HttpURLRequest request = new HttpURLRequest();
            request.setCallback(new HttpURLRequest.RequestCallback() {
                @Override
                public void updateResponseData(byte[] bytes, int len, boolean isFirstResponse) {
                    try {
                        mServerResponse.append(new String(bytes, 0, len, "utf-8"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void requestFail(int errorCode, String msg) {
                }

                @Override
                public void requestFinish() {
                    handleResponseData(mServerResponse.toString());
                }
            });
            Map<String, String> params = new HashMap<>(4);
            params.put("Content-Type", "application/json");
            try {
                String requestBody = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    requestBody = "{\"authorization\":\"wedsfs@2fsf234242SDSFS\",\"common\":{\"user\":{\"userName\":\"Paul\"},\"sid\":\"rta0000000a@ch312c0e3f63609f0900\"},\"data\":{\"inputFormat\":\"raw-16khz-16bit-mono-pcm\",\"payload\":\"" + Base64.getEncoder().encodeToString(Arrays.copyOf(bytes, bytes.length)) + "\"},\"date\":\"Wed, 29 Jun 2023 11:12:15 UTC\"}";
                }
                Log.i(Constants.TAG, "requestBody=" + requestBody);
                request.requestPostUrl("http://10.203.0.207:8081/gpt/getChatMsg", params, requestBody);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected synchronized void handleResponseData(String text) {
        try {
            JSONObject jsonObject = JSONObject.parseObject(text);
            String action = jsonObject.getString("action");
            int code = jsonObject.getInteger("code");
            String sid = jsonObject.getString("sid");
            String desc = jsonObject.getString("desc");
            byte[] textBase64Decode = null;
            if ("started".equals(action)) {
                if (0 == code) {
                    mSid = sid;
                }
            } else if ("result".equals(action)) {
                if (0 == code) {
                    JSONObject dataJson = JSONObject.parseObject(jsonObject.getString("data"));
                    String outFormat = dataJson.getString("outFormat");
                    String payload = dataJson.getString("payload");
                    if (outFormat.endsWith("pcm")) {
                        try {
                            if (null == mPcmOutputStream) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    mPcmOutputStream = Files.newOutputStream(Paths.get(mTempPcmPath));
                                } else {
                                    mPcmOutputStream = new FileOutputStream(mTempPcmPath);
                                }
                            }
                            textBase64Decode = new byte[0];
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                textBase64Decode = Base64.getDecoder().decode(payload);
                            }

                            for (byte b : textBase64Decode) {
                                mRingBuffer.put(b);
                            }

                            mPcmOutputStream.write(textBase64Decode);
                            mPcmOutputStream.flush();


                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            if (mPcmOutputStream != null) {
                                try {
                                    mPcmOutputStream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } finally {
                                    mPcmOutputStream = null;
                                }
                            }
                        }
                    } else if (outFormat.endsWith("txt")) {

                    }
                }
            } else if ("init".equals(action)) {
                if (0 == code) {

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void clearBuffer() {
        synchronized (mRingBufferLock) {
            mRingBuffer.clear();
        }
    }

    public byte[] getBuffer(int length) {
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


}
