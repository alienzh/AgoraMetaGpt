package io.agora.metagpt.ai;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.agora.metagpt.BuildConfig;
import io.agora.metagpt.ai.gpt.Gpt4RequestBody;
import io.agora.metagpt.ai.gpt.GptRequestBody;
import io.agora.metagpt.ai.gpt.GptResponse;
import io.agora.metagpt.ai.gpt.GptRetrofitManager;
import io.agora.metagpt.ai.minimax.Message;
import io.agora.metagpt.ai.minimax.MiniMaxRetrofitManager;
import io.agora.metagpt.ai.minimax.MinimaxChatCompletionRequestBody;
import io.agora.metagpt.ai.minimax.MinimaxCompletionRequestBody;
import io.agora.metagpt.ai.minimax.RoleMeta;
import io.agora.metagpt.ai.xf.XFGptWsManager;
import io.agora.metagpt.models.gpt.ChatMessage;
import io.agora.metagpt.utils.Constants;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ChatRobot implements XFGptWsManager.XfGptCallback {
    public interface ChatRobotCallback {
        void onAiAnswer(int code, String answer, long costTime);
    }

    // limit request per second
    private static final long MIN_REQUEST_INTERVAL = 3000;
    private static final ExecutorService mExecutorService = new ThreadPoolExecutor(1, 1,
        0, TimeUnit.MILLISECONDS,
        new LinkedBlockingDeque<Runnable>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
    private static long mLastRequestTime = 0;

    private List<ChatMessage> mGptChatMessageList;
    private int mAiPlatformIndex;
    private ChatRobotCallback mCallback;

    public ChatRobot(int aiPlatform, ChatRobotCallback cb) {
        mGptChatMessageList = new ArrayList<>();
        mAiPlatformIndex = aiPlatform;
        mCallback = cb;
        XFGptWsManager.getInstance().setCallback(this);
    }

    @Override
    public void onXfChatResult(int code, String message) {
        notifyChatResult(code, message);
    }

    public void setAiPlatform(int aiPlatform) {
        mAiPlatformIndex = aiPlatform;
    }

    public int appendChatMessage(String role, String content) {
        mGptChatMessageList.add(new ChatMessage(role, content));
        return 0;
    }

    public String getLastChatMessage() {
        if (mGptChatMessageList.isEmpty()) {
            return "";
        }
        return mGptChatMessageList.get(mGptChatMessageList.size() - 1).getContent();
    }

    public int clearChatMessage() {
        mGptChatMessageList.clear();
        return 0;
    }

    public void requestChatGpt() {
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                if (mGptChatMessageList.size() <= 0) {
                    return;
                }
//            String[] messages = new String[mGptChatMessageList.size()];
//            for (int i = 0; i < mGptChatMessageList.size(); i++) {
//                messages[i] = JSON.toJSON(mGptChatMessageList.get(i)).toString();
//            }
//            requestGpt(messages);
                try {
                    long curTime = System.currentTimeMillis();
                    if (curTime - mLastRequestTime < MIN_REQUEST_INTERVAL) {
                        Thread.sleep(MIN_REQUEST_INTERVAL - curTime + mLastRequestTime);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                mLastRequestTime = System.currentTimeMillis();

                Message minmaxMsg = new Message();
                ChatMessage chatGptMsg;
                JSONArray jsonArray = new JSONArray();
                switch (mAiPlatformIndex) {
                    case Constants.AI_PLATFORM_CHAT_GPT:
                        for (int i = 0; i < mGptChatMessageList.size(); i++) {
                            jsonArray.add(JSON.toJSON(mGptChatMessageList.get(i)));
                        }
                        requestGpt4(jsonArray);
                        break;
                    case Constants.AI_PLATFORM_MINIMAX_COMPLETION5:
                        break;
                    case Constants.AI_PLATFORM_MINIMAX_CHAT_COMPLETION4:
                        break;
                    case Constants.AI_PLATFORM_MINIMAX_CHAT_COMPLETION5:
                        for (int i = 0; i < mGptChatMessageList.size(); i++) {
                            chatGptMsg = mGptChatMessageList.get(i);
                            if (chatGptMsg.getRole().equals(Constants.GPT_ROLE_USER)) {
                                minmaxMsg.setSender_type(Constants.MINIMAX_SENDER_TYPE_USER);
                            } else if (chatGptMsg.getRole().equals(Constants.GPT_ROLE_ASSISTANT)) {
                                minmaxMsg.setSender_type(Constants.MINIMAX_SENDER_TYPE_BOT);
                            } else {
                                continue;
                            }
                            minmaxMsg.setText(chatGptMsg.getContent());
                            jsonArray.add(JSON.toJSON(minmaxMsg));
                        }
                        requestMinimaxChatCompletion5(jsonArray);
                        break;
                    case Constants.AI_PLATFORM_CHAT_XUNFEI:
                        for (int i = 0; i < mGptChatMessageList.size(); i++) {
                            if (mGptChatMessageList.get(i).getRole().equals(Constants.GPT_ROLE_ASSISTANT)) {
                                continue;
                            }
                            jsonArray.add(JSON.toJSON(mGptChatMessageList.get(i)));
                        }
                        XFGptWsManager.getInstance().requestChat("123", jsonArray);
                        break;
                    default:
                        for (int i = 0; i < mGptChatMessageList.size(); i++) {
                            jsonArray.add(JSON.toJSON(mGptChatMessageList.get(i)));
                        }
                        requestGpt4(jsonArray);
                        break;
                }
            }
        });
    }

    private void requestGpt(String[] question) {
        GptRequestBody body = new GptRequestBody();
        body.setQuestions(question);
        Log.i(Constants.TAG, "requestGpt: " + Arrays.toString(question));
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
                    ChatMessage gptAnswer = null;
                    try {
                        gptAnswer = JSON.parseObject(gptResponse.getAnswer(), ChatMessage.class);
                        notifyChatResult(0, gptAnswer.getContent());
                    } catch (Exception e) {
                        notifyChatResult(0, gptResponse.getAnswer());
                    }
                }

                @Override
                public void onError(Throwable e) {
                    Log.e(Constants.TAG, "requestGpt error=" + e);
                    notifyChatResult(-1, e.getMessage());
//                    if (Objects.equals(e.getMessage(), "timeout")) {
//                        updateHistoryList("GPT请求超时");
//                        requestChatGpt();
//                    }
                }

                @Override
                public void onComplete() {

                }
            });
    }

    private void notifyChatResult(int code, String answer) {
        if (mCallback != null) {
            mCallback.onAiAnswer(code, answer, System.currentTimeMillis() - mLastRequestTime);
        }
    }

    private void requestGpt4(JSONArray message) {
        Gpt4RequestBody body = new Gpt4RequestBody();
        body.setModel("gpt-4");
        body.setTemperature(1.0f);
        body.setMessages(message);
        Log.i(Constants.TAG, "requestGpt4: " + message.toJSONString());
        GptRetrofitManager.getInstance().getGptRequest().getGpt4Response(body)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<JSONObject>() {
                @Override
                public void onSubscribe(Disposable d) {

                }

                @Override
                public void onNext(JSONObject response) {
                    GptResponse gptResponse = JSONObject.toJavaObject(response, GptResponse.class);
                    ChatMessage gptAnswer = null;
                    try {
                        gptAnswer = JSON.parseObject(gptResponse.getAnswer(), ChatMessage.class);
                        notifyChatResult(0, gptAnswer.getContent());
                    } catch (Exception e) {
                        notifyChatResult(0, gptResponse.getAnswer());
                    }
                }

                @Override
                public void onError(Throwable e) {
                    Log.i(Constants.TAG, "requestGpt4 error=" + e);
                    notifyChatResult(-1, e.getMessage());
//                    if (Objects.equals(e.getMessage(), "timeout")) {
//                        updateHistoryList("GPT请求超时");
//                        requestChatGpt();
//                    }
                }

                @Override
                public void onComplete() {

                }
            });
    }

//    private void requestMinimaxCompletion5(String question) {
//        MinimaxCompletionRequestBody body = new MinimaxCompletionRequestBody();
//        body.setModel("abab5-completion");
//        body.setPrompt(question);
//        body.setTokens_to_generate(128);
//        body.setTemperature(0.9f);
//        body.setTop_p(0.95f);
//        body.setStream(false);
//
//        updateHistoryList("请求Minimax Completion5:" + question);
//        MiniMaxRetrofitManager.getInstance().getMinimaxRequest().getMinimaxCompletionResponse(BuildConfig.MINIMAX_GROUP_ID, body)
//            .subscribeOn(Schedulers.io())
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe(new Observer<JSONObject>() {
//                @Override
//                public void onSubscribe(Disposable d) {
//
//                }
//
//                @Override
//                public void onNext(JSONObject response) {
//                    String reply = response.getString("reply");
//                    updateHistoryList("Minimax回答:" + reply);
//                    requestTts(reply);
//                    sendAiAnswer(reply);
//                }
//
//                @Override
//                public void onError(Throwable e) {
//                    Log.i(TAG, "on error=" + e);
//                    if (Objects.equals(e.getMessage(), "timeout")) {
//                        updateHistoryList("Minimax请求超时");
//                    }
//                }
//
//                @Override
//                public void onComplete() {
//
//                }
//            });
//    }
//
//    private void requestMinimaxChatCompletion4(JSONArray messages) {
//        MinimaxChatCompletionRequestBody body = new MinimaxChatCompletionRequestBody();
//        //abab5-chat or abab4-chat
//        body.setModel("abab4-chat");
//        body.setPrompt("下面是小声和我的对话：");
//        RoleMeta roleMeta = new RoleMeta();
//        roleMeta.setBot_name("小声");
//        roleMeta.setUser_name("我");
//        body.setRole_meta(roleMeta);
////        Message chatMessage = new Message();
////        chatMessage.setSender_type("USER");
////        chatMessage.setText("请问小声," + question);
//        body.setMessages(messages);
//        body.setTokens_to_generate(128);
//        body.setTemperature(0.9f);
//        body.setTop_p(0.95f);
//        body.setStream(false);
//
//        updateHistoryList("请求Minimax Chat Completion4:" + messages.getJSONObject(messages.size() - 1));
//        MiniMaxRetrofitManager.getInstance().getMinimaxRequest().getMinimaxChatCompletionResponse(BuildConfig.MINIMAX_GROUP_ID, body)
//            .subscribeOn(Schedulers.io())
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe(new Observer<JSONObject>() {
//                @Override
//                public void onSubscribe(Disposable d) {
//
//                }
//
//                @Override
//                public void onNext(JSONObject response) {
//                    String reply = response.getString("reply");
//                    updateHistoryList("Minimax回答:" + reply);
//                    requestTts(reply);
//                    sendAiAnswer(reply);
//                }
//
//                @Override
//                public void onError(Throwable e) {
//                    Log.i(TAG, "on error=" + e);
//                    if (Objects.equals(e.getMessage(), "timeout")) {
//                        updateHistoryList("Minimax请求超时");
//                    }
//                }
//
//                @Override
//                public void onComplete() {
//
//                }
//            });
//    }

    private void requestMinimaxChatCompletion5(JSONArray msges) {
        MinimaxChatCompletionRequestBody body = new MinimaxChatCompletionRequestBody();
        //abab5-chat or abab4-chat
        body.setModel("abab5-chat");
        body.setMessages(msges);
        body.setTokens_to_generate(128);
        body.setTemperature(0.9f);
        body.setTop_p(0.95f);
        body.setStream(false);

        Log.i(Constants.TAG, "requestMinimaxChatCompletion5: " + msges.getJSONObject(msges.size() - 1));
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
                    notifyChatResult(0, reply);
                }

                @Override
                public void onError(Throwable e) {
                    Log.i(Constants.TAG, "requestMinimaxChatCompletion5 error=" + e);
                    notifyChatResult(-1, e.getMessage());
                }

                @Override
                public void onComplete() {

                }
            });
    }
}
