package io.agora.gpt.utils;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import io.agora.gpt.MainApplication;

public class ToastUtils {

    private static Handler mainHandler;

    public static void showToast(int resStringId) {
        runOnMainThread(() -> Toast.makeText(MainApplication.mGlobalApplication, resStringId, Toast.LENGTH_SHORT).show());
    }

    public static void showToast(String str) {
        runOnMainThread(() -> Toast.makeText(MainApplication.mGlobalApplication, str, Toast.LENGTH_SHORT).show());
    }

    public static void showToastLong(int resStringId) {
        runOnMainThread(() -> Toast.makeText(MainApplication.mGlobalApplication, resStringId, Toast.LENGTH_LONG).show());
    }

    public static void showToastLong(String str) {
        runOnMainThread(() -> Toast.makeText(MainApplication.mGlobalApplication, str, Toast.LENGTH_LONG).show());
    }

    private static void runOnMainThread(Runnable runnable){
        if(mainHandler == null){
            mainHandler = new Handler(Looper.getMainLooper());
        }
        if(Thread.currentThread() == mainHandler.getLooper().getThread()){
            runnable.run();
        }else{
            mainHandler.post(runnable);
        }
    }
}
