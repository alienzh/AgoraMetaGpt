package io.agora.gpt.stt.xf;

import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Objects;

import io.agora.gpt.BuildConfig;
import io.agora.gpt.stt.SttRobotBase;
import io.agora.gpt.utils.Constants;

public class XFSttRtasrRobot extends SttRobotBase {
    private WebSocketClient mWebSocketClient;
    private boolean mWsCloseFlag;

    public XFSttRtasrRobot() {
        super();
        mWsCloseFlag = true;
    }

    @Override
    public void init() {
        super.init();
        initWebSocketClient();
    }

    private void initWebSocketClient() {
        try {
            URI url = new URI(BuildConfig.XF_STT_HOST + XfAuthUtils.getRtasrHandShakeParams(BuildConfig.XF_STT_APP_ID, BuildConfig.XF_STT_API_KEY));

            mWebSocketClient = new WebSocketClient(url, new Draft_6455(), null, 5000) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    Log.i(Constants.TAG, "ws建立连接成功...");
                    mWsCloseFlag = false;
                }

                @Override
                public void onMessage(String text) {
                    Log.i(Constants.TAG, "stt返回：" + text);
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
                    Log.e(Constants.TAG, "ws链接已关闭，本次请求完成...");
                    mWsCloseFlag = true;
                }

                @Override
                public void onError(Exception e) {
                    Log.e(Constants.TAG, "发生错误 " + e.getMessage());
                    mWsCloseFlag = true;
                }
            };

            Log.e(Constants.TAG, "正在连接...");
            mWebSocketClient.connectBlocking();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(Constants.TAG, "exception:" + e.getMessage());
        }
    }

    @Override
    public synchronized void stt(byte[] bytes) {
        super.stt(bytes);
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (null == mWebSocketClient) {
                        initWebSocketClient();
                    }
                    long curTime = System.nanoTime();
                    if (!mWebSocketClient.isOpen()) {

                        Log.i(Constants.TAG, "正在连接...");
                        mWebSocketClient.reconnectBlocking();
                        curTime = System.nanoTime();
                    }

                    if (!mWebSocketClient.isOpen()) {
                        Log.e(Constants.TAG, "STT connection is closed, discard audio frame!");
                        return;
                    }

                    long diffMs = (curTime - mLastSendTime) / 1000 / 1000;
                    if (diffMs < Constants.INTERVAL_XF_REQUEST) {
                        Thread.sleep(Constants.INTERVAL_XF_REQUEST - diffMs);
                    }
                    mLastSendTime = System.nanoTime();
                    mWebSocketClient.send(bytes);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void close() {
        super.close();
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

}
