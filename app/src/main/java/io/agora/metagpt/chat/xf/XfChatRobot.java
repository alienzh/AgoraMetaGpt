package io.agora.metagpt.chat.xf;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import io.agora.metagpt.BuildConfig;
import io.agora.metagpt.chat.ChatRobotBase;
import io.agora.metagpt.utils.Constants;
import okhttp3.HttpUrl;

public class XfChatRobot extends ChatRobotBase {
    private WebSocketClient mWebSocketClient;

    public boolean mWsCloseFlag;

    private long mLastSendTime = 0;
    private String mCurAnswer = "";

    private String mUid;

    public XfChatRobot() {
        mWsCloseFlag = true;
        mUid = "123";
    }

    @Override
    public synchronized void requestChat(JSONArray messageJsonArray) {
        super.requestChat(messageJsonArray);
        try {
            if (null == mWebSocketClient) {
                initWebSocketClient();
            }
            Log.e(Constants.TAG, "正在连接...");
            mWebSocketClient.reconnectBlocking();
            long curTime = System.nanoTime();

            if (!mWebSocketClient.isOpen()) {
                Log.e(Constants.TAG, "STT connection is closed, discard audio frame!");
                return;
            }
            long diffMs = (curTime - mLastSendTime) / 1000 / 1000;
            if (diffMs < Constants.INTERVAL_XF_REQUEST) {
                Thread.sleep(Constants.INTERVAL_XF_REQUEST - diffMs);
            }
            mLastSendTime = System.nanoTime();

            if (null != mCallback) {
                mCallback.onChatRequestStart("讯飞GPT请求：" + messageJsonArray.toJSONString());
            }

            mWebSocketClient.send(genRequestString(mUid, messageJsonArray));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void requestChat(String message) {
        super.requestChat(message);
    }

    public void initWebSocketClient() {
        try {
            URI url = new URI(getAuthorizationUrl(BuildConfig.XF_GPT_HOST, BuildConfig.XF_GPT_API_KEY, BuildConfig.XF_GPT_API_SECRET).replace("https://", "wss://"));

            mWebSocketClient = new WebSocketClient(url, new Draft_6455(), null, 5000) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    Log.i(Constants.TAG, "ws建立连接成功...");
                    mWsCloseFlag = false;
                }

                @Override
                public void onMessage(String text) {
                    Log.i(Constants.TAG, "XfGPT response: " + text);
                    try {
                        JSONObject msgObj = JSON.parseObject(text);
                        JSONObject header = msgObj.getJSONObject("header");
                        int code = header.getInteger("code");
                        String codeMsg = header.getString("message");
                        int status = header.getInteger("status");
                        String answer = codeMsg;
                        if (code == 0) {
                            JSONObject payload = msgObj.getJSONObject("payload");
                            JSONObject choices = payload.getJSONObject("choices");
                            JSONArray texts = choices.getJSONArray("text");
                            if (!texts.isEmpty()) {
                                JSONObject t1 = texts.getJSONObject(0);
                                answer = t1.getString("content");
                                mCurAnswer = mCurAnswer + answer;
                            }
                        }
                        Log.i(Constants.TAG, "XfGpt response, status: " + status + ", code: " + code + ", answer: " + answer);
                        if (status == 2) {
                            if (mCallback != null) {
                                if (code == 0) {
                                    mCallback.onChatAnswer(code, mCurAnswer);
                                } else {
                                    mCallback.onChatAnswer(code, codeMsg);
                                }

                                mCallback.onChatRequestFinish();
                            }
                            mCurAnswer = "";
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
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
    public void close() {
        try {
            if (null != mWebSocketClient) {
                mWebSocketClient.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mWebSocketClient = null;
    }

    //鉴权url
    private String getAuthorizationUrl(String hostUrl, String apikey, String apisecret) throws Exception {
        //获取host
        URL url = new URL(hostUrl);
        //获取鉴权时间 date
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        System.out.println("format:\n" + format);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());
        //获取signature_origin字段
        StringBuilder builder = new StringBuilder("host: ").append(url.getHost()).append("\n").
                append("date: ").append(date).append("\n").
                append("GET ").append(url.getPath()).append(" HTTP/1.1");
        System.out.println("signature_origin:\n" + builder);
        //获得signatue
        Charset charset = Charset.forName("UTF-8");
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec sp = new SecretKeySpec(apisecret.getBytes(charset), "hmacsha256");
        mac.init(sp);
        byte[] basebefore = mac.doFinal(builder.toString().getBytes(charset));
        String signature = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            signature = Base64.getEncoder().encodeToString(basebefore);
        }
        //获得 authorization_origin
        String authorization_origin = String.format("api_key=\"%s\",algorithm=\"%s\",headers=\"%s\",signature=\"%s\"", apikey, "hmac-sha256", "host date request-line", signature);
        //获得authorization
        String authorization = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            authorization = Base64.getEncoder().encodeToString(authorization_origin.getBytes(charset));
        }
        //获取httpurl
        HttpUrl httpUrl = HttpUrl.parse("https://" + url.getHost() + url.getPath()).newBuilder().//
                addQueryParameter("authorization", authorization).//
                addQueryParameter("date", date).//
                addQueryParameter("host", url.getHost()).//
                build();

        return httpUrl.toString();
    }

    private String genRequestString(String uid, JSONArray msges) {
        JSONObject frame = new JSONObject();
        JSONObject header = new JSONObject();
        JSONObject chat = new JSONObject();
        JSONObject parameter = new JSONObject();
        JSONObject payload = new JSONObject();
        JSONObject message = new JSONObject();
        JSONObject text = new JSONObject();
        // JSONArray ja = new JSONArray();

        //填充header
        header.put("app_id", BuildConfig.XF_GPT_APP_ID);
        header.put("uid", uid);
        //填充parameter
        chat.put("domain", "general");
        chat.put("random_threshold", 0);
        chat.put("max_tokens", 1024);
        chat.put("auditing", "default");
        parameter.put("chat", chat);
        message.put("text", msges);
        payload.put("message", message);
        frame.put("header", header);
        frame.put("parameter", parameter);
        frame.put("payload", payload);

        return frame.toString();
    }

}
