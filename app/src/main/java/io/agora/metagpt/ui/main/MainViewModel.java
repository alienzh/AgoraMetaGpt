package io.agora.metagpt.ui.main;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.Arrays;
import java.util.List;

import io.agora.metagpt.context.GameContext;
import io.agora.metagpt.utils.KeyCenter;
import io.agora.metachat.AvatarModelInfo;
import io.agora.metachat.IMetachatEventHandler;
import io.agora.metachat.IMetachatScene;
import io.agora.metachat.MetachatBundleInfo;
import io.agora.metachat.MetachatSceneInfo;
import io.agora.metachat.MetachatUserInfo;
import io.agora.metagpt.MainApplication;
import io.agora.metagpt.context.MetaContext;
import io.agora.metagpt.utils.SingleLiveData;

public class MainViewModel extends ViewModel implements IMetachatEventHandler {
    private static final String TAG = MainViewModel.class.getSimpleName();

    private final SingleLiveData<List<MetachatSceneInfo>> sceneList = new SingleLiveData<>();
    private final SingleLiveData<Long> selectScene = new SingleLiveData<>();
    private final SingleLiveData<Boolean> requestDownloading = new SingleLiveData<>();
    private final SingleLiveData<Integer> downloadingProgress = new SingleLiveData<>();

    @Override
    protected void onCleared() {
        MetaContext.getInstance().unregisterMetaChatEventHandler(this);
        super.onCleared();
    }

    public LiveData<List<MetachatSceneInfo>> getSceneList() {
        return sceneList;
    }

    public LiveData<Long> getSelectScene() {
        return selectScene;
    }

    public LiveData<Boolean> getRequestDownloading() {
        return requestDownloading;
    }

    public LiveData<Integer> getDownloadingProgress() {
        return downloadingProgress;
    }

    public void getScenes() {
        MetaContext metaChatContext = MetaContext.getInstance();
        metaChatContext.registerMetaChatEventHandler(this);
        if (metaChatContext.initialize(MainApplication.mGlobalApplication)) {
            if (!metaChatContext.getSceneInfos()) {
                Log.e(TAG, "get scene info fail");
            }
        }
    }

    public void prepareScene(MetachatSceneInfo sceneInfo) {
        MetaContext metaChatContext = MetaContext.getInstance();
        metaChatContext.prepareScene(sceneInfo, new AvatarModelInfo() {{
            // TODO choose one
            MetachatBundleInfo[] bundles = sceneInfo.mBundles;
            for (MetachatBundleInfo bundleInfo : bundles) {
                if (bundleInfo.mBundleType == MetachatBundleInfo.BundleType.BUNDLE_TYPE_AVATAR) {
                    mBundleCode = bundleInfo.mBundleCode;
                    break;
                }
            }
            mLocalVisible = true;
            mRemoteVisible = false;
            mSyncPosition = false;

        }}, new MetachatUserInfo() {{
            mUserId = String.valueOf(KeyCenter.getUserUid());
            mUserName = GameContext.getInstance().getUserName();
            mUserIconUrl = "https://accpic.sd-rtn.com/pic/test/png/2.png";
        }});
        if (metaChatContext.isSceneDownloaded(sceneInfo)) {
            selectScene.postValue(sceneInfo.mSceneId);
        } else {
            requestDownloading.postValue(true);
        }
    }

    public void downloadScene(MetachatSceneInfo sceneInfo) {
        MetaContext.getInstance().downloadScene(sceneInfo);
    }

    public void cancelDownloadScene(MetachatSceneInfo sceneInfo) {
        MetaContext.getInstance().cancelDownloadScene(sceneInfo);
    }

    @Override
    public void onCreateSceneResult(IMetachatScene scene, int errorCode) {
    }

    @Override
    public void onConnectionStateChanged(int state, int reason) {

    }

    @Override
    public void onRequestToken() {

    }

    @Override
    public void onGetSceneInfosResult(MetachatSceneInfo[] scenes, int errorCode) {
        sceneList.postValue(Arrays.asList(scenes));
    }

    @Override
    public void onDownloadSceneProgress(long mSceneId, int progress, int state) {
        Log.d("progress", String.valueOf(progress));
        if (state == SceneDownloadState.METACHAT_SCENE_DOWNLOAD_STATE_FAILED) {
            downloadingProgress.postValue(-1);
            return;
        }
        downloadingProgress.postValue(progress);
        if (state == SceneDownloadState.METACHAT_SCENE_DOWNLOAD_STATE_DOWNLOADED) {
            selectScene.postValue(mSceneId);
        }
    }

}
