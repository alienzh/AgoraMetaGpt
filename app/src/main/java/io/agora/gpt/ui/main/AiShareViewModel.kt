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
import io.agora.aigc.sdk.constants.NoiseEnvironment
import io.agora.aigc.sdk.constants.ServiceCode
import io.agora.aigc.sdk.constants.ServiceEvent
import io.agora.aigc.sdk.constants.SpeechRecognitionCompletenessLevel
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
import java.lang.StringBuilder

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

    val mUserSttContent: MutableLiveData<String> = SingleLiveEvent()

    val mAIEngineInit: MutableLiveData<Boolean> = SingleLiveEvent()

    // ai 游戏中用户说话一轮合并一整句
    private val mUserSttContentList = mutableListOf<ChatMessageModel>()

    // 禁音按钮
    private var mMute: Boolean = false

    // 长按说话按钮
    private var mLongStting: Boolean = false

    // 第一次进入房间
    private var mFirstEnterRoom: Boolean = true

    val mChatMessageDataList = mutableListOf<ChatMessageModel>()

    private val mSttEndTimeMap = mutableMapOf<String, Long>()
    private var mLastSpeech2TextTime: Long = 0
    private var mEnableEnglishTeacher = false
    private var mIsStartVoice: Boolean = false
    private var mEnableVirtualHuman: Boolean = false
    var mCurrentScene: String = Constant.Scene_AI_Partner

    // ai 游戏保存上次一角色
    var mLastAiRole: AIRole? = null

    init {
        val currentLanguage = SPUtil.get(Constant.CURRENT_LANGUAGE, "zh") as String
        mAiEngineConfig.mLanguage = if (currentLanguage == "zh") {
            Language.ZH_CN
        } else {
            Language.EN_US
        }
    }

    fun isAiGame(): Boolean {
        return mCurrentScene == Constant.Scene_AI_Game
    }

    fun isFirstEnterRoom(): Boolean = mFirstEnterRoom

    fun setFirstEnterRoom(isFirstEnterRoom: Boolean) {
        this.mFirstEnterRoom = isFirstEnterRoom
    }

    fun currentLanguage(): Language {
        return mAiEngineConfig.mLanguage
    }

    fun initAiEngine() {
        val userId = KeyCenter.mUserUid
        val roomName = io.agora.aigc.sdk.utils.Utils.getCurrentDateStr("yyyyMMddHHmmss") +
                io.agora.aigc.sdk.utils.Utils.getRandomString(2)
        Log.d(TAG, "roomName:$roomName")
        val virtualHumanUid = KeyCenter.mVirtualHumanUid
        mAiEngineConfig.apply {
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

    //ServiceVendorGroup{
    // sttList= [
    // STTVendor{id='xunfei', vendorName='xunfei', accountInJson='null'},
    // STTVendor{id='microsoft', vendorName='microsoft', accountInJson='null'}
    // ],
    // llmList=[
    // LLMVendor{id='minimax-abab5.5-chat', vendorName='minimax', model='abab5.5-chat', accountInJson='null'},
    // LLMVendor{id='azureOpenai-gpt-4', vendorName='azureOpenai', model='gpt-4-turbo', accountInJson='null'},
    // LLMVendor{id='azureOpenai-gpt-35-turbo-16k', vendorName='azureOpenai', model='gpt-35-turbo-16k', accountInJson='null'}
    // ],
    // ttsList=[
    // TTSVendor{id='microsoft-zh-CN-xiaoxiao-cheerful', vendorName='microsoft', language='zh-CN', voiceName='晓晓(普通话)', voiceNameValue='zh-CN-XiaoxiaoNeural', voiceNameStyle='cheerful', accountInJson='null'},
    // TTSVendor{id='microsoft-zh-CN-xiaoyi-gentle', vendorName='microsoft', language='zh-CN', voiceName='晓伊(普通话)', voiceNameValue='zh-CN-XiaoyiNeural', voiceNameStyle='gentle', accountInJson='null'},
    // TTSVendor{id='microsoft-zh-CN-yunxi-cheerful', vendorName='microsoft', language='zh-CN', voiceName='云希(普通话)', voiceNameValue='zh-CN-YunxiNeural', voiceNameStyle='cheerful', accountInJson='null'},
    // TTSVendor{id='elevenLabs-Matilda', vendorName='elevenLabs', language='', voiceName='Matilda', voiceNameValue='XrExE9yKIg1WjnnlVkGX', voiceNameStyle='', accountInJson='null'},
    // TTSVendor{id='volcEngine-girl', vendorName='volcEngine', language='zh-CN', voiceName='girl', voiceNameValue='BV406_streaming', voiceNameStyle='', accountInJson='null'},
    // TTSVendor{id='volcEngine-boy', vendorName='volcEngine', language='zh-CN', voiceName='boy', voiceNameValue='BV705_streaming', voiceNameStyle='', accountInJson='null'}]}
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
            if (isAiGame()) {
                if (llmVendor.id.equals("azureOpenai-gpt-4", ignoreCase = true)) {
                    serviceVendor.llmVendor = llmVendor
                    break
                }
            } else {
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
        }
        for (ttsVendor in serviceVendors.ttsList) {
            if (currentLanguage() == Language.EN_US) {
                // 英文场景统一用 elevenLabs-Matilda
                if (ttsVendor.id.equals("elevenLabs-Matilda", true)) {
                    serviceVendor.ttsVendor = ttsVendor
                    break
                }
            } else {
                if (aiRole.roleId.equals(Constant.CN_Role_ID_yunibobo, true) &&
                    ttsVendor.id.equals("volcEngine-girl", true)
                ) {
                    serviceVendor.ttsVendor = ttsVendor
                    break
                } else if (aiRole.roleId.equals(Constant.CN_Role_ID_jingxiang, true) &&
                    ttsVendor.id.equals("microsoft-zh-CN-xiaoxiao-cheerful", true)
                ) {
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
            if (isRecognizedSpeech) {
                mLastSpeech2TextTime = System.currentTimeMillis()
                mSttEndTimeMap[tempSid] = System.currentTimeMillis()
            }
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
                    costTime = 0
                )
                // ai 游戏没有发送不用展示在历史消息中
                if (isAiGame()) {
                    val exitsMessageModel = mUserSttContentList.find { it.sid == messageModel.sid }
                    if (exitsMessageModel != null) {
                        exitsMessageModel.message = messageModel.message
                    } else {
                        mUserSttContentList.add(messageModel)
                    }
                    val sb = StringBuilder()
                    mUserSttContentList.map { it.message }.forEach {
                        sb.append(it)
                    }
                    mUserSttContent.value = sb.toString()
                } else {
                    mChatMessageDataList.add(messageModel)
                    mNewLineMessageModel.value = Triple(messageModel, true, mChatMessageDataList.size - 1)
                }
            }
        }
        return if (isAiGame()) HandleResult.DISCARD else HandleResult.CONTINUE
    }

    // gpt的回答，是流式的，同一个回答，是同一个sid
    override fun onLLMResult(sid: String?, answer: Data<String>): HandleResult {
        Log.i(TAG, "onLLMResult sid:$sid answer:$answer length ${sid?.length}")
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
                    sid = tempSid,
                    name = currentRole()?.roleName
                        ?: MainApplication.mGlobalApplication.getString(R.string.role_foodie),
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
        if (tempSid.length == 64 && tempSid.startsWith("00000000000000000000000000000000")) {
            return HandleResult.DISCARD
        } else {
            return HandleResult.CONTINUE
        }
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
                    mAIEngineInit.postValue(true)
                    Log.d(TAG, "AIGC INITIALIZE SUCCESS")
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

    fun currentRole(): AIRole? {
        return mAiEngine?.currentRole
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
        Log.d(TAG, "sdkAiRoles：$sdkAiRoles")
        if (isAiGame()) {
            return sdkAiRoles.filter { it.roleId.startsWith("game") }
        }

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
    fun setAvatarModel(aiRole: AIRole, needApply: Boolean = true) {
        val avatarName = KeyCenter.getAvatarName(aiRole)
        val avatarModel = AvatarModel().apply {
            name = avatarName
        }
        val avatarDrawableStr = KeyCenter.getAvatarDrawableStr(aiRole)
        avatarModel.bgFilePath = Utils.getCacheFilePath("$avatarDrawableStr.png")
        mAiEngineConfig.mAvatarModel = avatarModel
        mAiEngine?.updateConfig(mAiEngineConfig)
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
        // 不开麦
        if (isAiGame()) {
            mMute = true
            mAiEngine?.mute(true)
        }
        mIsStartVoice = true
        mAiEngine?.startVoiceChat()
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
        // 变声在基本 sdk 外，updateConfig 不会重新初始化
        callback.invoke(voiceChange)
    }

    // 静音/取消静音
    fun mute(callback: (Boolean) -> Unit) {
        mMute = !mMute
        mAiEngine?.mute(mMute)
        callback.invoke(mMute)
    }

    fun startUserSpeak(callback: () -> Unit) {
        mUserSttContentList.clear()
        mLongStting = true
        mMute = false
        mAiEngine?.mute(false)
        callback.invoke()
    }

    // 设置AI语言环境,只记录语言
    fun switchLanguage(callback: (Language) -> Unit) {
        mAiEngineConfig.mLanguage = if (mAiEngineConfig.mLanguage == Language.ZH_CN) {
            Language.EN_US
        } else {
            Language.ZH_CN
        }
        if (mAiEngineConfig.mLanguage == Language.EN_US) {
            mCurrentScene = Constant.Scene_AI_Partner
        }
        mAiEngine?.updateConfig(mAiEngineConfig)
        callback.invoke(mAiEngineConfig.mLanguage)
    }

    // 发送的数据加入
    fun checkAddUserSttContent() {
        if (mUserSttContentList.isEmpty()) return
        val lastMessageModel = mUserSttContentList.last()
        val chatMessageModel = ChatMessageModel(
            isAiMessage = false,
            sid = lastMessageModel.sid,
            name = lastMessageModel.name,
            message = "",
            costTime = 0
        )
        mUserSttContentList.forEach { messageModel ->
            chatMessageModel.message = chatMessageModel.message.plus(messageModel.message)
        }
        mChatMessageDataList.add(chatMessageModel)
        mNewLineMessageModel.value = Triple(chatMessageModel, true, mChatMessageDataList.size - 1)
        mUserSttContentList.clear()
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
        mLastAiRole = if (isAiGame()) {
            currentRole()
        } else {
            null
        }
        KeyCenter.mUserName = ""
        mMute = false
        mLongStting = false
        mFirstEnterRoom = true
        mChatMessageDataList.clear()
        mSttEndTimeMap.clear()
        mLastSpeech2TextTime = 0
        mEnableEnglishTeacher = false
        stopVoiceChat()
        AIEngine.destroy()
        mAiEngine = null

    }
}