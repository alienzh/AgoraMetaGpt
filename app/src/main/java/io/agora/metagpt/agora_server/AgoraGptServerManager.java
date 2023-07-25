package io.agora.metagpt.agora_server;

import android.os.Build;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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

import io.agora.metagpt.net.HttpURLRequest;
import io.agora.metagpt.utils.Constants;
import io.agora.metagpt.utils.RingBuffer;

public class AgoraGptServerManager {
    private final static String TAG = Constants.TAG + "-" + AgoraGptServerManager.class.getName();
    private final RingBuffer mRingBuffer;
    private final Object mRingBufferLock;

    private final ExecutorService mExecutorService;
    private final ExecutorService mExecutorCacheService;
    private String mTempPcmPath;

    private OutputStream mPcmOutputStream;

    private StringBuilder mServerResponse;

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
    }

    public void init(String pcmPath) {
        mTempPcmPath = pcmPath;
    }

    public void sendPcmData(byte[] bytes) {
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
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
                            handleResponseData();
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
        });
    }

    protected synchronized void handleResponseData() {
        try {
            JSONObject jsonObject = JSONObject.parseObject(mServerResponse.toString());
            String action = jsonObject.getString("action");
            int code = jsonObject.getInteger("code");
            String sid = jsonObject.getString("sid");
            String desc = jsonObject.getString("desc");
            byte[] textBase64Decode = null;
            if ("started".equals(action)) {
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
            } else if ("result".equals(action)) {
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
