package io.agora.metagpt.stt.xf;

import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.handshake.ServerHandshake;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.ByteBuffer;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


import io.agora.metagpt.BuildConfig;
import io.agora.metagpt.utils.Constants;
import io.agora.metagpt.utils.EncryptUtil;

public class XFSttWsManager {
    private WebSocketClient mWebSocketClient;

    private final ExecutorService mExecutorService;

    public boolean wsCloseFlag;

    private SttCallback mCallback;

    private boolean mIsFinished;

    private long mOfflineTime = 0;
    private long mLastSendTime = 0;

    private XFSttWsManager() {
        mExecutorService = new ThreadPoolExecutor(1, 1,
                0, TimeUnit.MILLISECONDS,
                new LinkedBlockingDeque<Runnable>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        wsCloseFlag = true;
        mIsFinished = false;
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
            URI url = new URI(BuildConfig.XF_STT_HOST + getHandShakeParams(BuildConfig.XF_STT_APP_ID, BuildConfig.XF_STT_API_KEY));

            mWebSocketClient = new WebSocketClient(url, new Draft_6455(), null, 5000) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    Log.i(Constants.TAG, "ws建立连接成功...");
                    wsCloseFlag = false;
                }

                @Override
                public void onMessage(String text) {
                    JSONObject msgObj = JSON.parseObject(text);
                    String action = msgObj.getString("action");
                    if (Objects.equals("started", action)) {
                        // 握手成功
                        Log.i(Constants.TAG, "握手成功！sid: " + msgObj.getString("sid"));

                    } else if (Objects.equals("result", action)) {
                        mIsFinished = false;
                        // 转写结果
                        String result = getContent(msgObj.getString("data"));
                        if (!TextUtils.isEmpty(result) && null != mCallback) {
                            mCallback.onSttResult(result, mIsFinished);
                        }
                    } else if (Objects.equals("error", action)) {
                        // 连接发生错误
                        Log.i(Constants.TAG, "error: " + text);
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
                    mOfflineTime = System.currentTimeMillis();
                    Log.e(Constants.TAG, "ws链接已关闭，本次请求完成...");
                    wsCloseFlag = true;
                }

                @Override
                public void onError(Exception e) {
                    mOfflineTime = System.currentTimeMillis();
                    Log.e(Constants.TAG, "发生错误 " + e.getMessage());
                    wsCloseFlag = true;
                }
            };

            Log.e(Constants.TAG, "正在连接...");
            mOfflineTime = System.currentTimeMillis();
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
                    long curTime = System.currentTimeMillis();
                    if (!mWebSocketClient.isOpen()) {
//                        if (curTime - mOfflineTime < 5000) {
//                            Thread.sleep(5000 - curTime + mOfflineTime);
//                        }
                        Log.e(Constants.TAG, "正在连接...");
//                        mOfflineTime = System.currentTimeMillis();
                        mWebSocketClient.reconnectBlocking();
                        curTime = System.currentTimeMillis();
                    }

                    if (!mWebSocketClient.isOpen()) {
                        Log.e(Constants.TAG, "STT connection is closed, discard audio frame!");
                        return;
                    }
                    if (curTime - mLastSendTime < 40) {
                        Thread.sleep(40 - curTime + mLastSendTime);
                    }
                    mLastSendTime = System.currentTimeMillis();
                    mWebSocketClient.send(bytes);
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

    // 生成握手参数
    private String getHandShakeParams(String appId, String secretKey) {
        String ts = System.currentTimeMillis() / 1000 + "";
        String signa = "";
        try {
            signa = EncryptUtil.HmacSHA1Encrypt(EncryptUtil.MD5(appId + ts), secretKey);
            return "?appid=" + appId + "&ts=" + ts + "&signa=" + URLEncoder.encode(signa, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
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
            JSONObject cn = messageObj.getJSONObject("cn");
            JSONObject st = cn.getJSONObject("st");
            //0-最终结果；1-中间结果
            if ("0".equals(st.getString("type"))) {
                isEnd = true;
            }
            JSONArray rtArr = st.getJSONArray("rt");
            for (int i = 0; i < rtArr.size(); i++) {
                JSONObject rtArrObj = rtArr.getJSONObject(i);
                JSONArray wsArr = rtArrObj.getJSONArray("ws");
                for (int j = 0; j < wsArr.size(); j++) {
                    JSONObject wsArrObj = wsArr.getJSONObject(j);
                    JSONArray cwArr = wsArrObj.getJSONArray("cw");
                    for (int k = 0; k < cwArr.size(); k++) {
                        JSONObject cwArrObj = cwArr.getJSONObject(k);
                        String wStr = cwArrObj.getString("w");
                        resultBuilder.append(wStr);
                    }
                }
            }
        } catch (Exception e) {
            return message;
        }
        String msg = resultBuilder.toString();
        Log.i(Constants.TAG, "STT isEnd: " + isEnd + ", getContent: " + msg);
        if (!isEnd) {
            return null;
        }

        return msg;
    }


    public interface SttCallback {
        void onSttResult(String text, boolean isFinish);
    }
}
