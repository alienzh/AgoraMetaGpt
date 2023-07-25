package io.agora.metagpt.ui.base;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.nio.ByteBuffer;

import io.agora.base.VideoFrame;
import io.agora.meta.IMetaScene;
import io.agora.meta.IMetaSceneEventHandler;
import io.agora.meta.IMetaServiceEventHandler;
import io.agora.meta.MetaSceneAssetsInfo;
import io.agora.meta.MetaUserPositionInfo;
import io.agora.metagpt.inf.IRtcEventCallback;
import io.agora.metagpt.context.MetaContext;
import io.agora.rtc2.IAudioFrameObserver;
import io.agora.rtc2.audio.AudioParams;
import io.reactivex.disposables.CompositeDisposable;

public class BaseActivity extends AppCompatActivity implements IMetaSceneEventHandler, IMetaServiceEventHandler, IRtcEventCallback, IAudioFrameObserver {
    protected CompositeDisposable compositeDisposable;
    protected boolean mIsFront;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTransparent(getWindow());
        adaptAndroidP(getWindow());

        initContentView();

        initData();

        initView();

        initListener();

        initClickEvent();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initData();

        initView();
    }

    protected void initClickEvent() {
    }

    protected void initContentView() {
    }

    protected void initView() {

    }

    protected void initData() {
        compositeDisposable = new CompositeDisposable();
        mIsFront = false;
    }

    protected void initListener() {

    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsFront = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsFront = false;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != compositeDisposable) {
            compositeDisposable.dispose();
        }
    }

    protected void unregister() {
        MetaContext.getInstance().unregisterMetaServiceEventHandler(this);
        MetaContext.getInstance().unregisterMetaSceneEventHandler(this);
    }

    protected void registerMeta() {
        MetaContext.getInstance().registerMetaSceneEventHandler(this);
        MetaContext.getInstance().registerMetaServiceEventHandler(this);

        MetaContext.getInstance().setRtcEventCallback(this);
    }

    protected void registerRtc() {
        MetaContext.getInstance().setRtcEventCallback(this);
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }


    //重写Activity该方法，当窗口焦点变化时自动隐藏system bar，这样可以排除在弹出dialog和menu时，
    //system bar会重新显示的问题(弹出dialog时似乎还是可以重新显示的0.0)。
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY //(修改这个选项，可以设置不同模式)
                        //使用下面三个参数，可以使内容显示在system bar的下面，防止system bar显示或
                        //隐藏时，Activity的大小被resize。
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // 隐藏导航栏和状态栏
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    /**
     * 1.使状态栏透明
     */
    private static void setTransparent(@NonNull Window window) {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        window.setStatusBarColor(Color.TRANSPARENT);
    }

    private static void adaptAndroidP(@NonNull Window window) {
        // 适配刘海屏,全屏去黑状态栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            window.setAttributes(layoutParams);
        }
    }


    @Override
    public void onCreateSceneResult(IMetaScene scene, int errorCode) {

    }

    @Override
    public void onConnectionStateChanged(int state, int reason) {

    }

    @Override
    public void onTokenWillExpire() {

    }

    @Override
    public void onGetSceneAssetsInfoResult(MetaSceneAssetsInfo[] metaSceneAssetsInfos, int errorCode) {

    }

    @Override
    public void onDownloadSceneAssetsProgress(long sceneId, int progress, int state) {

    }

    @Override
    public void onEnterSceneResult(int errorCode) {

    }

    @Override
    public void onLeaveSceneResult(int errorCode) {

    }

    @Override
    public void onSceneMessageReceived(byte[] message) {

    }

    @Override
    public void onUserPositionChanged(String uid, MetaUserPositionInfo posInfo) {

    }

    @Override
    public void onReleasedScene(int status) {
    }

    @Override
    public void onSceneVideoFrameCaptured(TextureView view, VideoFrame videoFrame) {

    }

    @Override
    public void onAddSceneViewResult(TextureView view, int errorCode) {

    }

    @Override
    public void onRemoveSceneViewResult(TextureView view, int errorCode) {

    }

    @Override
    public boolean onRecordAudioFrame(String channelId, int type, int samplesPerChannel, int bytesPerSample, int channels, int samplesPerSec, ByteBuffer buffer, long renderTimeMs, int avsync_type) {
        return false;
    }

    @Override
    public boolean onPlaybackAudioFrame(String channelId, int type, int samplesPerChannel, int bytesPerSample, int channels, int samplesPerSec, ByteBuffer buffer, long renderTimeMs, int avsync_type) {
        return false;
    }

    @Override
    public boolean onMixedAudioFrame(String channelId, int type, int samplesPerChannel, int bytesPerSample, int channels, int samplesPerSec, ByteBuffer buffer, long renderTimeMs, int avsync_type) {
        return false;
    }

    @Override
    public boolean onEarMonitoringAudioFrame(int type, int samplesPerChannel, int bytesPerSample, int channels, int samplesPerSec, ByteBuffer buffer, long renderTimeMs, int avsync_type) {
        return false;
    }

    @Override
    public boolean onPlaybackAudioFrameBeforeMixing(String channelId, int userId, int type, int samplesPerChannel, int bytesPerSample, int channels, int samplesPerSec, ByteBuffer buffer, long renderTimeMs, int avsync_type) {
        return false;
    }

    @Override
    public int getObservedAudioFramePosition() {
        return 0;
    }

    @Override
    public AudioParams getRecordAudioParams() {
        return null;
    }

    @Override
    public AudioParams getPlaybackAudioParams() {
        return null;
    }

    @Override
    public AudioParams getMixedAudioParams() {
        return null;
    }

    @Override
    public AudioParams getEarMonitoringAudioParams() {
        return null;
    }
}
