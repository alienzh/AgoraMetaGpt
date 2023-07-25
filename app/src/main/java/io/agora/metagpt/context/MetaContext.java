package io.agora.metagpt.context;


import static io.agora.rtc2.video.VideoEncoderConfiguration.STANDARD_BITRATE;

import android.content.Context;
import android.util.Log;
import android.view.TextureView;

import com.alibaba.fastjson.JSONObject;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import io.agora.base.VideoFrame;
import io.agora.meta.AvatarModelInfo;
import io.agora.meta.EnterSceneConfig;
import io.agora.meta.ILocalUserAvatar;
import io.agora.meta.IMetaScene;
import io.agora.meta.IMetaSceneEventHandler;
import io.agora.meta.IMetaService;
import io.agora.meta.IMetaServiceEventHandler;
import io.agora.meta.MetaSceneAssetsInfo;
import io.agora.meta.MetaSceneConfig;
import io.agora.meta.MetaServiceConfig;
import io.agora.meta.MetaUserInfo;
import io.agora.meta.MetaUserPositionInfo;
import io.agora.metagpt.inf.IRtcEventCallback;
import io.agora.metagpt.models.DataStreamModel;
import io.agora.metagpt.models.EnterSceneExtraInfo;
import io.agora.metagpt.utils.Constants;
import io.agora.metagpt.utils.KeyCenter;
import io.agora.metagpt.utils.Utils;
import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.DataStreamConfig;
import io.agora.rtc2.IAudioFrameObserver;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoEncoderConfiguration;

public class MetaContext implements IMetaServiceEventHandler, IMetaSceneEventHandler {

    private final static String TAG = Constants.TAG + "-" + MetaContext.class.getSimpleName();
    private volatile static MetaContext instance = null;

    private RtcEngine rtcEngine;
    private IMetaService metaService;
    private IMetaScene metaScene;
    private MetaSceneAssetsInfo sceneInfo;
    private AvatarModelInfo modelInfo;
    private MetaUserInfo userInfo;
    private String roomName;
    private int uid;
    private String userName;
    private TextureView sceneView;
    private final ConcurrentHashMap<IMetaServiceEventHandler, Integer> metaServiceEventHandlerMap;
    private final ConcurrentHashMap<IMetaSceneEventHandler, Integer> metaSceneEventHandlerMap;
    private ILocalUserAvatar localUserAvatar;

    private IRtcEventCallback iRtcEventCallback;

    private boolean isInitmeta;

    private String avatarType;


    private MetaContext() {
        metaServiceEventHandlerMap = new ConcurrentHashMap<>();
        metaSceneEventHandlerMap = new ConcurrentHashMap<>();
        iRtcEventCallback = null;
        isInitmeta = false;
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
                metaService = IMetaService.create();
                MetaServiceConfig config = new MetaServiceConfig() {{
                    mRtcEngine = rtcEngine;
                    mAppId = KeyCenter.APP_ID;
                    mRtmToken = KeyCenter.getRtmToken(uid);
                    mLocalDownloadPath = context.getExternalCacheDir().getPath();
                    mUserId = String.valueOf(uid);
                    mEventHandler = MetaContext.this;
                }};
                ret += metaService.initialize(config);
                Log.i(TAG, "launcher version=" + metaService.getLauncherVersion(context));

                isInitmeta = true;
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

                rtcEngine.registerExtension("agora_video_filters_metakit", "metakit", io.agora.rtc2.Constants.MediaSourceType.CUSTOM_VIDEO_SOURCE);

                rtcEngine.setExternalVideoSource(true, true, io.agora.rtc2.Constants.ExternalVideoSourceType.VIDEO_FRAME);

                rtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(
                        new VideoEncoderConfiguration.VideoDimensions(240, 240),
                        VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_30,
                        STANDARD_BITRATE,
                        VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_LANDSCAPE, VideoEncoderConfiguration.MIRROR_MODE_TYPE.MIRROR_MODE_DISABLED));


                rtcEngine.setParameters("{\"rtc.enable_debug_log\":true}");
                rtcEngine.enableAudio();
                //rtcEngine.enableVideo();
                rtcEngine.setAudioProfile(
                        io.agora.rtc2.Constants.AUDIO_PROFILE_DEFAULT, io.agora.rtc2.Constants.AUDIO_SCENARIO_GAME_STREAMING
                );
                rtcEngine.setDefaultAudioRoutetoSpeakerphone(true);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public void destroy() {
        if (isInitmeta) {
            IMetaService.destroy();
            metaService = null;
            isInitmeta = false;
        }

        RtcEngine.destroy();
        rtcEngine = null;

    }

    public void registerMetaServiceEventHandler(IMetaServiceEventHandler eventHandler) {
        metaServiceEventHandlerMap.put(eventHandler, 0);
    }

    public void unregisterMetaServiceEventHandler(IMetaServiceEventHandler eventHandler) {
        metaServiceEventHandlerMap.remove(eventHandler);
    }

    public void registerMetaSceneEventHandler(IMetaSceneEventHandler eventHandler) {
        metaSceneEventHandlerMap.put(eventHandler, 0);
    }

    public void unregisterMetaSceneEventHandler(IMetaSceneEventHandler eventHandler) {
        metaSceneEventHandlerMap.remove(eventHandler);
    }

    public boolean getSceneInfos() {
        return metaService.getSceneAssetsInfo() == io.agora.rtc2.Constants.ERR_OK;
    }

    public boolean isSceneDownloaded(MetaSceneAssetsInfo sceneInfo) {
        return metaService.isSceneAssetsDownloaded(sceneInfo.mSceneId) > 0;
    }

    public boolean downloadScene(MetaSceneAssetsInfo sceneInfo) {
        return metaService.downloadSceneAssets(sceneInfo.mSceneId) == io.agora.rtc2.Constants.ERR_OK;
    }

    public boolean cancelDownloadScene(MetaSceneAssetsInfo sceneInfo) {
        return metaService.cancelDownloadSceneAssets(sceneInfo.mSceneId) == io.agora.rtc2.Constants.ERR_OK;
    }

    public void prepareScene(MetaSceneAssetsInfo sceneInfo, AvatarModelInfo modelInfo, MetaUserInfo userInfo) {
        this.sceneInfo = sceneInfo;
        this.modelInfo = modelInfo;
        this.userInfo = userInfo;
    }

    public boolean createScene(Context activityContext, TextureView tv) {
        Log.d(TAG, "createAndEnterScene");
        this.sceneView = tv;

        MetaSceneConfig sceneConfig = new MetaSceneConfig();
        sceneConfig.mActivityContext = activityContext;

        int ret = -1;
        if (metaScene == null) {
            ret = metaService.createScene(sceneConfig);
        }

        return ret == io.agora.rtc2.Constants.ERR_OK;
    }

    public void enterScene() {
        Log.d(TAG, "enterScene");
        if (null != localUserAvatar) {
            localUserAvatar.setUserInfo(userInfo);
            //该model的mBundleType为MetaBundleInfo.BundleType.BUNDLE_TYPE_AVATAR类型
            localUserAvatar.setModelInfo(modelInfo);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("2dbg", "");
            jsonObject.put("avatar", avatarType);
            localUserAvatar.setExtraInfo(jsonObject.toJSONString().getBytes());

        }
        if (null != metaScene) {
            //设置回调接口
            metaScene.addEventHandler(MetaContext.getInstance());
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
            extraInfo.setSceneIndex(Constants.SCENE_INDEX_LIVE);

            //加载的场景index
            config.mExtraInfo = JSONObject.toJSONString(extraInfo).getBytes();
            metaScene.enterScene(config);
        }

    }

    @Override
    public void onCreateSceneResult(IMetaScene scene, int errorCode) {
        Log.d(TAG, "onCreateSceneResult errorCode:" + errorCode);
        metaScene = scene;
        localUserAvatar = metaScene.getLocalUserAvatar();
        for (IMetaServiceEventHandler handler : metaServiceEventHandlerMap.keySet()) {
            handler.onCreateSceneResult(scene, errorCode);
        }
    }


    public boolean updateRoleSpeak(boolean isSpeak) {
        int ret = io.agora.rtc2.Constants.ERR_OK;
        ret += rtcEngine.updateChannelMediaOptions(new ChannelMediaOptions() {{
            publishMicrophoneTrack = isSpeak;
            publishCustomAudioTrack = isSpeak;
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
        if (metaScene != null) {
            ret += leaveRtcChannel();
            ret += metaScene.leaveScene();
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
        if (state == ConnectionState.META_CONNECTION_STATE_ABORTED) {
            leaveScene();
        }

        for (IMetaServiceEventHandler handler : metaServiceEventHandlerMap.keySet()) {
            handler.onConnectionStateChanged(state, reason);
        }
    }

    @Override
    public void onTokenWillExpire() {

    }

    @Override
    public void onGetSceneAssetsInfoResult(MetaSceneAssetsInfo[] scenes, int errorCode) {
        Log.i(TAG, "onGetSceneAssetsInfoResult scenes:" + scenes.length);
        for (IMetaServiceEventHandler handler : metaServiceEventHandlerMap.keySet()) {
            handler.onGetSceneAssetsInfoResult(scenes, errorCode);
        }
    }

    @Override
    public void onDownloadSceneAssetsProgress(long sceneId, int progress, int state) {
        for (IMetaServiceEventHandler handler : metaServiceEventHandlerMap.keySet()) {
            handler.onDownloadSceneAssetsProgress(sceneId, progress, state);
        }
    }


    @Override
    public void onEnterSceneResult(int errorCode) {
        Log.d(TAG, String.format("onEnterSceneResult %d", errorCode));
        if (errorCode == 0) {
            if (null != metaScene) {
                metaScene.setSceneParameters("{\"debugUnity\":true}");
            }
            joinChannel(io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER);

        }
        for (IMetaSceneEventHandler handler : metaSceneEventHandlerMap.keySet()) {
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
            metaScene.release();
            metaScene = null;
        }

        for (IMetaSceneEventHandler handler : metaSceneEventHandlerMap.keySet()) {
            handler.onLeaveSceneResult(errorCode);
        }
    }

    @Override
    public void onReleasedScene(int status) {
        Log.d(TAG, String.format("onReleasedScene %d", status));
        for (IMetaSceneEventHandler handler : metaSceneEventHandlerMap.keySet()) {
            handler.onReleasedScene(status);
        }
    }

    @Override
    public void onSceneVideoFrameCaptured(TextureView view, VideoFrame videoFrame) {
        for (IMetaSceneEventHandler handler : metaSceneEventHandlerMap.keySet()) {
            handler.onSceneVideoFrameCaptured(view, videoFrame);
        }
    }

    @Override
    public void onAddSceneViewResult(TextureView view, int errorCode) {
        for (IMetaSceneEventHandler handler : metaSceneEventHandlerMap.keySet()) {
            handler.onAddSceneViewResult(view, errorCode);
        }
    }

    @Override
    public void onRemoveSceneViewResult(TextureView view, int errorCode) {
        for (IMetaSceneEventHandler handler : metaSceneEventHandlerMap.keySet()) {
            handler.onRemoveSceneViewResult(view, errorCode);
        }
    }

    @Override
    public void onSceneMessageReceived(byte[] message) {
        Log.d(TAG, String.format("onSceneMessageReceived %s", new String(message)));
        for (IMetaSceneEventHandler handler : metaSceneEventHandlerMap.keySet()) {
            handler.onSceneMessageReceived(message);
        }
    }

    @Override
    public void onUserPositionChanged(String uid, MetaUserPositionInfo posInfo) {
        Log.d(TAG, String.format("onUserPositionChanged %s %s %s %s %s", uid,
                Arrays.toString(posInfo.mPosition),
                Arrays.toString(posInfo.mForward),
                Arrays.toString(posInfo.mRight),
                Arrays.toString(posInfo.mUp)
        ));
        for (IMetaSceneEventHandler handler : metaSceneEventHandlerMap.keySet()) {
            handler.onUserPositionChanged(uid, posInfo);
        }
    }


    public void setAvatarType(String avatarType) {
        this.avatarType = avatarType;
    }

    public MetaSceneAssetsInfo getSceneInfo() {
        return sceneInfo;
    }

    public void sendSceneMessage(String msg) {
        if (metaScene == null) {
            Log.e(TAG, "sendMessageToScene metaScene is null");
            return;
        }

        metaScene.sendSceneMessage(msg.getBytes());
    }


    public boolean isInitmeta() {
        return isInitmeta;
    }

    public void setRtcEventCallback(IRtcEventCallback iRtcEventCallback) {
        this.iRtcEventCallback = iRtcEventCallback;
    }

    public RtcEngine getRtcEngine() {
        return rtcEngine;
    }

    public int getSceneId() {
        return Constants.SCENE_ID_GPT;
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

            rtcEngine.enableAudioVolumeIndication(40, 3, true);
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

    public void setPlaybackAudioFrameParameters(int sampleRate, int channel, int mode, int samplesPerCall) {
        if (null != rtcEngine) {
            rtcEngine.setPlaybackAudioFrameParameters(sampleRate, channel, mode, samplesPerCall);
        }
    }

    public int pullPlaybackAudioFrame(byte[] data, int lengthInByte) {
        if (null != rtcEngine) {
            return rtcEngine.pullPlaybackAudioFrame(data, lengthInByte);
        }
        return -1;
    }

    public int setExternalAudioSink(boolean enabled, int sampleRate, int channels) {
        if (null != rtcEngine) {
            return rtcEngine.setExternalAudioSink(enabled, sampleRate, channels);
        }
        return -1;
    }

    public void enableVoiceDriveAvatar(boolean enable) {
        if (null != metaScene) {
            metaScene.enableVoiceDriveAvatar(enable);
        }
    }

    public void pushAudioToDriveAvatar(byte[] data, long timestamp) {
        if (null != metaScene) {
            metaScene.pushAudioToDriveAvatar(data, timestamp, Constants.RTC_AUDIO_SAMPLE_RATE, Constants.RTC_AUDIO_SAMPLE_NUM_OF_CHANNEL);
        }
    }
}
