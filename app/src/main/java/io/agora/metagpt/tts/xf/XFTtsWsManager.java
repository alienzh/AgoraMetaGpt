package io.agora.metagpt.tts.xf;

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
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import io.agora.metagpt.BuildConfig;
import io.agora.metagpt.utils.Constants;
import okhttp3.HttpUrl;

public class XFTtsWsManager {

    private WebSocketClient mWebSocketClient;

    private final ExecutorService mExecutorService;

    private static final String TTE = "UTF8";

    private static final String VCN = "xiaoyan";

    private final Gson gson;

    private TtsCallback mCallback;

    private OutputStream mOutputStream;

    public boolean wsCloseFlag;

    private String mTempPcmPath;

    private XFTtsWsManager() {
        mExecutorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        gson = new Gson();
        mOutputStream = null;
        wsCloseFlag = true;
    }

    private static final class InstanceHolder {
        static final XFTtsWsManager M_INSTANCE = new XFTtsWsManager();
    }

    public static XFTtsWsManager getInstance() {
        return InstanceHolder.M_INSTANCE;
    }

    public void setCallback(TtsCallback callback) {
        mCallback = callback;
    }

    public void initWebSocketClient(final String path) {
        mTempPcmPath = path;
        try {
            String wsUrl = getAuthUrl(BuildConfig.XF_TTS_HOST, BuildConfig.XF_API_KEY, BuildConfig.XF_API_SECRET).replace("https://", "wss://");
            mWebSocketClient = new WebSocketClient(new URI(wsUrl)) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    Log.i(Constants.TAG, "ws建立连接成功...");
                }

                @Override
                public void onMessage(String text) {
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
                                    mOutputStream = Files.newOutputStream(Paths.get(path));
                                } else {
                                    mOutputStream = new FileOutputStream(path);
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
                            if (null != mCallback) {
                                mCallback.onTtsFinish();
                            }
                            wsCloseFlag = true;
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


    public void tts(final String msg) {
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                if (wsCloseFlag) {
                    mWebSocketClient.reconnect();
                    try {
                        int waitCount = 0;
                        while (!mWebSocketClient.getReadyState().equals(ReadyState.OPEN)) {
                            if (waitCount > 50) {
                                initWebSocketClient(mTempPcmPath);
                            }
                            Log.e(Constants.TAG, "正在连接...");
                            Thread.sleep(100);
                            waitCount++;
                        }
                    } catch (InterruptedException e) {
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
                                "    \"vcn\": \"" + VCN + "\",\n" +
                                "    \"pitch\": 50,\n" +
                                "    \"speed\": 50\n" +
                                "  },\n" +
                                "  \"data\": {\n" +
                                "    \"status\": 2,\n" +
                                "    \"text\": \"" + Base64.getEncoder().encodeToString(msg.getBytes(StandardCharsets.UTF_8)) + "\"\n" +
                                "  }\n" +
                                "}";
                    }
                    if (null != mCallback) {
                        mCallback.onTtsStart(msg);
                    }
                    mWebSocketClient.send(requestJson);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void close() {
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

    public interface TtsCallback {
        void onTtsStart(String text);

        void onTtsFinish();
    }
}
