package io.agora.metagpt.stt;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.agora.metagpt.MainApplication;
import io.agora.metagpt.inf.ISttRobot;
import io.agora.metagpt.inf.SttCallback;

public class SttRobotBase implements ISttRobot {

    private Handler mHandler = new Handler(Looper.getMainLooper());
    protected SttCallback mCallback;
    protected final Gson mGson;
    protected final ExecutorService mExecutorService;

    protected boolean mIsFinished;

    protected volatile long mLastSendTime;

    public SttRobotBase() {
        mExecutorService = new ThreadPoolExecutor(1, 1,
                0, TimeUnit.MILLISECONDS,
                new LinkedBlockingDeque<Runnable>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
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

    protected final void runOnUiThread(Runnable action) {
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
            mHandler.post(action);
        } else {
            action.run();
        }
    }

    protected void toast(String message){
        runOnUiThread(() -> Toast.makeText(MainApplication.mGlobalApplication.getApplicationContext(),message,Toast.LENGTH_SHORT).show());
    }
}
