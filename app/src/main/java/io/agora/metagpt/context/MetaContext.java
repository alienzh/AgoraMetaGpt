package io.agora.metagpt.context;


import android.content.Context;
import android.util.Log;
import android.view.TextureView;


import com.alibaba.fastjson.JSONObject;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import io.agora.base.VideoFrame;
import io.agora.metagpt.models.DataStreamModel;
import io.agora.metagpt.utils.KeyCenter;
import io.agora.metachat.AvatarModelInfo;
import io.agora.metachat.EnterSceneConfig;
import io.agora.metachat.ILocalUserAvatar;
import io.agora.metachat.IMetachatEventHandler;
import io.agora.metachat.IMetachatScene;
import io.agora.metachat.IMetachatSceneEventHandler;
import io.agora.metachat.IMetachatService;
import io.agora.metachat.MetachatConfig;
import io.agora.metachat.MetachatSceneConfig;
import io.agora.metachat.MetachatSceneInfo;
import io.agora.metachat.MetachatUserInfo;
import io.agora.metachat.MetachatUserPositionInfo;
import io.agora.metagpt.inf.IRtcEventCallback;
import io.agora.metagpt.models.EnterSceneExtraInfo;
import io.agora.metagpt.utils.Constants;
import io.agora.metagpt.utils.Utils;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.DataStreamConfig;
import io.agora.rtc2.IAudioFrameObserver;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;

public class MetaContext implements IMetachatEventHandler, IMetachatSceneEventHandler {

    private final static String TAG = Constants.TAG + "-" + MetaContext.class.getSimpleName();
    private volatile static MetaContext instance = null;

    private RtcEngine rtcEngine;
    private IMetachatService metaChatService;
    private IMetachatScene metaChatScene;
    private MetachatSceneInfo sceneInfo;
    private AvatarModelInfo modelInfo;
    private MetachatUserInfo userInfo;
    private String roomName;
    private int uid;
    private String userName;
    private TextureView sceneView;
    private final ConcurrentHashMap<IMetachatEventHandler, Integer> metaChatEventHandlerMap;
    private final ConcurrentHashMap<IMetachatSceneEventHandler, Integer> metaChatSceneEventHandlerMap;
    private ILocalUserAvatar localUserAvatar;

    private IRtcEventCallback iRtcEventCallback;

    private boolean isInitMetachat;


    private MetaContext() {
        metaChatEventHandlerMap = new ConcurrentHashMap<>();
        metaChatSceneEventHandlerMap = new ConcurrentHashMap<>();
        iRtcEventCallback = null;
        isInitMetachat = false;
    }

    public static MetaContext getInstance() {
        if (instance == null) {
            synchronized (MetaContext.class) {
                if (instance == null) {
                    instance = new MetaContext();
                }
            }
        }
        return instance;
    }

    public void initRoomNameAndUid(String roomName, int uid, String userName) {
        this.roomName = roomName;
        this.uid = uid;
        this.userName = userName;
    }

    public boolean initialize(Context context) {
        int ret = io.agora.rtc2.Constants.ERR_OK;

        if (initializeRtc(context)) {
            try {
                metaChatService = IMetachatService.create();
                MetachatConfig config = new MetachatConfig() {{
                    mRtcEngine = rtcEngine;
                    mAppId = KeyCenter.APP_ID;
                    mRtmToken = KeyCenter.getRtmToken(uid);
                    mLocalDownloadPath = context.getExternalCacheDir().getPath();
                    mUserId = String.valueOf(KeyCenter.getUserUid());
                    mEventHandler = MetaContext.this;
                }};
                ret += metaChatService.initialize(config);
                Log.i(TAG, "launcher version=" + metaChatService.getLauncherVersion(context));

                isInitMetachat = true;
            } catch (Exception e) {
                return false;
            }
        }
        return ret == io.agora.rtc2.Constants.ERR_OK;
    }

    public boolean initializeRtc(Context context) {
        if (rtcEngine == null) {
            try {
                RtcEngineConfig rtcEngineConfig = new RtcEngineConfig();
                rtcEngineConfig.mContext = context;
                rtcEngineConfig.mAppId = KeyCenter.APP_ID;
                rtcEngineConfig.mChannelProfile = io.agora.rtc2.Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
                rtcEngineConfig.mEventHandler = new IRtcEngineEventHandler() {
                    @Override
                    public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                        Log.d(TAG, String.format("onJoinChannelSuccess %s %d", channel, uid));
                        if (null != iRtcEventCallback) {
                            iRtcEventCallback.onJoinChannelSuccess(channel, uid, elapsed);
                        }
                    }

                    @Override
                    public void onLeaveChannel(RtcStats stats) {
                        Log.d(TAG, "onLeaveChannel stats=" + stats.toString());
                        if (null != iRtcEventCallback) {
                            iRtcEventCallback.onLeaveChannel(stats);
                        }
                    }

                    @Override
                    public void onUserOffline(int uid, int reason) {
                        Log.d(TAG, String.format("onUserOffline %d %d ", uid, reason));
                        if (null != iRtcEventCallback) {
                            iRtcEventCallback.onUserOffline(uid, reason);
                        }
                    }

                    @Override
                    public void onAudioRouteChanged(int routing) {
                        Log.d(TAG, String.format("onAudioRouteChanged %d", routing));
                    }

                    @Override
                    public void onUserJoined(int uid, int elapsed) {
                        Log.d(TAG, "onUserJoined uid=" + uid);
                        if (null != iRtcEventCallback) {
                            iRtcEventCallback.onUserJoined(uid, elapsed);
                        }
                    }

                    @Override
                    public void onStreamMessage(int uid, int streamId, byte[] data) {
                        Log.d(TAG, "onStreamMessage uid=" + uid + ", streamId=" + streamId);
                        if (null != iRtcEventCallback) {
                            iRtcEventCallback.onStreamMessage(uid, streamId, data);
                        }
                    }

                    @Override
                    public void onAudioVolumeIndication(AudioVolumeInfo[] speakers, int totalVolume) {
                        if (null != iRtcEventCallback) {
                            iRtcEventCallback.onAudioVolumeIndication(speakers, totalVolume);
                        }
                    }
                };
                rtcEngineConfig.mAudioScenario = io.agora.rtc2.Constants.AudioScenario.getValue(io.agora.rtc2.Constants.AudioScenario.DEFAULT);
                rtcEngine = RtcEngine.create(rtcEngineConfig);

                rtcEngine.setParameters("{\"rtc.enable_debug_log\":true}");
                rtcEngine.enableAudio();
                rtcEngine.enableVideo();
                rtcEngine.setChannelProfile(io.agora.rtc2.Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public void destroy() {
        if (isInitMetachat) {
            IMetachatService.destroy();
            metaChatService = null;
            isInitMetachat = false;
        }

        RtcEngine.destroy();
        rtcEngine = null;

    }

    public void registerMetaChatEventHandler(IMetachatEventHandler eventHandler) {
        metaChatEventHandlerMap.put(eventHandler, 0);
    }

    public void unregisterMetaChatEventHandler(IMetachatEventHandler eventHandler) {
        metaChatEventHandlerMap.remove(eventHandler);
    }

    public void registerMetaChatSceneEventHandler(IMetachatSceneEventHandler eventHandler) {
        metaChatSceneEventHandlerMap.put(eventHandler, 0);
    }

    public void unregisterMetaChatSceneEventHandler(IMetachatSceneEventHandler eventHandler) {
        metaChatSceneEventHandlerMap.remove(eventHandler);
    }

    public boolean getSceneInfos() {
        return metaChatService.getSceneInfos() == io.agora.rtc2.Constants.ERR_OK;
    }

    public boolean isSceneDownloaded(MetachatSceneInfo sceneInfo) {
        return metaChatService.isSceneDownloaded(sceneInfo.mSceneId) > 0;
    }

    public boolean downloadScene(MetachatSceneInfo sceneInfo) {
        return metaChatService.downloadScene(sceneInfo.mSceneId) == io.agora.rtc2.Constants.ERR_OK;
    }

    public boolean cancelDownloadScene(MetachatSceneInfo sceneInfo) {
        return metaChatService.cancelDownloadScene(sceneInfo.mSceneId) == io.agora.rtc2.Constants.ERR_OK;
    }

    public void prepareScene(MetachatSceneInfo sceneInfo, AvatarModelInfo modelInfo, MetachatUserInfo userInfo) {
        this.sceneInfo = sceneInfo;
        this.modelInfo = modelInfo;
        this.userInfo = userInfo;
    }

    public boolean createScene(Context activityContext, TextureView tv) {
        Log.d(TAG, "createAndEnterScene");
        this.sceneView = tv;

        MetachatSceneConfig sceneConfig = new MetachatSceneConfig();
        sceneConfig.mActivityContext = activityContext;

        int ret = -1;
        if (metaChatScene == null) {
            ret = metaChatService.createScene(sceneConfig);
        }

        return ret == io.agora.rtc2.Constants.ERR_OK;
    }

    public void enterScene() {
        Log.d(TAG, "enterScene");
        if (null != localUserAvatar) {
            localUserAvatar.setUserInfo(userInfo);

            //该model的mBundleType为MetachatBundleInfo.BundleType.BUNDLE_TYPE_AVATAR类型
            localUserAvatar.setModelInfo(modelInfo);

        }
        if (null != metaChatScene) {
            //设置回调接口
            metaChatScene.addEventHandler(MetaContext.getInstance());
            EnterSceneConfig config = new EnterSceneConfig();
            //sceneView必须为Texture类型，为渲染unity显示的view
            config.mSceneView = this.sceneView;
            //rtc加入channel的ID
            config.mRoomName = this.roomName;
            if (null != sceneInfo) {
                //内容中心对应的ID
                config.mSceneId = this.sceneInfo.mSceneId;
            }

            /*
             *仅为示例格式，具体格式以项目实际为准
             *   "extraCustomInfo":{
             *     "sceneIndex":0  //0为默认场景，在这里指咖啡厅，1为换装设置场景
             *   }
             */
            EnterSceneExtraInfo extraInfo = new EnterSceneExtraInfo();

            extraInfo.setSceneIndex(Constants.SCENE_INDEX_WORD);

            //加载的场景index
            config.mExtraCustomInfo = JSONObject.toJSONString(extraInfo).getBytes();
            metaChatScene.enterScene(config);
        }

    }

    @Override
    public void onCreateSceneResult(IMetachatScene scene, int errorCode) {
        Log.d(TAG, "onCreateSceneResult errorCode:" + errorCode);
        metaChatScene = scene;
        localUserAvatar = metaChatScene.getLocalUserAvatar();
        for (IMetachatEventHandler handler : metaChatEventHandlerMap.keySet()) {
            handler.onCreateSceneResult(scene, errorCode);
        }
    }


    public boolean updateRoleSpeak(boolean isSpeak) {
        int ret = io.agora.rtc2.Constants.ERR_OK;
        ret += rtcEngine.updateChannelMediaOptions(new ChannelMediaOptions() {{
            publishMicrophoneTrack = isSpeak;
        }});
        return ret == io.agora.rtc2.Constants.ERR_OK;
    }

    public void updatePublishMediaOptions(boolean enable, int playerId) {
        if (null != rtcEngine) {
            rtcEngine.updateChannelMediaOptions(new ChannelMediaOptions() {{
                publishMediaPlayerAudioTrack = enable;
                publishMediaPlayerId = playerId;
            }});
        }
    }

    public boolean enableLocalAudio(boolean enabled) {
        return rtcEngine.enableLocalAudio(enabled) == io.agora.rtc2.Constants.ERR_OK;
    }

    public boolean leaveScene() {
        Log.d(TAG, "leaveScene");
        int ret = io.agora.rtc2.Constants.ERR_OK;
        if (metaChatScene != null) {
            ret += leaveRtcChannel();
            ret += metaChatScene.leaveScene();
        }
        Log.d(TAG, "leaveScene success");
        return ret == io.agora.rtc2.Constants.ERR_OK;
    }

    public int leaveRtcChannel() {
        if (null == rtcEngine) {
            return io.agora.rtc2.Constants.ERR_FAILED;
        }
        return rtcEngine.leaveChannel();
    }

    @Override
    public void onConnectionStateChanged(int state, int reason) {
        Log.d(TAG, "onConnectionStateChanged state=" + state + ",reason=" + reason);
        if (state == ConnectionState.METACHAT_CONNECTION_STATE_ABORTED) {
            leaveScene();
        }

        for (IMetachatEventHandler handler : metaChatEventHandlerMap.keySet()) {
            handler.onConnectionStateChanged(state, reason);
        }
    }

    @Override
    public void onRequestToken() {
        for (IMetachatEventHandler handler : metaChatEventHandlerMap.keySet()) {
            handler.onRequestToken();
        }
    }

    @Override
    public void onGetSceneInfosResult(MetachatSceneInfo[] scenes, int errorCode) {
        for (IMetachatEventHandler handler : metaChatEventHandlerMap.keySet()) {
            handler.onGetSceneInfosResult(scenes, errorCode);
        }
    }

    @Override
    public void onDownloadSceneProgress(long SceneId, int progress, int state) {
        for (IMetachatEventHandler handler : metaChatEventHandlerMap.keySet()) {
            handler.onDownloadSceneProgress(SceneId, progress, state);
        }
    }

    @Override
    public void onEnterSceneResult(int errorCode) {
        Log.d(TAG, String.format("onEnterSceneResult %d", errorCode));
        if (errorCode == 0) {
            if (null != metaChatScene) {
                metaChatScene.setSceneParameters("{\"debugUnity\":true}");
            }
            joinChannel(io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER);

        }
        for (IMetachatSceneEventHandler handler : metaChatSceneEventHandlerMap.keySet()) {
            handler.onEnterSceneResult(errorCode);
        }
    }

    public void joinChannel(int roleType) {
        int ret = rtcEngine.joinChannel(
                KeyCenter.getRtcToken(roomName, uid), roomName, uid,
                new ChannelMediaOptions() {{
                    publishMicrophoneTrack = false;
                    autoSubscribeAudio = true;
                    clientRoleType = roleType;
                }});
        Log.d(TAG, String.format("joinChannel ret %d", ret));
    }


    @Override
    public void onLeaveSceneResult(int errorCode) {
        Log.d(TAG, String.format("onLeaveSceneResult %d", errorCode));
        if (errorCode == 0) {
            metaChatScene.release();
            metaChatScene = null;
        }

        for (IMetachatSceneEventHandler handler : metaChatSceneEventHandlerMap.keySet()) {
            handler.onLeaveSceneResult(errorCode);
        }
    }

    @Override
    public void onReleasedScene(int status) {
        Log.d(TAG, String.format("onReleasedScene %d", status));
        for (IMetachatSceneEventHandler handler : metaChatSceneEventHandlerMap.keySet()) {
            handler.onReleasedScene(status);
        }
    }

    @Override
    public void onSceneVideoFrame(TextureView view, VideoFrame videoFrame) {

    }

    @Override
    public void onRecvMessageFromScene(byte[] message) {
        Log.d(TAG, String.format("onRecvMessageFromScene %s", new String(message)));
        for (IMetachatSceneEventHandler handler : metaChatSceneEventHandlerMap.keySet()) {
            handler.onRecvMessageFromScene(message);
        }
    }

    @Override
    public void onUserPositionChanged(String uid, MetachatUserPositionInfo posInfo) {
        Log.d(TAG, String.format("onUserPositionChanged %s %s %s %s %s", uid,
                Arrays.toString(posInfo.mPosition),
                Arrays.toString(posInfo.mForward),
                Arrays.toString(posInfo.mRight),
                Arrays.toString(posInfo.mUp)
        ));
        for (IMetachatSceneEventHandler handler : metaChatSceneEventHandlerMap.keySet()) {
            handler.onUserPositionChanged(uid, posInfo);
        }
    }

    @Override
    public void onEnumerateVideoDisplaysResult(String[] displayIds) {

    }

    public MetachatSceneInfo getSceneInfo() {
        return sceneInfo;
    }

    public void sendSceneMessage(String msg) {
        if (metaChatScene == null) {
            Log.e(TAG, "sendMessageToScene metaChatScene is null");
            return;
        }

        metaChatScene.sendMessageToScene(msg.getBytes());
    }


    public boolean isInitMetachat() {
        return isInitMetachat;
    }

    public void setRtcEventCallback(IRtcEventCallback iRtcEventCallback) {
        this.iRtcEventCallback = iRtcEventCallback;
    }

    public RtcEngine getRtcEngine() {
        return rtcEngine;
    }

    public int getSceneId() {
        return Constants.SCENE_ID_WORD;
    }

    public void updatePublishCustomAudioTrackChannelOptions(boolean enable, int sampleRate, int channels, int sourceNumber, boolean localPlayback, boolean publish) {
        if (null != rtcEngine) {
            ChannelMediaOptions option = new ChannelMediaOptions();
            option.publishCustomAudioTrack = enable;
            rtcEngine.updateChannelMediaOptions(option);
            rtcEngine.enableCustomAudioLocalPlayback(0, enable);

            rtcEngine.setExternalAudioSource(true, sampleRate, channels, sourceNumber, localPlayback, publish);

        }
    }

    public void updateAutoSubscribeAudio(boolean enable) {
        if (null != rtcEngine) {
            ChannelMediaOptions option = new ChannelMediaOptions();
            option.autoSubscribeAudio = enable;
            rtcEngine.updateChannelMediaOptions(option);

            rtcEngine.enableAudioVolumeIndication(50, 3, true);
        }
    }

    public int pushExternalAudioFrame(byte[] data, long timestamp) {
        if (null != rtcEngine) {
            return rtcEngine.pushExternalAudioFrame(data, timestamp);
        }
        return -1;
    }

    public int createDataStream(DataStreamConfig config) {
        if (null != rtcEngine) {
            return rtcEngine.createDataStream(config);
        } else {
            return -1;
        }
    }

    public String getRoomName() {
        return roomName;
    }

    public String getUserName() {
        return userName;
    }

    public int sendDataStreamMessage(int streamId, String cmd, String message) {
        if (null == rtcEngine) {
            return -1;
        }
        int ret = -1;
        DataStreamModel model = new DataStreamModel();
        model.setCmd(cmd);

        String[] msgs = Utils.stringToStringArray(message, 100);
        if (msgs.length == 1) {
            model.setMessage(message);
            model.setIsEnd(true);
            ret = rtcEngine.sendStreamMessage(streamId, JSONObject.toJSON(model).toString().getBytes());
        } else if (msgs.length > 1) {
            for (int i = 0; i < msgs.length; i++) {
                model.setIsEnd(i == msgs.length - 1);
                model.setMessage(msgs[i]);
                ret = rtcEngine.sendStreamMessage(streamId, JSONObject.toJSON(model).toString().getBytes());
            }
        }
        return ret;
    }

    public void registerAudioFrameObserver(IAudioFrameObserver observer) {
        if (null != rtcEngine) {
            rtcEngine.registerAudioFrameObserver(observer);
        }
    }

    public void setRecordingAudioFrameParameters(int sampleRate, int channel, int mode, int samplesPerCall) {
        if (null != rtcEngine) {
            rtcEngine.setRecordingAudioFrameParameters(sampleRate, channel, mode, samplesPerCall);
        }
    }
}
