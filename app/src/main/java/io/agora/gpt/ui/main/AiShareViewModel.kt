package io.agora.gpt.ui.main

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.TextureView
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.agora.aiengine.AIEngine
import io.agora.aiengine.AIEngineCallback
import io.agora.aiengine.AIEngineConfig
import io.agora.aiengine.model.VirtualHumanVendor
import io.agora.aigc.sdk.constants.HandleResult
import io.agora.aigc.sdk.constants.Language
import io.agora.aigc.sdk.constants.NoiseEnvironment
import io.agora.aigc.sdk.constants.ServiceCode
import io.agora.aigc.sdk.constants.ServiceEvent
import io.agora.aigc.sdk.constants.SpeechRecognitionCompletenessLevel
import io.agora.aigc.sdk.model.AIRole
import io.agora.aigc.sdk.model.Data
import io.agora.aigc.sdk.model.ServiceVendor
import io.agora.aigc.sdk.model.ServiceVendorGroup
import io.agora.gpt.MainApplication
import io.agora.gpt.utils.Constant
import io.agora.gpt.utils.KeyCenter
import io.agora.gpt.utils.SingleLiveEvent
import io.agora.gpt.utils.ToastUtils
import io.agora.gpt.utils.Utils
import io.agora.mediaplayer.Constants
import io.agora.mediaplayer.IMediaPlayer
import io.agora.mediaplayer.IMediaPlayerObserver
import io.agora.mediaplayer.data.PlayerUpdatedInfo
import io.agora.mediaplayer.data.SrcInfo

class AiShareViewModel : ViewModel(), AIEngineCallback {

    private val TAG = "zhangw-" + AiShareViewModel::class.java.simpleName

    private val mHandler by lazy {
        Handler(Looper.getMainLooper())
    }

    private var mAiEngine: AIEngine? = null

    private val mAiEngineConfig: AIEngineConfig = AIEngineConfig()

    val mEventResultModel: MutableLiveData<EventResultModel> = SingleLiveEvent()
    val mDownloadProgress: MutableLiveData<DownloadProgressModel> = SingleLiveEvent()
    val mPrepareResult: MutableLiveData<Boolean> = SingleLiveEvent()
    val mDownloadRes: MutableLiveData<DownloadResModel> = SingleLiveEvent()
    val mDownloadResFinish: MutableLiveData<Boolean> = SingleLiveEvent()

    val mNewLineMessageModel: MutableLiveData<Triple<ChatMessageModel, Boolean, Int>> = SingleLiveEvent()

    val mAIEngineInit: MutableLiveData<Boolean> = SingleLiveEvent()

    val mVirtualHumanStart: MutableLiveData<Boolean> = SingleLiveEvent()
    val mVirtualHumanStop: MutableLiveData<Boolean> = SingleLiveEvent()

    // 禁音按钮
    private var mMute: Boolean = false

    val mChatMessageDataList = mutableListOf<ChatMessageModel>()

    private val mNeedEnableVirtualHuman: Boolean = true
    private var mEnableVirtualHuman: Boolean = false

    private var mMediaPlayer: IMediaPlayer? = null

    val mVideoDurationData: MutableLiveData<Long> = SingleLiveEvent()
    val mVideoStartData: MutableLiveData<Boolean> = SingleLiveEvent()
    val mVideoCurrentPosition: MutableLiveData<Long> = SingleLiveEvent()

    fun initAiEngine() {
        val userId = KeyCenter.mUserUid
        val roomName = io.agora.aigc.sdk.utils.Utils.getCurrentDateStr("yyyyMMddHHmmss") +
                io.agora.aigc.sdk.utils.Utils.getRandomString(2)
        Log.d(TAG, "roomName:$roomName")
        val virtualHumanUid = KeyCenter.mVirtualHumanUid
        mAiEngineConfig.apply {
            mLanguage = Language.ZH_CN
            mContext = MainApplication.mGlobalApplication.applicationContext
            mCallback = this@AiShareViewModel
            mEnableLog = true
            mEnableSaveLogToFile = true
            mUserName = KeyCenter.mUserName
            mUid = userId
            mRtcAppId = KeyCenter.APP_ID
            mRtcToken = KeyCenter.getRtcToken(roomName, userId)
            mRtmToken = KeyCenter.getRtmToken(userId)
            mVirtualHumanUid = virtualHumanUid
            mVirtualHumanRtcToken = KeyCenter.getRtcToken(roomName, virtualHumanUid)
            mRoomName = roomName
            mEnableVoiceChange = false
            mSpeechRecognitionFiltersLength = 1
            mEnableChatConversation = true
            noiseEnvironment = NoiseEnvironment.NOISE
            speechRecognitionCompletenessLevel = SpeechRecognitionCompletenessLevel.NORMAL
        }
        if (mAiEngine == null) {
            mAiEngine = AIEngine.create()
        }
        mAiEngine?.initialize(mAiEngineConfig)
        mChatMessageDataList.clear()
    }

    fun isAIGCEngineInit(): Boolean = mAIEngineInit.value == true

    fun checkDownloadRes() {
        mAiEngine?.checkDownloadRes()
    }

    // ServiceVendorGroup{
    // sttList= [STTVendor{id='xunfei', vendorName='xunfei', accountInJson='null'},
    // STTVendor{id='microsoft', vendorName='microsoft', accountInJson='null'}],
    // llmList=[LLMVendor{id='minimax-abab5.5-chat', vendorName='minimax', model='abab5.5-chat', accountInJson='null'},
    // LLMVendor{id='azureOpenai-gpt-4', vendorName='azureOpenai', model='gpt-4', accountInJson='null'}],
    // ttsList=[TTSVendor{id='microsoft-en-US-Jenny-cheerful', vendorName='microsoft', language='en-US', voiceName='Jenny', voiceNameValue='en-US-JennyNeural', voiceNameStyle='cheerful', accountInJson='null'},
    // TTSVendor{id='microsoft-en-US-Jenny-gentle', vendorName='microsoft', language='en-US', voiceName='Jenny', voiceNameValue='en-US-JennyNeural', voiceNameStyle='gentle', accountInJson='null'},
    // TTSVendor{id='microsoft-en-US-Davis-cheerful', vendorName='microsoft', language='en-US', voiceName='Davis', voiceNameValue='en-US-DavisNeural', voiceNameStyle='cheerful', accountInJson='null'},
    // TTSVendor{id='elevenLabs-Matilda', vendorName='elevenLabs', language='', voiceName='Matilda', voiceNameValue='XrExE9yKIg1WjnnlVkGX', voiceNameStyle='', accountInJson='null'}]}

    fun setServiceVendor(aiRole: AIRole) {
        val serviceVendors: ServiceVendorGroup = mAiEngine?.serviceVendors ?: return
        Log.d(TAG, "serviceVendors $serviceVendors")
        val serviceVendor = ServiceVendor()
        for (sttVendor in serviceVendors.sttList) {
            if (sttVendor.id.equals("xunfei", ignoreCase = true)) {
                serviceVendor.sttVendor = sttVendor
                break
            }
        }
        for (llmVendor in serviceVendors.llmList) {
            if (llmVendor.id.equals("minimax-abab5.5-chat", ignoreCase = true)) {
                serviceVendor.llmVendor = llmVendor
                break
            }
        }
        for (ttsVendor in serviceVendors.ttsList) {
            if (ttsVendor.id.equals("microsoft-zh-CN-xiaoxiao-cheerful", true)) {
                serviceVendor.ttsVendor = ttsVendor
                break
            }
        }
        Log.d(TAG, "setServiceVendor $serviceVendor")
        mAiEngine?.setServiceVendor(serviceVendor)
    }

    //setVirtualHumanVendors
    // [VirtualHumanVendor{vendorName='senseTime',
    // videos=[
    // VirtualHumanVideo{id='yuxuan', name='羽萱', description='', params={"name":"羽萱","scale":1,"id":4054,"position":{"x":10,"y":60},"face_feature_id":"0404_jiaboyang_s1","url":"https://dwg-aigc-paas.oss-cn-hangzhou.aliyuncs.com/materials/77/0404_jiaboyang_s1_20230427131242239.zip"}},
    // VirtualHumanVideo{id='ruxue', name='茹雪', description='', params={"name":"茹雪","scale":1,"id":218880,"position":{"x":10,"y":60},"face_feature_id":"e564c3cd6e8740cdae052f036dc3793d_s1","url":"https://dwg-aigc-paas.oss-cn-hangzhou.aliyuncs.com/materials/77/cut_470023ab074e477c9fd4dcefe4954995_s1_result_20230921192127182.zip"}},
    // VirtualHumanVideo{id='menghan', name='梦涵', description='', params={"name":"梦涵","scale":1,"id":167674,"position":{"x":10,"y":60},"face_feature_id":"c91086e5b59e4a12ba9e9cc0b0253969_s1","url":"https://dwg-aigc-paas.oss-cn-hangzhou.aliyuncs.com/materials/77/package_1688540310650_20230831115015361.zip"}}],
    // voices=[ VirtualHumanVideo{id='zh-CN-XiaoxiaoNeural', name='xiaoxiao', description='', params={"volume":400,"vendor_id":5,"pitch_offset":0,"speed_ratio":1,"name":"xiaoxiao","language":"zh-CN","id":"zh-CN-XiaoxiaoNeural"}}]},
    // VirtualHumanVendor{vendorName='xiaoIce',
    // videos=[VirtualHumanVideo{id='e5aa1468-5c4d-11ee-9e9f-d1d5100e790e', name='metastaff-agora-ann-test-a', description='', params={}}],
    // voices=[VirtualHumanVideo{id='e5aa1468-5c4d-11ee-9e9f-d1d5100e790e', name='metastaff-agora-ann-test-a', description='', params={}}]}]
    fun setVirtualHumanVendors() {
        val virtualHumanVendors: List<VirtualHumanVendor> = mAiEngine?.virtualHumanVendors ?: return
        Log.d(TAG, "setVirtualHumanVendors $virtualHumanVendors")
        for (virtualVendor in virtualHumanVendors) {
            if (virtualVendor.vendorName.equals("xiaoIce", true)) {
                val serviceVendor = io.agora.aiengine.model.ServiceVendor().apply {
                    vendorName = virtualVendor.vendorName
                    if (virtualVendor.videos.isNotEmpty()) {
                        video = virtualVendor.videos[0]
                    }
                    if (virtualVendor.voices.isNotEmpty()) {
                        voice = virtualVendor.voices[0]
                    }
                }
                mAiEngine?.setVirtualHumanVendor(serviceVendor)
                break
            }
        }
    }

    // stt-llm-tts 同一个 roundId
    // 语音转文字的回调
    override fun onSpeech2TextResult(sid: String?, result: Data<String>, isRecognizedSpeech: Boolean): HandleResult {
        Log.i(TAG, "onSpeech2TextResult sid:$sid,result:$result,isRecognizedSpeech:$isRecognizedSpeech")
        var tempSid = sid
        if (tempSid.isNullOrEmpty()) {
            tempSid = KeyCenter.getRandomString(20)
        }

        mHandler.post {
            val oldChatMessageModel = mChatMessageDataList.find { it.sid == tempSid && !it.isAiMessage }
            if (oldChatMessageModel != null) {
                val index = mChatMessageDataList.indexOf(oldChatMessageModel)
                oldChatMessageModel.message = result.data
                mNewLineMessageModel.value = Triple(oldChatMessageModel, false, index)
            } else {
                val messageModel = ChatMessageModel(
                    isAiMessage = false,
                    sid = tempSid,
                    name = KeyCenter.mUserName ?: "",
                    message = result.data,
                )
                mChatMessageDataList.add(messageModel)
                mNewLineMessageModel.value = Triple(messageModel, true, mChatMessageDataList.size - 1)
            }
        }
        return HandleResult.CONTINUE
    }

    // gpt的回答，是流式的，同一个回答，是同一个sid
    override fun onLLMResult(sid: String?, answer: Data<String>): HandleResult {
        Log.i(TAG, "onLLMResult sid:$sid answer:$answer")
        var tempSid = sid
        if (tempSid.isNullOrEmpty()) {
            tempSid = KeyCenter.getRandomString(20)
        }
        // fix FT-1018
        if (isLLMPrompt(tempSid)) {
            Log.i(TAG, "onLLMResult isPartner isLLMPrompt")
        } else {
            mHandler.post {
                val oldChatMessageModel = mChatMessageDataList.find { it.sid == tempSid && it.isAiMessage }
                if (oldChatMessageModel != null) {
                    val index = mChatMessageDataList.indexOf(oldChatMessageModel)
                    oldChatMessageModel.message = oldChatMessageModel.message.plus(answer.data)
                    mNewLineMessageModel.value = Triple(oldChatMessageModel, false, index)
                } else {
                    val messageModel = ChatMessageModel(
                        isAiMessage = true,
                        sid = tempSid,
                        name = "AI主播",
                        message = answer.data,
//                    costTime = System.currentTimeMillis() - (mSttEndTimeMap[sid] ?: mLastSpeech2TextTime)
                    )
                    mChatMessageDataList.add(messageModel)
                    mNewLineMessageModel.value = Triple(messageModel, true, mChatMessageDataList.size - 1)
                }
            }
        }
        if (isLLMPrompt(tempSid)) {
            return HandleResult.DISCARD
        } else {
            return HandleResult.CONTINUE
        }
    }

    private fun isLLMPrompt(sid: String): Boolean {
        return sid.length == 64 && sid.startsWith("00000000000000000000000000000000")
    }

    // tts
    override fun onText2SpeechResult(
        roundId: String?, voice: Data<ByteArray>?, sampleRates: Int, channels: Int, bits: Int
    ): HandleResult {

        Log.i(TAG, "onText2SpeechResult roundId:$roundId,sampleRates:$sampleRates,channels:$channels,bits:$bits")
        var tempSid = roundId
        if (tempSid.isNullOrEmpty()) {
            tempSid = KeyCenter.getRandomString(20)
        }
        return HandleResult.CONTINUE
    }

    override fun onCheckDownloadResFail(errorCode: Int, msg: String?) {
        Log.i(TAG, "onCheckDownloadResFail errorCode:$errorCode,msg:$msg")
        mHandler.post {
            ToastUtils.showToast("onCheckDownloadResFail errorCode:$errorCode,msg:$msg")
        }
    }

    override fun onDownloadResFinish() {
        Log.i(TAG, "onDownloadResFinish")
        mHandler.post {
            mDownloadResFinish.value = true
        }
    }

    override fun onCheckDownloadResResult(totalSize: Long, fileCount: Int) {
        Log.i(TAG, "onCheckDownloadResResult totalSize:$totalSize,fileCount:$fileCount")
        mHandler.post {
            if (totalSize == 0L) { // totalSize:0 目前就表示不用下载的
                mDownloadResFinish.value = true
            } else {
                mDownloadRes.value = DownloadResModel(totalSize, fileCount)
            }
        }
    }

    override fun onDownloadResProgress(progress: Int, index: Int, count: Int) {
        Log.i(TAG, "onDownloadResProgress progress:$progress,index:$index,count:$count")
        mHandler.post {
            mDownloadProgress.value = DownloadProgressModel(progress, index, count)
        }
    }

    override fun onEventResult(event: ServiceEvent, code: ServiceCode, msg: String?) {
        Log.d(TAG, "onEventResult event:$event,code:$code,msg:$msg")
        mHandler.post {
            when (event) {
                ServiceEvent.INITIALIZE -> {
                    if (code == ServiceCode.SUCCESS) {
                        mAIEngineInit.postValue(true)
                    } else {
                        ToastUtils.showToast("AIGC Initialize error:$code,msg:$msg")
                    }
                }

                ServiceEvent.START -> {
                    if (code == ServiceCode.SUCCESS) {
                        mMediaPlayer?.let { iMediaPlayer ->
                            if (mVideoState == Constants.MediaPlayerState.PLAYER_STATE_OPEN_COMPLETED ||
                                mVideoState == Constants.MediaPlayerState.PLAYER_STATE_PLAYBACK_ALL_LOOPS_COMPLETED
                            ) {
                                iMediaPlayer.play()
                                checkPushTxtToTts(0)
                            } else if (mVideoState == Constants.MediaPlayerState.PLAYER_STATE_PAUSED) {
                                iMediaPlayer.resume()
                            } else {
                                Log.d(TAG, "onEventResult start iMediaPlayer state $mVideoState")
                            }
                        }
                    } else {
                        ToastUtils.showToast("AIGC Start error:$code,msg:$msg")
                    }
                }

                ServiceEvent.STOP -> {
                    if (code == ServiceCode.SUCCESS) {

                    } else {
                        ToastUtils.showToast("AIGC Stop error:$code,msg:$msg")
                    }
                }
            }
            mEventResultModel.value = EventResultModel(event, code)
        }
    }

    override fun onPrepareResult(code: Int, msg: String?) {
        Log.i(TAG, "onPrepareResult code:$code,msg:$msg")
        mHandler.post {
            if (code == ServiceCode.SUCCESS.code) {
                innerStartEnableVirtualHuman {
                    startVirtualHumanCallback = null
                }
            } else {
                ToastUtils.showToast("onPrepareResult code:$code,msg:$msg")
                mPrepareResult.postValue(false)
            }
        }
    }

    override fun onUpdateConfigResult(code: Int, msg: String?) {
        Log.i(TAG, "onUpdateConfigResult code:$code,msg:$msg")
    }

    override fun onVirtualHumanStart(code: Int, msg: String?) {
        Log.i(TAG, "onVirtualHumanStart code:$code,msg:$msg")
        // 数字人开启后才能获取 rtcEngine
        mHandler.post {
            if (code == 0) {
                mEnableVirtualHuman = true
                startVirtualHumanCallback?.invoke(true)
                mVirtualHumanStart.postValue(true)
            } else {
                ToastUtils.showToast("数字人启动失败:$msg")
                startVirtualHumanCallback?.invoke(false)
            }
        }
    }

    override fun onVirtualHumanStop(code: Int, msg: String?) {
        Log.i(TAG, "onVirtualHumanStop code:$code,msg:$msg")
        mHandler.post {
            if (code == 0) {
                mEnableVirtualHuman = false
                stopVirtualHumanCallback?.invoke(true)
            } else {
                stopVirtualHumanCallback?.invoke(false)
            }
        }
    }

    fun currentRole(): AIRole? {
        return mAiEngine?.currentRole
    }

    fun cancelDownloadRes() {
        mAiEngine?.cancelDownloadRes()
    }

    fun prepare() {
        mAiEngine?.prepare()
    }

    fun downloadRes() {
        mAiEngine?.downloadRes()
    }

    // 获取可用的AIRole
    fun getUsableAiRoles(): List<AIRole> {
        val sdkAiRoles = mAiEngine?.roles ?: emptyArray()
        Log.d(TAG, "sdkAiRoles：$sdkAiRoles")
        val sdkAiRoleMap = sdkAiRoles.associateBy { it.roleId }

        val localRoleIds = if (mAiEngineConfig.mLanguage == Language.EN_US) {
            mutableListOf(Constant.EN_Role_ID_yunibobo, Constant.EN_Role_ID_WENDY, Constant.EN_Role_ID_Cindy)
        } else {
            mutableListOf(Constant.CN_Role_ID_yunibobo, Constant.CN_Role_ID_jingxiang)
        }

        val usableAiRoles = mutableListOf<AIRole>()
        for (i in localRoleIds.indices) {
            val roleId = localRoleIds[i]
            sdkAiRoleMap[roleId]?.let { aiRole ->
                usableAiRoles.add(aiRole)
            }
        }
        return usableAiRoles
    }

    fun setAiRole(aiRole: AIRole) {
        mAiEngine?.setRole(aiRole.roleId)
    }

    // 设置SDK使用的AIRol
    fun setAvatarModel(aiRole: AIRole) {
//        val avatarName = KeyCenter.getAvatarName(aiRole)
//        val avatarModel = AvatarModel().apply {
//            name = avatarName
//        }
//        val avatarDrawableStr = KeyCenter.getAvatarDrawableStr(aiRole)
//        avatarModel.bgFilePath = Utils.getCacheFilePath("$avatarDrawableStr.png")
//        mAiEngineConfig.mAvatarModel = avatarModel
//        mAiEngine?.updateConfig(mAiEngineConfig)
//        Log.d(TAG, "setRole:$aiRole,avatarModel:$avatarModel")
    }

    // 设置 texture
    fun setTexture(activity: Activity, texture: TextureView) {
        mAiEngineConfig.mTextureView = texture
        mAiEngineConfig.mActivity = activity
        mAiEngine?.updateConfig(mAiEngineConfig)
    }

    // 开启语聊
    fun startVoiceChat() {
        if (mNeedEnableVirtualHuman) {
            innerStartEnableVirtualHuman {
                startVirtualHumanCallback = null
                mAiEngine?.startVoiceChat()
            }
        } else {
            mAiEngine?.startVoiceChat()
        }
    }

    // 关闭语聊
    fun stopVoiceChat(callback: (Boolean) -> Unit) {
        mMediaPlayer?.pause()
        mAiEngine?.stopVoiceChat()
        callback.invoke(true)
        mMediaPlayer?.pause()
    }

    private var startVirtualHumanCallback: ((Boolean) -> Unit)? = null
    private var stopVirtualHumanCallback: ((Boolean) -> Unit)? = null

    private fun innerStartEnableVirtualHuman(callback: (Boolean) -> Unit) {
        startVirtualHumanCallback = callback
        if (mEnableVirtualHuman) {
            callback.invoke(true)
        } else {
            mAiEngine?.enableVirtualHuman(true)
        }
    }

    // 数字人关闭 rtcEngine 需要重新创建，不退出房间不关闭数字人
    private fun innerStopEnableVirtualHuman(callback: (Boolean) -> Unit) {
        stopVirtualHumanCallback = callback
        if (!mEnableVirtualHuman) {
            callback.invoke(true)
        } else {
            mAiEngine?.enableVirtualHuman(false)
        }
    }

    // 静音/取消静音
    fun mute(callback: (Boolean) -> Unit) {
        mMute = !mMute
        mAiEngine?.mute(mMute)
        callback.invoke(mMute)
    }

    fun pushText(command: String?, text: String? = null) {

        var content = ""
        when (command) {
            Constant.COMMAND_TOPIC -> {
                if (text.equals("/start")) {
                    content = "$text"
                } else {
                    content = "/$command $text"
                }
            }

            Constant.COMMAND_EVALUATE -> {
                content = "/$command"
            }

            Constant.COMMAND_START -> {
                content = "/$command"
            }

            null -> {
                content = text ?: ""
            }

            else -> {
                Log.d(TAG, "not support command：$command")
            }
        }
        if (content.isNotEmpty()) {
            Log.d(TAG, "pushText:$content")
            mAiEngine?.pushText(content)
        }
    }

    fun mayReleaseEngine() {
        innerStopEnableVirtualHuman {
            if (mEventResultModel.value?.event == ServiceEvent.START) {
                stopVoiceChat {
                    innerStopEnableVirtualHuman {
                        innerReleaseEngine()
                    }
                }
            } else {
                innerReleaseEngine()
            }
        }
    }

    private fun innerReleaseEngine() {
        mMediaPlayer?.let {
            it.unRegisterPlayerObserver(mMediaPlayerObserver)
            it.destroy()
            mMediaPlayer = null
            mVideoState = Constants.MediaPlayerState.PLAYER_STATE_IDLE
        }
        KeyCenter.mUserName = ""
        mMute = false
        mChatMessageDataList.clear()
        AIEngine.destroy()
        mAiEngine = null
        lastSportTxt = null
    }

    //===== mediaPlayer ==========
    fun startOpenVideo(texture: TextureView, url: String) {
        if (mMediaPlayer == null) {
            if (mAiEngine?.rtcEngine == null) {
                ToastUtils.showToast("createMediaPlayer rtcEngine==null")
                Log.d(TAG, "createMediaPlayer rtcEngine==null")
            }
            mMediaPlayer = mAiEngine?.rtcEngine?.createMediaPlayer()
        }
        if (mMediaPlayer == null) {
            ToastUtils.showToast("createMediaPlayer error")
            return
        }

        mMediaPlayer?.let { iMediaPlayer ->
            iMediaPlayer.registerPlayerObserver(mMediaPlayerObserver)
            iMediaPlayer.setView(texture)
            iMediaPlayer.open(url, 0)
        }
    }

    val mVideoDuration: Int
        get() = mVideoDurationData.value?.toInt() ?: 0

    private var mSportTxtByMins = mutableListOf<SportsTextModel>()

    fun seekPlay(progress: Int) {
        mSportTxtByMins.clear()
        mMediaPlayer?.let { iMediaPlayer ->
            iMediaPlayer.seek(progress.toLong())
        }
    }

    private var mVideoState: Constants.MediaPlayerState = Constants.MediaPlayerState.PLAYER_STATE_IDLE

    private var lastSportTxt: SportsTextModel? = null
    private fun checkPushTxtToTts(position: Long) {
        if (mSportTxtByMins.isEmpty()) {
            KeyCenter.mSportList.filter { it.time * 60 * 1000 - 500 < position && it.time * 60 * 1000 + 500 > position }
                .let {
                    mSportTxtByMins.addAll(it)
                }
        }
        if (mSportTxtByMins.isNotEmpty()) {
            Log.d(TAG, "onPositionChanged pushTxtToTTS mSportTxtByMins $mSportTxtByMins")
            mSportTxtByMins.removeFirst().apply {
                Log.d(TAG, "onPositionChanged pushTxtToTTS $time $content")
                if (lastSportTxt?.content != content) {
                    pushTxtToTTS(content, lastSportTxt?.time == time)
                }
                lastSportTxt = this
            }
        }
    }

    fun pushTxtToTTS(txt: String, isAppend: Boolean) {
        mAiEngine?.pushTxtToTTS(txt, isAppend)
        val tempSid = KeyCenter.getRandomString(20)
        val messageModel = ChatMessageModel(
            isAiMessage = false,
            sid = tempSid,
            name = "AI主播",
            message = txt,
        )
        mChatMessageDataList.add(messageModel)
        mNewLineMessageModel.value = Triple(messageModel, true, mChatMessageDataList.size - 1)
    }

    private val mMediaPlayerObserver = object : IMediaPlayerObserver {
        override fun onPlayerStateChanged(state: Constants.MediaPlayerState, error: Constants.MediaPlayerError) {
            Log.d(TAG, "onPlayerStateChanged $state $error")
            mHandler.post {
                mVideoState = state
                when (state) {
                    Constants.MediaPlayerState.PLAYER_STATE_OPEN_COMPLETED -> {
                        mMediaPlayer?.let { iMediaPlayer ->
                            iMediaPlayer.mute(true)
                            mVideoDurationData.postValue(iMediaPlayer.duration)
                            mPrepareResult.postValue(true)
                        }
                    }

                    Constants.MediaPlayerState.PLAYER_STATE_PLAYBACK_ALL_LOOPS_COMPLETED -> {
                        lastSportTxt = null
                    }

                    Constants.MediaPlayerState.PLAYER_STATE_PLAYING -> {
                        mVideoStartData.postValue(true)
                    }
                }
            }

        }

        override fun onPositionChanged(positionMs: Long, timestampMs: Long) {
            Log.d(TAG, "onPositionChanged $positionMs $timestampMs")
            mHandler.post {
                mVideoCurrentPosition.postValue(positionMs)
                checkPushTxtToTts(positionMs)
            }
        }

        override fun onPlayerEvent(p0: Constants.MediaPlayerEvent?, p1: Long, p2: String?) {
        }

        override fun onMetaData(p0: Constants.MediaPlayerMetadataType?, p1: ByteArray?) {
        }

        override fun onPlayBufferUpdated(p0: Long) {
        }

        override fun onPreloadEvent(p0: String?, p1: Constants.MediaPlayerPreloadEvent?) {
        }

        override fun onAgoraCDNTokenWillExpire() {
        }

        override fun onPlayerSrcInfoChanged(p0: SrcInfo?, p1: SrcInfo?) {
        }

        override fun onPlayerInfoUpdated(p0: PlayerUpdatedInfo?) {
        }

        override fun onAudioVolumeIndication(p0: Int) {

        }
    }
}