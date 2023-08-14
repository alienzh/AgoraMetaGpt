package io.agora.gpt.chat.bigmodel;

import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import io.agora.gpt.BuildConfig;
import io.agora.gpt.chat.ChatRobotBase;
import io.agora.gpt.models.chat.bigmodel.ChatGlmRequestBody;
import io.agora.gpt.models.chat.bigmodel.Event;
import io.agora.gpt.net.HttpURLRequest;
import io.agora.gpt.utils.Constants;
import io.agora.gpt.utils.ErrorCode;
import io.agora.gpt.utils.Utils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class BigModelChatRobot extends ChatRobotBase {

    private String token;

    public BigModelChatRobot() {
        generateToken();
    }

    private void generateToken() {
        mExecutorCacheService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String[] apiKeyParts = BuildConfig.BIG_MODEL_API_KEY.split("\\.");
                    if (apiKeyParts.length != 2) {
                        throw new IllegalArgumentException("invalid apiKey");
                    }
                    String apiId = apiKeyParts[0];
                    String secret = apiKeyParts[1];

                    Map<String, Object> header = new HashMap<>(2);
                    header.put("alg", "HS256");
                    header.put("sign_type", "SIGN");

                    long currentTimeMillis = System.currentTimeMillis();
                    long expMillis = currentTimeMillis + (1000 * 60 * 60 * 24);

                    Map<String, Object> payload = new HashMap<>(3);
                    payload.put("api_key", apiId);
                    payload.put("exp", expMillis);
                    payload.put("timestamp", currentTimeMillis);

                    token = Jwts.builder()
                            .setHeader(header)
                            .setClaims(payload)
                            .signWith(SignatureAlgorithm.HS256, secret.getBytes(StandardCharsets.UTF_8))
                            .compact();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(Constants.TAG, e.getMessage());
                }
            }
        });

    }


    @Override
    public void requestChat(JSONArray messageJsonArray) {
        super.requestChat(messageJsonArray);
        if (TextUtils.isEmpty(token)) {
            Log.e(Constants.TAG, "Big model token is empty");
            return;
        }
        switch (mModelIndex) {
            case Constants.AI_PLATFORM_BIG_MODEL_CHATGLM_PRO:
            case Constants.AI_PLATFORM_BIG_MODEL_CHATGLM_STD:
            case Constants.AI_PLATFORM_BIG_MODEL_CHATGLM_LITE:
                requestChatGlm(messageJsonArray);
                break;
            default:
                break;
        }
    }

    private void requestChatGlm(JSONArray messageJsonArray) {
        mExecutorCacheService.execute(new Runnable() {
            @Override
            public void run() {
                mRequestThreadIds.add(Thread.currentThread().getId());
                ChatGlmRequestBody body = new ChatGlmRequestBody();
                body.setPrompt(messageJsonArray);
                body.setTemperature(Constants.GPT_TEMPERATURE);
                body.setTop_p(Constants.GPT_TOP_P);
                body.setRequest_id(Utils.getSessionId());
                body.setIncremental(true);

                if (null != mCallback) {
                    mCallback.onChatRequestStart("请求BigModel:" + messageJsonArray.toJSONString());
                }
                Log.i(Constants.TAG, "big model requestChatGlm:" + JSON.toJSONString(body));
                HttpURLRequest request = new HttpURLRequest();
                request.setCallback(new HttpURLRequest.RequestCallback() {
                    @Override
                    public void updateResponseData(byte[] bytes, int len, boolean isFirstResponse) {
                        if (!mRequestThreadIds.contains(Thread.currentThread().getId())) {
                            return;
                        }
                        try {
                            String line = new String(bytes, 0, len, StandardCharsets.UTF_8);
                            Event event = parseEvent(line);
                            Log.i(Constants.TAG, "Big Model response:" + event);

                            if ("add".equals(event.getEvent())) {
                                if (null != mCallback && !TextUtils.isEmpty(event.getData())) {
                                    mCallback.onChatAnswer(ErrorCode.ERROR_NONE, event.getData());
                                }
                            } else if ("finish".equals(event.getEvent())) {
                                //nothing to do
                            } else if ("error".equals(event.getEvent()) || "interrupted".equals(event.getEvent())) {
                                if (null != mCallback) {
                                    mCallback.onChatAnswer(ErrorCode.ERROR_GENERAL, event.getData());
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void requestFail(int errorCode, String msg) {
                        if (!mRequestThreadIds.contains(Thread.currentThread().getId())) {
                            return;
                        }
                        if (null != mCallback) {
                            mCallback.onChatAnswer(errorCode, msg);
                        }
                    }

                    @Override
                    public void requestFinish() {
                        if (!mRequestThreadIds.contains(Thread.currentThread().getId())) {
                            return;
                        }
                        if (null != mCallback) {
                            mCallback.onChatRequestFinish();
                        }
                    }
                });

                Map<String, String> params = new HashMap<>(3);
                params.put("Content-Type", "application/json;charset=utf-8");
                params.put("Accept", "text/event-stream");
                params.put("Authorization", token);

                String url = BuildConfig.BIG_MODEL_SERVER_HOST.replace(Constants.REPLACE_GPT_MODEL_NAME_LABEL, getModelName());
                request.requestPostUrl(url, params, JSON.toJSONString(body), true);
                mHttpUrlRequests.add(request);

            }
        });
    }

    private Event parseEvent(String eventStr) {
        Event event = new Event();
        String[] lines = eventStr.split("\n");
        for (String line : lines) {
            if (line.startsWith("data:")) {
                event.setData(line.substring(line.indexOf(":") + 1));
            } else if (line.startsWith("id:")) {
                event.setId(line.substring(line.indexOf(":") + 1));
            } else if (line.startsWith("event:")) {
                event.setEvent(line.substring(line.indexOf(":") + 1));
            } else if (line.startsWith("meta:")) {
                event.setMeta(line.substring(line.indexOf(":") + 1));
            }
        }
        return event;
    }

    @Override
    public void requestChatKeyInfo(JSONArray messageJsonArray) {
        super.requestChatKeyInfo(messageJsonArray);
        mExecutorCacheService.execute(new Runnable() {
            @Override
            public void run() {
                ChatGlmRequestBody body = new ChatGlmRequestBody();
                body.setPrompt(messageJsonArray);
                body.setTemperature(Constants.GPT_TEMPERATURE);
                body.setTop_p(Constants.GPT_TOP_P);
                body.setRequest_id(Utils.getSessionId());
                body.setIncremental(false);

                Log.i(Constants.TAG, "big model requestChatKeyInfo:" + JSON.toJSONString(body));
                HttpURLRequest request = new HttpURLRequest();
                request.setCallback(new HttpURLRequest.RequestCallback() {
                    @Override
                    public void onHttpResponse(String responseTxt) {
                        try {
                            Event event = parseEvent(responseTxt);
                            Log.i(Constants.TAG, "Big Model response:" + event);

                            if ("add".equals(event.getEvent())) {
                                //nothing to do
                            } else if ("finish".equals(event.getEvent())) {
                                if (null != mCallback && !TextUtils.isEmpty(event.getData())) {
                                    mCallback.onChatKeyInfoUpdate(event.getData());
                                }
                            } else if ("error".equals(event.getEvent()) || "interrupted".equals(event.getEvent())) {
                                Log.i(Constants.TAG, "Big Model requestChatKeyInfo error msg:" + event.getData());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void requestFail(int errorCode, String msg) {
                        Log.i(Constants.TAG, "Big Model requestChatKeyInfo requestFail errorCode:" + errorCode + ",msg:" + msg);
                    }

                    @Override
                    public void requestFinish() {
                        Log.i(Constants.TAG, "Big Model requestChatKeyInfo requestFinish");
                    }
                });

                Map<String, String> params = new HashMap<>(3);
                params.put("Content-Type", "application/json;charset=utf-8");
                params.put("Accept", "text/event-stream");
                params.put("Authorization", token);

                String url = BuildConfig.BIG_MODEL_SERVER_HOST.replace(Constants.REPLACE_GPT_MODEL_NAME_LABEL, getModelName());
                request.requestPostUrl(url, params, JSON.toJSONString(body), false);
                mHttpUrlRequests.add(request);

            }
        });

    }

    private String getModelName() {
        String modelName = Constants.MODEL_BIT_MODEL_CHATGLM_PRO;
        if (mModelIndex == Constants.AI_PLATFORM_BIG_MODEL_CHATGLM_PRO) {
            modelName = Constants.MODEL_BIT_MODEL_CHATGLM_PRO;
        } else if (mModelIndex == Constants.AI_PLATFORM_BIG_MODEL_CHATGLM_STD) {
            modelName = Constants.MODEL_BIT_MODEL_CHATGLM_STD;
        } else if (mModelIndex == Constants.AI_PLATFORM_BIG_MODEL_CHATGLM_LITE) {
            modelName = Constants.MODEL_BIT_MODEL_CHATGLM_LITE;
        }
        return modelName;
    }
}
