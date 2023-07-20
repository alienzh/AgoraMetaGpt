package io.agora.metagpt.ui.main;

import android.util.Log;

import androidx.lifecycle.ViewModel;

import io.agora.metachat.IMetachatEventHandler;
import io.agora.metachat.IMetachatScene;
import io.agora.metachat.MetachatSceneInfo;
import io.agora.metagpt.MainApplication;
import io.agora.metagpt.context.MetaContext;

public class CreateRoomViewModel extends ViewModel implements IMetachatEventHandler {
    private static final String TAG = CreateRoomViewModel.class.getSimpleName();

    @Override
    protected void onCleared() {
        MetaContext.getInstance().unregisterMetaChatEventHandler(this);
        super.onCleared();
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
    }

    @Override
    public void onDownloadSceneProgress(long mSceneId, int progress, int state) {

    }
}
