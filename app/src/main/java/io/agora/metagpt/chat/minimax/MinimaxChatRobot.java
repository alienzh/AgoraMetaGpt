package io.agora.metagpt.chat.minimax;

import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.agora.metagpt.BuildConfig;
import io.agora.metagpt.chat.ChatRobotBase;
import io.agora.metagpt.models.chat.minimax.MinimaxChatCompletionProRequestBody;
import io.agora.metagpt.models.chat.minimax.MinimaxChatCompletionRequestBody;
import io.agora.metagpt.models.chat.minimax.MinimaxProBotSetting;
import io.agora.metagpt.models.chat.minimax.MinimaxReplyConstraints;
import io.agora.metagpt.models.chat.minimax.MinimaxRoleMeta;
import io.agora.metagpt.net.HttpURLRequest;
import io.agora.metagpt.utils.Config;
import io.agora.metagpt.utils.Constants;
import io.agora.metagpt.utils.ErrorCode;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MinimaxChatRobot extends ChatRobotBase {

    @Override
    public void requestChat(JSONArray messageJsonArray) {
        super.requestChat(messageJsonArray);
        switch (mModelIndex) {
            case Constants.AI_PLATFORM_MINIMAX_CHAT_COMPLETION_5:
            case Constants.AI_PLATFORM_MINIMAX_CHAT_COMPLETION_55:
                requestChatCompletion(messageJsonArray);
                break;
            case Constants.AI_PLATFORM_MINIMAX_CHAT_COMPLETION_PRO_55:
                requestChatCompletionPro(messageJsonArray);
                break;
            default:
                break;
        }
    }

    private void requestChatCompletion(JSONArray messageJsonArray) {
        mExecutorCacheService.execute(new Runnable() {
            @Override
            public void run() {
                mRequestThreadIds.add(Thread.currentThread().getId());
                MinimaxChatCompletionRequestBody body = new MinimaxChatCompletionRequestBody();
                body.setModel(getModelName());
                body.setMessages(messageJsonArray);
                body.setTokens_to_generate(Constants.GPT_MAX_TOKENS);
                body.setTemperature(Constants.GPT_TEMPERATURE);
                body.setTop_p(Constants.GPT_TOP_P);
                body.setStream(Config.GPT_REQUEST_STREAM);
                if (null != mChatBotRole) {
                    body.setPrompt(mChatBotRole.getChatBotPrompt());

                    MinimaxRoleMeta roleMeta = new MinimaxRoleMeta();
                    roleMeta.setBot_name(mChatBotRole.getChatBotName());
                    roleMeta.setUser_name(mChatBotRole.getChatBotUserName());
                    body.setRole_meta(roleMeta);
                }

                if (null != mCallback) {
                    mCallback.onChatRequestStart("请求Minimax chat Completion5:" + messageJsonArray.toJSONString());
                }

                if (Config.GPT_REQUEST_STREAM) {
                    HttpURLRequest request = new HttpURLRequest();
                    request.setCallback(new HttpURLRequest.RequestCallback() {
                        @Override
                        public void updateResponseData(byte[] bytes, int len, boolean isFirstResponse) {
                            if (!mRequestThreadIds.contains(Thread.currentThread().getId())) {
                                return;
                            }
                            try {
                                String line = new String(bytes, 0, len, "utf-8");
                                Log.i(Constants.TAG, "minimax response:" + line);
                                JSONObject jsonObject = JSONObject.parseObject(line.replace("data: ", ""));
                                JSONArray jsonArray = JSONArray.parseArray(jsonObject.getString("choices"));
                                if (null != jsonArray && jsonArray.size() > 0) {
                                    line = ((JSONObject) jsonArray.get(0)).getString("delta");
                                    if (!TextUtils.isEmpty(line)) {
                                        if (null != mCallback) {
                                            mCallback.onChatAnswer(ErrorCode.ERROR_NONE, line);
                                        }
                                    } else {
                                        if ("length".equals(((JSONObject) jsonArray.get(0)).getString("finish_reason"))) {
                                            if (null != mCallback) {
                                                mCallback.onChatAnswer(ErrorCode.ERROR_CHAT_LENGTH_LIMIT, "");
                                            }
                                        }
                                    }
                                } else {
                                    jsonObject = JSONObject.parseObject(jsonObject.getString("base_resp"));
                                    if ("rate limit".equals(jsonObject.getString("status_msg"))) {
                                        if (null != mCallback) {
                                            mCallback.onChatAnswer(jsonObject.getInteger("status_code"), "minimax 请求错误 rate limit");
                                        }
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
                    params.put("Authorization", "Bearer " + BuildConfig.MINIMAX_AUTHORIZATION);

                    String url = BuildConfig.MINIMAX_SERVER_HOST + "v1/text/chatcompletion?GroupId=" + BuildConfig.MINIMAX_GROUP_ID;
                    request.requestPostUrl(url, params, JSON.toJSONString(body));
                    mHttpUrlRequests.add(request);
                } else {
                    MiniMaxRetrofitManager.getInstance().getMinimaxRequest().getMinimaxChatCompletionResponse(BuildConfig.MINIMAX_GROUP_ID, body)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Observer<JSONObject>() {
                                @Override
                                public void onSubscribe(Disposable d) {

                                }

                                @Override
                                public void onNext(JSONObject response) {
                                    String reply = response.getString("reply");
                                    if (null != mCallback) {
                                        mCallback.onChatAnswer(0, reply);
                                    }
                                }

                                @Override
                                public void onError(Throwable e) {
                                    Log.i(Constants.TAG, "requestMinimaxChatCompletion5 error=" + e);
                                    if (null != mCallback) {
                                        mCallback.onChatAnswer(-1, e.getMessage());
                                    }
                                }

                                @Override
                                public void onComplete() {

                                }
                            });
                }
            }
        });
    }

    private void requestChatCompletionPro(JSONArray messageJsonArray) {
        mExecutorCacheService.execute(new Runnable() {
            @Override
            public void run() {
                mRequestThreadIds.add(Thread.currentThread().getId());
                MinimaxChatCompletionProRequestBody body = new MinimaxChatCompletionProRequestBody();
                body.setModel(getModelName());
                body.setStream(true);
                body.setTokens_to_generate(Constants.GPT_MAX_TOKENS);
                body.setTemperature(Constants.GPT_TEMPERATURE);
                body.setTop_p(Constants.GPT_TOP_P);
                body.setMask_sensitive_info(false);
                body.setMessages(messageJsonArray);
                if (null != mChatBotRole) {
                    JSONArray botSettingJson = new JSONArray();
                    MinimaxProBotSetting minimaxProBotSetting = new MinimaxProBotSetting();
                    minimaxProBotSetting.setBot_name(mChatBotRole.getChatBotName());
                    minimaxProBotSetting.setContent(mChatBotRole.getChatBotPrompt());
                    botSettingJson.add(JSON.toJSON(minimaxProBotSetting));
                    body.setBot_setting(botSettingJson);

                    MinimaxReplyConstraints replyConstraints = new MinimaxReplyConstraints();
                    replyConstraints.setSender_type(Constants.MINIMAX_SENDER_TYPE_BOT);
                    replyConstraints.setSender_name(mChatBotRole.getChatBotName());
                    body.setReply_constraints(replyConstraints);
                }

                if (null != mCallback) {
                    mCallback.onChatRequestStart("请求Minimax chat Completion5:" + messageJsonArray.toJSONString());
                }

                HttpURLRequest request = new HttpURLRequest();
                request.setCallback(new HttpURLRequest.RequestCallback() {
                    @Override
                    public void updateResponseData(byte[] bytes, int len, boolean isFirstResponse) {
                        if (!mRequestThreadIds.contains(Thread.currentThread().getId())) {
                            return;
                        }
                        try {
                            String line = new String(bytes, 0, len, "utf-8");
                            Log.i(Constants.TAG, "minimax pro response:" + line);
                            JSONObject jsonObject = JSONObject.parseObject(line.replace("data: ", ""));
                            JSONArray jsonArray = JSONArray.parseArray(jsonObject.getString("choices"));
                            if (null != jsonArray && jsonArray.size() > 0) {
                                JSONObject firstChoiceJson = ((JSONObject) jsonArray.get(0));
                                if ("length".equals(((JSONObject) jsonArray.get(0)).getString("finish_reason"))) {
                                    if (null != mCallback) {
                                        mCallback.onChatAnswer(ErrorCode.ERROR_CHAT_LENGTH_LIMIT, "");
                                    }
                                } else if ("max_output".equals(((JSONObject) jsonArray.get(0)).getString("finish_reason"))) {
                                    if (null != mCallback) {
                                        mCallback.onChatAnswer(ErrorCode.ERROR_CHAT_MAX_OUTPUT, "输入+模型输出内容超过模型最大能力限制");
                                    }
                                } else if ("stop".equals(((JSONObject) jsonArray.get(0)).getString("finish_reason"))) {
                                    //stop gpt stream return
                                } else {
                                    JSONArray messageArray = firstChoiceJson.getJSONArray("messages");
                                    if (null != messageArray && jsonArray.size() > 0) {
                                        line = ((JSONObject) messageArray.get(0)).getString("text");
                                        if (!TextUtils.isEmpty(line)) {
                                            if (null != mCallback) {
                                                mCallback.onChatAnswer(ErrorCode.ERROR_NONE, line);
                                            }
                                        }
                                    }
                                }
                            } else {
                                jsonObject = JSONObject.parseObject(jsonObject.getString("base_resp"));
                                if ("rate limit".equals(jsonObject.getString("status_msg"))) {
                                    if (null != mCallback) {
                                        mCallback.onChatAnswer(jsonObject.getInteger("status_code"), "minimax 请求错误 rate limit");
                                    }
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
                params.put("Authorization", "Bearer " + BuildConfig.MINIMAX_AUTHORIZATION);

                String url = BuildConfig.MINIMAX_SERVER_HOST + "v1/text/chatcompletion_pro?GroupId=" + BuildConfig.MINIMAX_GROUP_ID;
                request.requestPostUrl(url, params, JSON.toJSONString(body));
                mHttpUrlRequests.add(request);
            }
        });
    }

    @Override
    public void requestChatKeyInfo(JSONArray messageJsonArray) {
        super.requestChatKeyInfo(messageJsonArray);
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                MinimaxChatCompletionProRequestBody body = new MinimaxChatCompletionProRequestBody();
                body.setModel(getModelName());
                body.setStream(false);
                body.setTokens_to_generate(Constants.GPT_MAX_TOKENS);
                body.setTemperature(Constants.GPT_TEMPERATURE);
                body.setTop_p(Constants.GPT_TOP_P);
                body.setMask_sensitive_info(false);
                body.setMessages(messageJsonArray);
                if (null != mChatBotRole) {
                    JSONArray botSettingJson = new JSONArray();
                    MinimaxProBotSetting minimaxProBotSetting = new MinimaxProBotSetting();
                    minimaxProBotSetting.setBot_name(mChatBotRole.getChatBotName());
                    minimaxProBotSetting.setContent(mChatBotRole.getChatBotPrompt());
                    botSettingJson.add(JSON.toJSON(minimaxProBotSetting));
                    body.setBot_setting(botSettingJson);

                    MinimaxReplyConstraints replyConstraints = new MinimaxReplyConstraints();
                    replyConstraints.setSender_type(Constants.MINIMAX_SENDER_TYPE_BOT);
                    replyConstraints.setSender_name(mChatBotRole.getChatBotName());
                    body.setReply_constraints(replyConstraints);
                }

                Log.i(Constants.TAG, "body:" + body.toString());

                MiniMaxRetrofitManager.getInstance().getMinimaxRequest().getMinimaxChatCompletionProResponse(BuildConfig.MINIMAX_GROUP_ID, body)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<JSONObject>() {
                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onNext(JSONObject response) {
                                Log.i(Constants.TAG, "request key info response:" + response);
                                String reply = response.getString("reply");
                                if (!TextUtils.isEmpty(reply)) {
                                    if (null != mCallback) {
                                        mCallback.onChatKeyInfoUpdate(reply);
                                    }
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.i(Constants.TAG, "requestMinimaxChatCompletion5 error=" + e);

                            }

                            @Override
                            public void onComplete() {

                            }
                        });
            }
        });

    }

    private String getModelName() {
        String modelName = Constants.MINIMAX_MODEL_55;
        if (mModelIndex == Constants.AI_PLATFORM_MINIMAX_CHAT_COMPLETION_55 || mModelIndex == Constants.AI_PLATFORM_MINIMAX_CHAT_COMPLETION_PRO_55) {
            modelName = Constants.MINIMAX_MODEL_55;
        } else if (mModelIndex == Constants.AI_PLATFORM_MINIMAX_CHAT_COMPLETION_5) {
            modelName = Constants.MINIMAX_MODEL_5;
        }
        return modelName;
    }
}
