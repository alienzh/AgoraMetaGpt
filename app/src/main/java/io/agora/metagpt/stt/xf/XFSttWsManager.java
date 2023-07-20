package io.agora.metagpt.stt.xf;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.handshake.ServerHandshake;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import io.agora.metagpt.BuildConfig;
import io.agora.metagpt.utils.Constants;
import io.agora.metagpt.utils.EncryptUtil;
import okhttp3.HttpUrl;

public class XFSttWsManager {

    protected final Gson mGson;
    private WebSocketClient mWebSocketClient;

    private final ExecutorService mExecutorService;
    public boolean wsCloseFlag;

    private SttCallback mCallback;

    private boolean mIsFinished;

    private long mLastSendTime = 0;

    private int mFrameIndex;

    private static final int FRAME_INDEX_FIRST = 0;
    private static final int FRAME_INDEX_CONTINUE = 1;
    private static final int FRAME_INDEX_LAST = 2;

    private XFSttWsManager() {
        mExecutorService = new ThreadPoolExecutor(1, 1,
                0, TimeUnit.MILLISECONDS,
                new LinkedBlockingDeque<Runnable>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        mGson = new Gson();
        wsCloseFlag = true;
        mIsFinished = false;
        mFrameIndex = FRAME_INDEX_FIRST;
    }

    private static final class InstanceHolder {
        static final XFSttWsManager M_INSTANCE = new XFSttWsManager();
    }

    public static XFSttWsManager getInstance() {
        return InstanceHolder.M_INSTANCE;
    }

    public void setCallback(SttCallback callback) {
        mCallback = callback;
    }

    public void initWebSocketClient() {
        try {
            String wsUrl = XfAuthUtils.assembleRequestUrl(BuildConfig.XF_STT_IST_HOST, BuildConfig.XF_STT_API_KEY, BuildConfig.XF_STT_API_SECRET);

            mWebSocketClient = new WebSocketClient(new URI(wsUrl), new Draft_6455(), null, 5000) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    Log.i(Constants.TAG, "ws建立连接成功...");
                    wsCloseFlag = false;
                }

                @Override
                public void onMessage(String text) {
                    //Log.i(Constants.TAG, "xf text:" + text);
                    ResponseData resp = mGson.fromJson(text, ResponseData.class);
                    if (resp != null) {
                        if (resp.getCode() != 0) {
                            Log.e(Constants.TAG, "error=>" + resp.getMessage() + " sid=" + resp.getSid());
                            if (null != mCallback) {
                                mCallback.onSttFail(resp.getCode(), resp.getMessage());
                            }
                            return;
                        }
                        if (resp.getData() != null) {
                            if (resp.getData().getResult() != null) {
                                if (resp.getData().getStatus() == 2) {

                                } else {
                                    String result = getContent(resp.getData().getResult().toString());
                                    if (!TextUtils.isEmpty(result) && null != mCallback) {
                                        mCallback.onSttResult(result, mIsFinished);
                                    }
                                }
                            }

                        }
                    }
                }

                @Override
                public void onMessage(ByteBuffer bytes) {
                    try {
                        Log.i(Constants.TAG, "\t服务端返回：" + new String(bytes.array(), "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
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
                    wsCloseFlag = true;
                }
            };

            Log.e(Constants.TAG, "正在连接...");
            mWebSocketClient.connectBlocking();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(Constants.TAG, "exception:" + e.getMessage());
        }
    }

    public synchronized void stt(byte[] bytes) {
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (null == mWebSocketClient) {
                        initWebSocketClient();
                    }
                    long curTime = System.nanoTime();
                    if (!mWebSocketClient.isOpen()) {
                        Log.e(Constants.TAG, "正在连接...");
                        mWebSocketClient.reconnectBlocking();
                        curTime = System.nanoTime();
                    }

                    if (!mWebSocketClient.isOpen()) {
                        Log.e(Constants.TAG, "STT connection is closed, discard audio frame!");
                        return;
                    }
                    long diffMs = (curTime - mLastSendTime) / 1000 / 1000;
                    if (diffMs < Constants.XF_REQUEST_INTERVAL) {
                        Thread.sleep(Constants.XF_REQUEST_INTERVAL - diffMs);
                    }
                    if (null == bytes) {
                        mFrameIndex = FRAME_INDEX_LAST;
                    }
                    switch (mFrameIndex) {
                        case FRAME_INDEX_FIRST:
                            JsonObject frame = new JsonObject();
                            //第一帧必须发送
                            JsonObject business = new JsonObject();
                            //第一帧必须发送
                            JsonObject common = new JsonObject();
                            //每一帧都要发送
                            JsonObject data = new JsonObject();
                            // 填充common
                            common.addProperty("app_id", BuildConfig.XF_STT_APP_ID);

                            //填充business
                            business.addProperty("aue", "raw");
                            business.addProperty("language", "zh_cn");
                            business.addProperty("domain", "ist_ed_open");
                            business.addProperty("accent", "mandarin");
                            business.addProperty("rate", "16000");
                            business.addProperty("dwa", "wpgs");
                            business.addProperty("spkdia", 2);

                            data.addProperty("status", FRAME_INDEX_FIRST);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                data.addProperty("audio", Base64.getEncoder().encodeToString(Arrays.copyOf(bytes, bytes.length)));
                            }
                            //填充frame
                            frame.add("common", common);
                            frame.add("business", business);
                            frame.add("data", data);

                            mWebSocketClient.send(frame.toString());
                            mFrameIndex = FRAME_INDEX_CONTINUE;
                            break;

                        case FRAME_INDEX_CONTINUE:
                            JsonObject continueFrame = new JsonObject();
                            JsonObject data1 = new JsonObject();
                            data1.addProperty("status", FRAME_INDEX_CONTINUE);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                data1.addProperty("audio", Base64.getEncoder().encodeToString(Arrays.copyOf(bytes, bytes.length)));
                            }
                            continueFrame.add("data", data1);
                            mWebSocketClient.send(continueFrame.toString());
                            break;

                        case FRAME_INDEX_LAST:
                            JsonObject lastFrame = new JsonObject();
                            JsonObject data2 = new JsonObject();
                            data2.addProperty("status", FRAME_INDEX_LAST);
                            data2.addProperty("encoding", "raw");
                            lastFrame.add("data", data2);
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
        });
    }

    public void close() {
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (null != mWebSocketClient) {
                        mWebSocketClient.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mWebSocketClient = null;
            }
        });
    }


    // 把转写结果解析为句子
    private String getContent(String message) {
        boolean isEnd = false;
        StringBuilder resultBuilder = new StringBuilder();
        try {
            JSONObject messageObj = JSON.parseObject(message);
            if (messageObj.getBooleanValue("ls")) {
                mIsFinished = true;
            }

            if (messageObj.getBooleanValue("sub_end")) {
                isEnd = true;
            }
            JSONArray wsArr = messageObj.getJSONArray("ws");

            for (int i = 0; i < wsArr.size(); i++) {
                JSONObject wsArrObj = wsArr.getJSONObject(i);
                JSONArray cwArr = wsArrObj.getJSONArray("cw");
                for (int k = 0; k < cwArr.size(); k++) {
                    JSONObject cwArrObj = cwArr.getJSONObject(k);
                    String wStr = cwArrObj.getString("w");
                    resultBuilder.append(wStr);
                }

            }
        } catch (Exception e) {
            return message;
        }
        if (isEnd) {
            return resultBuilder.toString();
        } else {
            return null;
        }
    }

    public static class ResponseData {
        private int code;
        private String message;
        private String sid;
        private Data data;

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return this.message;
        }

        public String getSid() {
            return sid;
        }

        public Data getData() {
            return data;
        }
    }

    public static class Data {
        private int status;
        private JsonObject result;

        public int getStatus() {
            return status;
        }

        public JsonObject getResult() {
            return result;
        }
    }
}
