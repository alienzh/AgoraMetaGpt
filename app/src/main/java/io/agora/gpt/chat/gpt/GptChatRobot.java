package io.agora.gpt.chat.gpt;

import android.util.Log;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.Arrays;

import io.agora.gpt.chat.ChatRobotBase;
import io.agora.gpt.models.chat.gpt.Gpt4RequestBody;
import io.agora.gpt.models.chat.gpt.GptRequestBody;
import io.agora.gpt.models.chat.gpt.GptResponse;
import io.agora.gpt.utils.Config;
import io.agora.gpt.utils.Constants;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class GptChatRobot extends ChatRobotBase {

    @Override
    public void requestChat(JSONArray messageJsonArray) {
        super.requestChat(messageJsonArray);
        if (mModelIndex == Constants.AI_PLATFORM_CHAT_GPT_35) {
            requestGpt(Constants.CHAT_GPT_MODEL_35, messageJsonArray);
        } else if (mModelIndex == Constants.AI_PLATFORM_CHAT_GPT_40) {
            requestGpt(Constants.CHAT_GPT_MODEL_40, messageJsonArray);
        }
    }

    @Override
    public void requestChat(String message) {
        super.requestChat(message);
        if (mModelIndex == Constants.AI_PLATFORM_CHAT_GPT_35) {
            requestGptWithQuestion(new String[]{message});
        }
    }

    private void requestGptWithQuestion(String[] question) {
        GptRequestBody body = new GptRequestBody();
        body.setQuestions(question);
        if (null != mCallback) {
            mCallback.onChatRequestStart("请求ChatGPT：" + Arrays.toString(question));
        }
        GptRetrofitManager.getInstance().getGptRequest().getGptResponse(body)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<JSONObject>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(JSONObject response) {
                        GptResponse gptResponse = JSONObject.toJavaObject(response, GptResponse.class);
                        try {
                            if (null != mCallback) {
                                mCallback.onChatAnswer(0, gptResponse.getAnswer());
                            }
                        } catch (Exception e) {
                            if (null != mCallback) {
                                mCallback.onChatAnswer(-1, e.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(Constants.TAG, "requestGpt error=" + e);
                        if (null != mCallback) {
                            mCallback.onChatAnswer(-1, e.getMessage());
                        }
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void requestGpt(String model, JSONArray message) {
        Gpt4RequestBody body = new Gpt4RequestBody();
        body.setModel(model);
        body.setTemperature(1.0f);
        body.setMessages(message);
        body.setStream(Config.GPT_REQUEST_STREAM);
        if (null != mCallback) {
            mCallback.onChatRequestStart("请求ChatGPT：" + message.toJSONString());
        }
        GptRetrofitManager.getInstance().getGptRequest().getGpt4Response(body)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<JSONObject>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(JSONObject response) {
                        GptResponse gptResponse = JSONObject.parseObject(response.toJSONString(), GptResponse.class);
                        try {
                            if (null != mCallback) {
                                mCallback.onChatAnswer(0, gptResponse.getAnswer());
                            }
                        } catch (Exception e) {
                            if (null != mCallback) {
                                mCallback.onChatAnswer(-1, e.getMessage());
                            }
                        }

                        if (null != mCallback) {
                            mCallback.onChatRequestFinish();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i(Constants.TAG, "requestGpt error=" + e);
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
