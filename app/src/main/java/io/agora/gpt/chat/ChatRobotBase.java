package io.agora.gpt.chat;

import com.alibaba.fastjson.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.agora.gpt.inf.ChatCallback;
import io.agora.gpt.inf.IChatRobot;
import io.agora.gpt.models.chat.ChatBotRole;
import io.agora.gpt.net.HttpURLRequest;

public class ChatRobotBase implements IChatRobot {
    protected final ExecutorService mExecutorService;
    protected final ExecutorService mExecutorCacheService;
    protected ChatCallback mCallback;
    protected int mModelIndex;
    protected ChatBotRole mChatBotRole;

    protected volatile List<HttpURLRequest> mHttpUrlRequests;
    protected List<Long> mRequestThreadIds;

    public ChatRobotBase() {
        mExecutorService = new ThreadPoolExecutor(1, 1,
                0, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());

        mExecutorCacheService = new ThreadPoolExecutor(Integer.MAX_VALUE, Integer.MAX_VALUE,
                0, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        mHttpUrlRequests = new ArrayList<>();
        mRequestThreadIds = new ArrayList<>();
    }

    @Override
    public void init() {

    }

    @Override
    public void setChatCallback(ChatCallback callback) {
        mCallback = callback;
    }

    @Override
    public void setModelIndex(int index) {
        mModelIndex = index;
    }

    @Override
    public void setChatBotRole(ChatBotRole chatBotRole) {
        mChatBotRole = chatBotRole;
    }


    @Override
    public void requestChat(JSONArray messageJsonArray) {

    }

    @Override
    public void requestChat(String message) {

    }

    @Override
    public void requestChatKeyInfo(JSONArray messageJsonArray) {

    }

    @Override
    public void cancelRequestChat(boolean cancel) {
        if (cancel) {
            mRequestThreadIds.clear();
            if (null != mHttpUrlRequests && mHttpUrlRequests.size() > 0) {
                for (HttpURLRequest httpUrlRequest : mHttpUrlRequests) {
                    httpUrlRequest.setCancelled(true);
                }
                mHttpUrlRequests.clear();
            }
        }
    }

    @Override
    public void close() {

    }
}
