package io.agora.gpt.stt;

import com.google.gson.Gson;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.agora.gpt.inf.ISttRobot;
import io.agora.gpt.inf.SttCallback;

public class SttRobotBase implements ISttRobot {
    protected SttCallback mCallback;
    protected final Gson mGson;
    protected final ExecutorService mExecutorService;
    protected final ExecutorService mExecutorCacheService;

    protected boolean mIsFinished;

    protected volatile long mLastSendTime;

    public SttRobotBase() {
        mExecutorService = new ThreadPoolExecutor(1, 1,
                0, TimeUnit.MILLISECONDS,
                new LinkedBlockingDeque<Runnable>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        mExecutorCacheService = new ThreadPoolExecutor(Integer.MAX_VALUE, Integer.MAX_VALUE,
                0, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        mGson = new Gson();
        mIsFinished = false;
        mLastSendTime = 0;
    }

    @Override
    public void init() {

    }

    @Override
    public void setSttCallback(SttCallback callback) {
        mCallback = callback;
    }

    @Override
    public synchronized void stt(byte[] bytes) {

    }

    @Override
    public void close() {

    }
}
