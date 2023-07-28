package io.agora.gpt.ui.main;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.Arrays;
import java.util.List;

import io.agora.meta.AvatarModelInfo;
import io.agora.meta.IMetaScene;
import io.agora.meta.IMetaServiceEventHandler;
import io.agora.meta.MetaBundleInfo;
import io.agora.meta.MetaSceneAssetsInfo;
import io.agora.meta.MetaUserInfo;
import io.agora.gpt.MainApplication;
import io.agora.gpt.context.GameContext;
import io.agora.gpt.context.MetaContext;
import io.agora.gpt.utils.KeyCenter;
import io.agora.gpt.utils.SingleLiveData;

public class MainViewModel extends ViewModel implements IMetaServiceEventHandler {
    private static final String TAG = MainViewModel.class.getSimpleName();

    private final SingleLiveData<List<MetaSceneAssetsInfo>> sceneList = new SingleLiveData<>();
    private final SingleLiveData<Long> selectScene = new SingleLiveData<>();
    private final SingleLiveData<Boolean> requestDownloading = new SingleLiveData<>();
    private final SingleLiveData<Integer> downloadingProgress = new SingleLiveData<>();

    @Override
    protected void onCleared() {
        MetaContext.getInstance().unregisterMetaServiceEventHandler(this);
        super.onCleared();
    }

    public LiveData<List<MetaSceneAssetsInfo>> getSceneList() {
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
        MetaContext metaContext = MetaContext.getInstance();
        metaContext.registerMetaServiceEventHandler(this);
        if (metaContext.initialize(MainApplication.mGlobalApplication)) {
            if (!metaContext.getSceneInfos()) {
                Log.e(TAG, "get scene info fail");
            }
        }
    }

    public void prepareScene(MetaSceneAssetsInfo sceneInfo) {
        MetaContext metaContext = MetaContext.getInstance();
        metaContext.prepareScene(sceneInfo, new AvatarModelInfo() {{
            // TODO choose one
            MetaBundleInfo[] bundles = sceneInfo.mBundles;
            for (MetaBundleInfo bundleInfo : bundles) {
                if (bundleInfo.mBundleType == MetaBundleInfo.BundleType.BUNDLE_TYPE_AVATAR) {
                    mBundleCode = bundleInfo.mBundleCode;
                    break;
                }
            }
            mLocalVisible = true;
            mRemoteVisible = false;
            mSyncPosition = false;

        }}, new MetaUserInfo() {{
            mUserId = String.valueOf(KeyCenter.getUserUid());
            mUserName = GameContext.getInstance().getUserName();
            mUserIconUrl = "https://accpic.sd-rtn.com/pic/test/png/2.png";
        }});
        if (metaContext.isSceneDownloaded(sceneInfo)) {
            selectScene.postValue(sceneInfo.mSceneId);
        } else {
            requestDownloading.postValue(true);
        }
    }

    public void downloadScene(MetaSceneAssetsInfo sceneInfo) {
        MetaContext.getInstance().downloadScene(sceneInfo);
    }

    public void cancelDownloadScene(MetaSceneAssetsInfo sceneInfo) {
        MetaContext.getInstance().cancelDownloadScene(sceneInfo);
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
        sceneList.postValue(Arrays.asList(metaSceneAssetsInfos));
    }

    @Override
    public void onDownloadSceneAssetsProgress(long sceneId, int progress, int state) {
        Log.d("progress", String.valueOf(progress));
        if (state == SceneDownloadState.META_SCENE_DOWNLOAD_STATE_FAILED) {
            downloadingProgress.postValue(-1);
            return;
        }
        downloadingProgress.postValue(progress);
        if (state == SceneDownloadState.META_SCENE_DOWNLOAD_STATE_DOWNLOADED) {
            selectScene.postValue(sceneId);
        }
    }

}
