package io.agora.gpt.ui.main

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.TextureView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.agora.aiengine.AIEngine
import io.agora.aiengine.AIEngineCallback
import io.agora.aiengine.AIEngineConfig
import io.agora.aiengine.model.AvatarModel
import io.agora.aigc.sdk.constants.Constants
import io.agora.aigc.sdk.constants.HandleResult
import io.agora.aigc.sdk.constants.Language
import io.agora.aigc.sdk.constants.ServiceCode
import io.agora.aigc.sdk.constants.ServiceEvent
import io.agora.aigc.sdk.model.AIRole
import io.agora.aigc.sdk.model.Data
import io.agora.aigc.sdk.model.ServiceVendor
import io.agora.aigc.sdk.model.ServiceVendorGroup
import io.agora.gpt.MainApplication
import io.agora.gpt.R
import io.agora.gpt.utils.Constant
import io.agora.gpt.utils.KeyCenter
import io.agora.gpt.utils.SPUtil
import io.agora.gpt.utils.SingleLiveEvent
import io.agora.gpt.utils.ToastUtils
import io.agora.gpt.utils.Utils

class AiShareViewModel : ViewModel(), AIEngineCallback {

    private val TAG = "zhangw-" + AiShareViewModel::class.java.simpleName

    private val mHandler by lazy {
        Handler(Looper.getMainLooper())
    }

    private var mAiEngine: AIEngine? = null

    private val mAiEngineConfig: AIEngineConfig = AIEngineConfig()

    private val _mEventResultModel: MutableLiveData<EventResultModel> = SingleLiveEvent()
    val mEventResultModel: LiveData<EventResultModel> = _mEventResultModel
    private val _mDownloadProgress: MutableLiveData<DownloadProgressModel> = SingleLiveEvent()
    val mDownloadProgress: LiveData<DownloadProgressModel> = _mDownloadProgress
    val _mPrepareResult: MutableLiveData<Boolean> = SingleLiveEvent()
    val mPrepareResult: LiveData<Boolean> = _mPrepareResult
    val _mDownloadRes: MutableLiveData<DownloadResModel> = SingleLiveEvent()
    val mDownloadRes: LiveData<DownloadResModel> = _mDownloadRes
    val _mDownloadResFinish: MutableLiveData<Boolean> = SingleLiveEvent()
    val mDownloadResFinish: LiveData<Boolean> = _mDownloadResFinish

    val _mNewLineMessageModel: MutableLiveData<Triple<ChatMessageModel, Boolean, Int>> = SingleLiveEvent()
    val mNewLineMessageModel: LiveData<Triple<ChatMessageModel, Boolean, Int>> = _mNewLineMessageModel

    val _mAIEngineInit: MutableLiveData<Boolean> = SingleLiveEvent()
    val mAIEngineInit: MutableLiveData<Boolean> = _mAIEngineInit

    private var mMute: Boolean = false

    val mChatMessageDataList = mutableListOf<ChatMessageModel>()

    private val mSttEndTimeMap = mutableMapOf<String, Long>()
    private var mLastSpeech2TextTime: Long = 0
    private var mEnableEnglishTeacher = false
    private var mIsStartVoice: Boolean = false
    private var mEnableVirtualHuman: Boolean = false

    init {
        mAiEngineConfig.mLanguage = Language.JA_JP
    }

    private var mCurrentAiRole: AIRole? = null

    fun getCurAiRole(): AIRole {
        return mCurrentAiRole ?: getUsableAiRoles()[0]
    }

    fun currentLanguage(): Language {
        return mAiEngineConfig.mLanguage
    }

    fun initAiEngine() {
        val userId = KeyCenter.userUid
        val roomName = KeyCenter.roomName
        val virtualHumanUid = KeyCenter.virtualHumanUid
        mAiEngineConfig.apply {
            mContext = MainApplication.mGlobalApplication.applicationContext
            mCallback = this@AiShareViewModel
            mEnableLog = true
            mEnableSaveLogToFile = true
            mUserName = KeyCenter.userName
            mUid = userId
            mRtcAppId = KeyCenter.APP_ID
            mRtcToken = KeyCenter.getRtcToken(roomName, userId)
            mRtmToken = KeyCenter.getRtmToken(userId)
            mVirtualHumanUid = virtualHumanUid
            mVirtualHumanRtcToken = KeyCenter.getRtcToken(roomName, virtualHumanUid)
            mRoomName = roomName
            mEnableVoiceChange = false
            mEnableChatConversation = true
            mSpeechRecognitionFiltersLength = 0
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

    //sttList= [STTVendor{id='microsoft', vendorName='microsoft', accountInJson='null'}],
    // llmList=[LLMVendor{id='azureOpenai-gpt-35-turbo-16k', vendorName='azureOpenai', model='gpt-35-turbo-16k', accountInJson='null'}], 、
    // ttsList=[TTSVendor{id='microsoft-ja-JP-Nanami-cheerful-female', vendorName='microsoft', language='ja-JP', voiceName='Nanami', voiceNameValue='ja-JP-NanamiNeural', voiceNameStyle='cheerful', accountInJson='null'}]}
    fun setServiceVendor() {
        val serviceVendors: ServiceVendorGroup = mAiEngine?.serviceVendors ?: return
        Log.d(TAG, "serviceVendors $serviceVendors")
        val serviceVendor = ServiceVendor()
        for (sttVendor in serviceVendors.sttList) {
            if (sttVendor.id.equals("microsoft", ignoreCase = true)) {
                serviceVendor.sttVendor = sttVendor
                break
            }
        }
        for (llmVendor in serviceVendors.llmList) {
            if (llmVendor.id.equals("azureOpenai-gpt-35-turbo-16k", ignoreCase = true)) {
                serviceVendor.llmVendor = llmVendor
                break
            }
        }
        if (serviceVendors.ttsList.isNotEmpty()) {
            serviceVendor.ttsVendor = serviceVendors.ttsList[0]
        }
        Log.d(TAG, "setServiceVendor $serviceVendor")
        mAiEngine?.setServiceVendor(serviceVendor)
    }

    // stt-llm-tts 同一个 roundId
    // 语音转文字的回调
    override fun onSpeech2TextResult(sid: String?, result: Data<String>, isRecognizedSpeech: Boolean): HandleResult {
        Log.i(TAG, "onSpeech2TextResult sid:$sid,result:$result,isRecognizedSpeech:$isRecognizedSpeech")
        var tempSid = sid
        if (tempSid.isNullOrEmpty()) {
            tempSid = KeyCenter.getRandomString(20)
            Log.i(TAG, "onSpeech2TextResult tempSid:$tempSid,result:$result,isRecognizedSpeech:$isRecognizedSpeech")
        }
        mHandler.post {
            if (isRecognizedSpeech) {
                mLastSpeech2TextTime = System.currentTimeMillis()
                mSttEndTimeMap[tempSid] = System.currentTimeMillis()
            }
            val oldChatMessageModel = mChatMessageDataList.find { it.sid == tempSid && !it.isAiMessage }
            if (oldChatMessageModel != null) {
                val index = mChatMessageDataList.indexOf(oldChatMessageModel)
                oldChatMessageModel.message = result.data
                _mNewLineMessageModel.value = Triple(oldChatMessageModel, false, index)
            }else{
                val messageModel = ChatMessageModel(
                    isAiMessage = false,
                    sid = tempSid,
                    name = KeyCenter.userName ?: "",
                    message = result.data,
                    costTime = 0
                )
                mChatMessageDataList.add(messageModel)
                _mNewLineMessageModel.value = Triple(messageModel, true, mChatMessageDataList.size - 1)
            }
            // 用户说话只需要最后完整的就行
//            if (isRecognizedSpeech) {
//                mLastSpeech2TextTime = System.currentTimeMillis()
//                mSttEndTimeMap[tempSid] = System.currentTimeMillis()
//                val messageModel = ChatMessageModel(
//                    isAiMessage = false,
//                    sid = tempSid,
//                    name = KeyCenter.userName ?: "",
//                    message = result.data,
//                    costTime = 0
//                )
//                mChatMessageDataList.add(messageModel)
//                _mNewLineMessageModel.value = Triple(messageModel, true, mChatMessageDataList.size - 1)
//            }
        }
        return HandleResult.CONTINUE
    }

    // gpt的回答，是流式的，同一个回答，是同一个sid
    override fun onLLMResult(sid: String?, answer: Data<String>): HandleResult {
        Log.i(TAG, "onLLMResult sid:$sid answer:$answer")
        var tempSid = sid
        if (tempSid.isNullOrEmpty()) {
            tempSid = KeyCenter.getRandomString(20)
            Log.i(TAG, "onLLMResult tempSid:$tempSid answer:$answer")
        }
        mHandler.post {
            val oldChatMessageModel = mChatMessageDataList.find { it.sid == tempSid && it.isAiMessage }
            if (oldChatMessageModel != null) {
                val index = mChatMessageDataList.indexOf(oldChatMessageModel)
                oldChatMessageModel.message = oldChatMessageModel.message.plus(answer.data)
                _mNewLineMessageModel.value = Triple(oldChatMessageModel, false, index)
            } else {
                val messageModel = ChatMessageModel(
                    isAiMessage = true,
                    sid = tempSid,
                    name = getAiName(),
                    message = answer.data,
//                    costTime = System.currentTimeMillis() - (mSttEndTimeMap[sid] ?: mLastSpeech2TextTime)
                )
                // stt 识别完没有 sid ，一个新的sid，默认就是 ai 问询语句
                if (!mSttEndTimeMap.containsKey(tempSid)) {
                    mSttEndTimeMap[tempSid] = System.currentTimeMillis()
                }
                mChatMessageDataList.add(messageModel)
                _mNewLineMessageModel.value = Triple(messageModel, true, mChatMessageDataList.size - 1)
            }
        }
        return HandleResult.CONTINUE
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
        mHandler.post {
            val oldChatMessageModel = mChatMessageDataList.find { it.sid == tempSid && it.isAiMessage }
            if (oldChatMessageModel != null && oldChatMessageModel.costTime == 0L) {
                val index = mChatMessageDataList.indexOf(oldChatMessageModel)
                oldChatMessageModel.costTime =
                    System.currentTimeMillis() - (mSttEndTimeMap[tempSid] ?: mLastSpeech2TextTime)
                _mNewLineMessageModel.value = Triple(oldChatMessageModel, false, index)
            }
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
            _mDownloadResFinish.value = true
        }
    }

    override fun onCheckDownloadResResult(totalSize: Long, fileCount: Int) {
        Log.i(TAG, "onCheckDownloadResResult totalSize:$totalSize,fileCount:$fileCount")
        mHandler.post {
            if (totalSize == 0L) { // totalSize:0 目前就表示不用下载的
                _mDownloadResFinish.value = true
            } else {
                _mDownloadRes.value = DownloadResModel(totalSize, fileCount)
            }
        }
    }

    override fun onDownloadResProgress(progress: Int, index: Int, count: Int) {
        Log.i(TAG, "onDownloadResProgress progress:$progress,index:$index,count:$count")
        mHandler.post {
            _mDownloadProgress.value = DownloadProgressModel(progress, index, count)
        }
    }

    override fun onEventResult(event: ServiceEvent, code: ServiceCode, msg: String?) {
        Log.d(TAG, "onEventResult event:$event,code:$code,msg:$msg")
        mHandler.post {
            if (event == ServiceEvent.INITIALIZE) {
                if (code == ServiceCode.SUCCESS) {
                    mAIEngineInit.postValue(true)
                    Log.d(TAG, "AIGC INITIALIZE SUCCESS")
                } else {
                    ToastUtils.showToast("AIGC INITIALIZE error:$code,msg:$msg")
                }
            } else if (event == ServiceEvent.DESTROY && code == ServiceCode.SUCCESS) {
            }
            _mEventResultModel.postValue(EventResultModel(event, code))
        }
    }

    override fun onPrepareResult(code: Int, msg: String?) {
        Log.i(TAG, "onPrepareResult code:$code,msg:$msg")
        mHandler.post {
            _mPrepareResult.value = code == ServiceCode.SUCCESS.code
        }
    }

    override fun onUpdateConfigResult(code: Int, msg: String?) {
        Log.i(TAG, "onUpdateConfigResult code:$code,msg:$msg")
    }

    override fun onVirtualHumanStart(code: Int, msg: String?) {
        Log.i(TAG, "onVirtualHumanStart code:$code,msg:$msg")
        mHandler.post {
            if (code == 0) {
                mEnableVirtualHuman = true
                enableVirtualHumanCallback?.invoke(true)
            } else {

            }
        }

    }

    override fun onVirtualHumanStop(code: Int, msg: String?) {
        Log.i(TAG, "onVirtualHumanStop code:$code,msg:$msg")
        mHandler.post {
            if (code == 0) {
                mEnableVirtualHuman = false
                enableVirtualHumanCallback?.invoke(false)
            } else {

            }
        }
    }

    fun getAiName(): String {
        return mAiEngine?.currentRole?.roleName
            ?: MainApplication.mGlobalApplication.getString(R.string.default_ai_name)
    }

    fun isEnglishTeacher(aiRole: AIRole): Boolean {
        mEnableEnglishTeacher =
            mAiEngineConfig.mLanguage == Language.EN_US && (aiRole.roleName == "Wendy" || aiRole.roleName == "Cindy")
        return mEnableEnglishTeacher
    }

    fun enableEnglishTeacher(): Boolean {
        return mEnableEnglishTeacher && mIsStartVoice
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
        return sdkAiRoles.asList()
    }

    // 设置SDK使用的AIRol
    fun setAvatarModel(aiRole: AIRole) {
        val roleAvatars = KeyCenter.avatars
        val avatarModel = AvatarModel().apply {
            name = roleAvatars[0]
        }
        if (aiRole.gender == Constants.GENDER_MALE) {
            avatarModel.bgFilePath = Utils.getCacheFilePath("bg_ai_male.png")
        } else {
            avatarModel.bgFilePath =  Utils.getCacheFilePath("bg_ai_female.png")
        }
        mAiEngineConfig.mAvatarModel = avatarModel
        mAiEngineConfig.mEnableChatConversation = isEnglishTeacher(aiRole)
        mAiEngine?.updateConfig(mAiEngineConfig)
        mAiEngine?.setRole(aiRole.roleId)
        Log.d(TAG, "setRole:$aiRole,avatarModel:$avatarModel")
    }

    // 设置 texture
    fun setTexture(activity: Activity, texture: TextureView) {
        mAiEngineConfig.mTextureView = texture
        mAiEngineConfig.mActivity = activity
        mAiEngine?.updateConfig(mAiEngineConfig)
    }

    // 开启语聊
    fun startVoiceChat() {
        mIsStartVoice = true
        mAiEngine?.startVoiceChat()
        if (mEnableEnglishTeacher) {
            mAiEngineConfig.mEnableChatConversation = true
            pushText(Constant.COMMAND_INIT_CHAT_MESSAGE)
        } else {
            mAiEngineConfig.mEnableChatConversation = false
        }
    }

    // 结束语聊
    fun stopVoiceChat() {
        mIsStartVoice = false
        mAiEngine?.stopVoiceChat()
        mayDisableVirtualHuman()
    }

    // 开启/关闭 变声
    fun enableVoiceChange(callback: (Boolean) -> Unit) {
        val voiceChange = !mAiEngineConfig.mEnableVoiceChange
        mAiEngineConfig.mEnableVoiceChange = voiceChange
        mAiEngine?.updateConfig(mAiEngineConfig)
        callback.invoke(voiceChange)
    }

    // 静音/取消静音
    fun mute(callback: (Boolean) -> Unit) {
        mMute = !mMute
        mAiEngine?.mute(mMute)
        callback.invoke(mMute)
    }

    // 设置AI语言环境,只记录语言
    fun switchLanguage(language: Language, callback: (Language) -> Unit) {
        mAiEngineConfig.mLanguage = language
        callback.invoke(mAiEngineConfig.mLanguage)
    }

    fun pushText(command: String, text: String? = null) {

        var content = ""
        when (command) {
            Constant.COMMAND_TOPIC -> {
                content = "/$command $text"
            }

            Constant.COMMAND_EVALUATE -> {
                content = "/$command"
            }

            Constant.COMMAND_INIT_CHAT_MESSAGE -> {
                content = "/$command"
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

    private var enableVirtualHumanCallback: ((Boolean) -> Unit)? = null

    fun enableVirtualHuman(callback: (Boolean) -> Unit) {
        enableVirtualHumanCallback = callback
        if (mEnableVirtualHuman) {
            mAiEngine?.enableVirtualHuman(false)
        } else {
            mAiEngine?.enableVirtualHuman(true)
        }
    }

    private fun mayDisableVirtualHuman() {
        if (mEnableVirtualHuman) {
            mAiEngine?.enableVirtualHuman(false)
        }
    }

    fun releaseEngine() {
        KeyCenter.userName = ""
        mMute = false
        mChatMessageDataList.clear()
        mSttEndTimeMap.clear()
        mLastSpeech2TextTime = 0
        mEnableEnglishTeacher = false
        stopVoiceChat()
        AIEngine.destroy()
        mAiEngine = null
    }
}