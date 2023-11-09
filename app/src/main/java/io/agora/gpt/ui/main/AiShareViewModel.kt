package io.agora.gpt.ui.main

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.TextureView
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

    private var mMute: Boolean = false

    val mChatMessageDataList = mutableListOf<ChatMessageModel>()

    private val mSttEndTimeMap = mutableMapOf<String, Long>()
    private var mLastSpeech2TextTime: Long = 0
    private var mEnableEnglishTeacher = false
    private var mIsStartVoice: Boolean = false
    private var mEnableVirtualHuman: Boolean = false

    init {
        val currentLanguage = SPUtil.get(Constant.CURRENT_LANGUAGE, "zh") as String
        mAiEngineConfig.mLanguage = if (currentLanguage == "zh") {
            Language.ZH_CN
        } else {
            Language.EN_US
        }
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
            mSpeechRecognitionFiltersLength = 3
        }
        if (mAiEngine == null) {
            mAiEngine = AIEngine.create()
        }
        mAiEngine?.initialize(mAiEngineConfig)
        mChatMessageDataList.clear()
    }

    // ServiceVendorGroup{sttList=[STTVendor{id='xunfei', vendorName='xunfei', accountInJson='null'},
    // STTVendor{id='microsoft', vendorName='microsoft', accountInJson='null'}],
    // llmList=[LLMVendor{id='minimax-abab5.5-chat', vendorName='minimax', model='abab5.5-chat', accountInJson='null'},
    // LLMVendor{id='azureOpenai-gpt-4', vendorName='azureOpenai', model='gpt-4', accountInJson='null'},
    // LLMVendor{id='bigModel-chatglm_pro', vendorName='bigModel', model='chatglm_pro', accountInJson='null'},
    // LLMVendor{id='senseTime-nova-ptc-xl-v1', vendorName='senseTime', model='nova-ptc-xl-v1', accountInJson='null'}],
    // ttsList=[TTSVendor{id='microsoft-en-US-Jenny-cheerful', vendorName='microsoft', language='en-US', voiceName='Jenny', voiceNameValue='en-US-JennyNeural', voiceNameStyle='cheerful', accountInJson='null'},
    // TTSVendor{id='microsoft-en-US-Jenny-gentle', vendorName='microsoft', language='en-US', voiceName='Jenny', voiceNameValue='en-US-JennyNeural', voiceNameStyle='gentle', accountInJson='null'},
    // TTSVendor{id='microsoft-en-US-Davis-cheerful', vendorName='microsoft', language='en-US', voiceName='Davis', voiceNameValue='en-US-DavisNeural', voiceNameStyle='cheerful', accountInJson='null'},
    // TTSVendor{id='elevenLabs-Bella', vendorName='elevenLabs', language='', voiceName='Bella', voiceNameValue='EXAVITQu4vr4xnSDxMaL', voiceNameStyle='', accountInJson='null'}]}


    //ServiceVendorGroup{sttList=[
    // STTVendor{id='xunfei', vendorName='xunfei', accountInJson='null'},
    // STTVendor{id='microsoft', vendorName='microsoft', accountInJson='null'}],
    // llmList=[
    // LLMVendor{id='minimax-abab5.5-chat', vendorName='minimax', model='abab5.5-chat', accountInJson='null'},
    // LLMVendor{id='azureOpenai-gpt-4', vendorName='azureOpenai', model='gpt-4', accountInJson='null'},
    // LLMVendor{id='bigModel-chatglm_pro', vendorName='bigModel', model='chatglm_pro', accountInJson='null'},
    // LLMVendor{id='senseTime-nova-ptc-xl-v1', vendorName='senseTime', model='nova-ptc-xl-v1', accountInJson='null'}],
    // ttsList=[
    // TTSVendor{id='microsoft-zh-CN-xiaoxiao-cheerful', vendorName='microsoft', language='zh-CN', voiceName='晓晓(普通话)', voiceNameValue='zh-CN-XiaoxiaoNeural', voiceNameStyle='cheerful', accountInJson='null'},
    // TTSVendor{id='microsoft-zh-CN-xiaoyi-gentle', vendorName='microsoft', language='zh-CN', voiceName='晓伊(普通话)', voiceNameValue='zh-CN-XiaoyiNeural', voiceNameStyle='gentle', accountInJson='null'},
    // TTSVendor{id='microsoft-zh-CN-yunxi-cheerful', vendorName='microsoft', language='zh-CN', voiceName='云希(普通话)', voiceNameValue='zh-CN-YunxiNeural', voiceNameStyle='cheerful', accountInJson='null'},
    // TTSVendor{id='xunfei-zh-CN-xiaoyan', vendorName='xunfei', language='zh-CN', voiceName='晓燕(普通话)', voiceNameValue='xiaoyan', voiceNameStyle='', accountInJson='null'},
    // TTSVendor{id='xunfei-zh-CN-xiaoyu', vendorName='xunfei', language='zh-CN', voiceName='xiaoyu', voiceNameValue='xiaoyu', voiceNameStyle='', accountInJson='null'},
    // TTSVendor{id='elevenLabs-Bella', vendorName='elevenLabs', language='', voiceName='Bella', voiceNameValue='EXAVITQu4vr4xnSDxMaL', voiceNameStyle='', accountInJson='null'}]}
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
            if (currentLanguage() == Language.EN_US) {
                if (llmVendor.id.equals("azureOpenai-gpt-4", ignoreCase = true)) {
                    serviceVendor.llmVendor = llmVendor
                    break
                }
            } else {
                if (llmVendor.id.equals("minimax-abab5.5-chat", ignoreCase = true)) {
                    serviceVendor.llmVendor = llmVendor
                    break
                }
            }

        }
        for (ttsVendor in serviceVendors.ttsList) {
            if (currentLanguage() == Language.EN_US) {
                if (aiRole.roleId.equals("Wendy-en-US", true) &&
                    ttsVendor.id.equals("microsoft-en-US-Jenny-cheerful", true)
                ) {
                    serviceVendor.ttsVendor = ttsVendor
                    break
                } else if (aiRole.roleId.equals("Cindy-en-US", true)
                    && ttsVendor.id.equals("microsoft-en-US-Jenny-gentle", ignoreCase = true)
                ) {
                    serviceVendor.ttsVendor = ttsVendor
                    break
                } else if (aiRole.roleId.equals("yunibobo-en-US", true) &&
                    ttsVendor.id.equals("microsoft-en-US-Davis-cheerful", true)
                ) {
                    serviceVendor.ttsVendor = ttsVendor
                    break
                }
            } else {
                if (aiRole.roleId.equals("yunibobo-zh-CN", true) &&
                    ttsVendor.id.equals("microsoft-zh-CN-xiaoxiao-cheerful", true)) {
                    serviceVendor.ttsVendor = ttsVendor
                    break
                }else if (aiRole.roleId.equals("jingxiang-zh-CN", true) &&
                    ttsVendor.id.equals("microsoft-zh-CN-xiaoyi-gentle", true)) {
                    serviceVendor.ttsVendor = ttsVendor
                    break
                }
            }

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
            if (isRecognizedSpeech && tempSid != null) {
                mLastSpeech2TextTime = System.currentTimeMillis()
                mSttEndTimeMap[tempSid] = System.currentTimeMillis()
                val messageModel = ChatMessageModel(
                    isAiMessage = false,
                    sid = tempSid,
                    name = KeyCenter.userName ?: "",
                    message = result.data,
                    costTime = 0
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
            Log.i(TAG, "onLLMResult tempSid:$tempSid answer:$answer")
        }
        mHandler.post {
            val oldChatMessageModel = mChatMessageDataList.find { it.sid == tempSid && it.isAiMessage }
            if (oldChatMessageModel != null) {
                val index = mChatMessageDataList.indexOf(oldChatMessageModel)
                oldChatMessageModel.message = oldChatMessageModel.message.plus(answer.data)
                mNewLineMessageModel.value = Triple(oldChatMessageModel, false, index)
            } else {
                val messageModel = ChatMessageModel(
                    isAiMessage = true,
                    sid = tempSid!!,
                    name = getAiName(),
                    message = answer.data,
//                    costTime = System.currentTimeMillis() - (mSttEndTimeMap[sid] ?: mLastSpeech2TextTime)
                )
                // stt 识别完没有 sid ，一个新的sid，默认就是 ai 问询语句
                if (!mSttEndTimeMap.containsKey(tempSid)) {
                    mSttEndTimeMap[tempSid] = System.currentTimeMillis()
                }
                mChatMessageDataList.add(messageModel)
                mNewLineMessageModel.value = Triple(messageModel, true, mChatMessageDataList.size - 1)
            }
        }
        return HandleResult.CONTINUE
    }

    // tts
    override fun onText2SpeechResult(
        roundId: String?, voice: Data<ByteArray>?, sampleRates: Int, channels: Int, bits: Int
    ): HandleResult {

        var tempSid = roundId
        if (tempSid.isNullOrEmpty()) {
            tempSid = KeyCenter.getRandomString(20)
            Log.i(TAG, "onText2SpeechResult tempSid:$tempSid,sampleRates:$sampleRates,channels:$channels,bits:$bits")
        }
        mHandler.post {
            val oldChatMessageModel = mChatMessageDataList.find { it.sid == tempSid && it.isAiMessage }
            if (oldChatMessageModel != null && oldChatMessageModel.costTime == 0L) {
                val index = mChatMessageDataList.indexOf(oldChatMessageModel)
                oldChatMessageModel.costTime =
                    System.currentTimeMillis() - (mSttEndTimeMap[tempSid] ?: mLastSpeech2TextTime)
                mNewLineMessageModel.value = Triple(oldChatMessageModel, false, index)
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
            if (event == ServiceEvent.INITIALIZE) {
                if (code == ServiceCode.SUCCESS) {
                    mAiEngine?.checkDownloadRes()
                } else {
                    ToastUtils.showToast("AIGC INITIALIZE error:$code,msg:$msg")
                }
            } else if (event == ServiceEvent.DESTROY && code == ServiceCode.SUCCESS) {
            }
            mEventResultModel.value = EventResultModel(event, code)
        }
    }

    override fun onPrepareResult(code: Int, msg: String?) {
        Log.i(TAG, "onPrepareResult code:$code,msg:$msg")
        mHandler.post {
            mPrepareResult.value = code == ServiceCode.SUCCESS.code
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
        return mAiEngine?.currentRole?.roleName ?: MainApplication.mGlobalApplication.getString(R.string.role_foodie)
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
        val roleAvatars = if (mAiEngineConfig.mLanguage == Language.EN_US) {
            KeyCenter.mEnRoleAvatars
        } else {
            KeyCenter.mCnRoleAvatars
        }
        val usableAiRoles = mutableListOf<AIRole>()
        val sdkAiRoleMap = sdkAiRoles.associateBy { it.roleId }
        for (i in roleAvatars.indices) {
            val roleAvatar = roleAvatars[i]
            sdkAiRoleMap[roleAvatar.roleId]?.let { aiRole ->
                usableAiRoles.add(aiRole)
            }
        }
        return usableAiRoles
    }

    // 设置SDK使用的AIRol
    fun setAvatarModel(aiRole: AIRole) {
        val roleAvatars = if (mAiEngineConfig.mLanguage == Language.EN_US) {
            KeyCenter.mEnRoleAvatars
        } else {
            KeyCenter.mCnRoleAvatars
        }
        val avatarModel = AvatarModel().apply {
            for (i in roleAvatars.indices) {
                val roleAvatar = roleAvatars[i]
                if (aiRole.getRoleId() == roleAvatar.roleId) {
                    name = roleAvatar.avatar
                    break
                }
            }
        }
        if (aiRole.gender == Constants.GENDER_MALE) {
            avatarModel.bgFilePath = "bg_ai_male.png"
        } else {
            avatarModel.bgFilePath = "bg_ai_female.png"
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
//        mAiEngine?.updateConfig(mAiEngineConfig)
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
    fun switchLanguage(callback: (Language) -> Unit) {
        mAiEngineConfig.mLanguage = if (mAiEngineConfig.mLanguage == Language.ZH_CN) {
            Language.EN_US
        } else {
            Language.ZH_CN
        }
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