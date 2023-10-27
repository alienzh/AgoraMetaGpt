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
import io.agora.gpt.utils.ToastUtils

class AiShareViewModel : ViewModel(), AIEngineCallback {

    private val TAG = "zhangw-" + AiShareViewModel::class.java.simpleName

    private val mHandler by lazy {
        Handler(Looper.getMainLooper())
    }

    private var mAiEngine: AIEngine? = null

    private val mAiEngineConfig: AIEngineConfig = AIEngineConfig()

    val mEventResultModel: MutableLiveData<EventResultModel> = MutableLiveData()
    val mDownloadProgress: MutableLiveData<DownloadProgressModel> = MutableLiveData()
    val mPrepareResult: MutableLiveData<Boolean> = MutableLiveData()
    val mDownloadRes: MutableLiveData<DownloadResModel> = MutableLiveData()
    val mDownloadResFinish: MutableLiveData<Boolean> = MutableLiveData()

    val mNewLineMessageModel: MutableLiveData<Triple<ChatMessageModel, Boolean, Int>> = MutableLiveData()

    private var mMute: Boolean = false

    val mChatMessageDataList = mutableListOf<ChatMessageModel>()

    private val mCostTimeMap = mutableMapOf<String, Long>()
    private var mLastSpeech2TextTime: Long = 0
    private var mCurrentRoundId: String = ""


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
            mEnableChatConversation = true
            mSpeechRecognitionFiltersLength = 3
        }
        if (mAiEngine == null) {
            mAiEngine = AIEngine.create()
        }
        mAiEngine?.initialize(mAiEngineConfig)
        mChatMessageDataList.clear()
    }

    fun setServiceVendor(){
        val serviceVendors: ServiceVendorGroup = mAiEngine?.serviceVendors ?:return
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
            if (ttsVendor.id.equals("microsoft-zh-CN-xiaoxiao-cheerful", ignoreCase = true)) {
                serviceVendor.ttsVendor = ttsVendor
                break
            }
        }
        mAiEngine?.setServiceVendor(serviceVendor)
    }

    // stt-llm-tts 同一个 roundId
    // 语音转文字的回调
    override fun onSpeech2TextResult(sid: String, result: Data<String>, isRecognizedSpeech: Boolean): HandleResult {
        Log.i(TAG, "onSpeech2TextResult sid:$sid,result:$result,isRecognizedSpeech:$isRecognizedSpeech")
        mHandler.post {
            if (mCurrentRoundId != sid) {
                mCurrentRoundId = sid
            }
            if (isRecognizedSpeech) {
                mLastSpeech2TextTime = System.currentTimeMillis()
                mCostTimeMap[sid] = System.currentTimeMillis()
                val messageModel = ChatMessageModel(
                    isAiMessage = false, sid = "", name = KeyCenter.userName ?: "", message = result.data, costTime = 0
                )
                mChatMessageDataList.add(messageModel)
                mNewLineMessageModel.value = Triple(messageModel, true, mChatMessageDataList.size - 1)
            }
        }
        return HandleResult.CONTINUE
    }

    // gpt的回答，是流式的，同一个回答，是同一个sid
    override fun onLLMResult(sid: String, answer: Data<String>): HandleResult {
        Log.i(TAG, "onLLMResult sid:$sid answer:$answer")
        mHandler.post {
            val lastIndex = mChatMessageDataList.indexOfLast { it.sid == sid }
            if (lastIndex >= 0) {
                val lastChatMessageModel: ChatMessageModel = mChatMessageDataList[lastIndex]
                lastChatMessageModel.message = lastChatMessageModel.message.plus(answer)
                mNewLineMessageModel.value = Triple(lastChatMessageModel, false, lastIndex)
            } else {
                val messageModel = ChatMessageModel(
                    isAiMessage = true,
                    sid = sid,
                    name = getAiName(),
                    message = answer.data,
                    costTime = System.currentTimeMillis() - (mCostTimeMap[sid] ?: mLastSpeech2TextTime)
                )
//                mCostTimeMap[sid]?.let { startTime ->
//                    messageModel.costTime = System.currentTimeMillis() - startTime
//                }
                if (sid != mCurrentRoundId) {
                    // 新的 roundId，不用计算耗时，AI 自动问询的消息
                    messageModel.costTime = 0
                }
                mChatMessageDataList.add(messageModel)
                mNewLineMessageModel.value = Triple(messageModel, true, mChatMessageDataList.size - 1)
            }
        }
        return HandleResult.CONTINUE
    }

    // tts
    override fun onText2SpeechResult(
        roundId: String, voice: Data<ByteArray>?, sampleRates: Int, channels: Int, bits: Int
    ): HandleResult {
//        Log.i(TAG, "onText2SpeechResult roundId:$roundId,sampleRates:$sampleRates,channels:$channels,bits:$bits")
        mHandler.post {
            val lastIndex = mChatMessageDataList.indexOfLast { it.sid == roundId }
            if (lastIndex >= 0) {
                val lastChatMessageModel: ChatMessageModel = mChatMessageDataList[lastIndex]
                lastChatMessageModel.costTime =
                    System.currentTimeMillis() - (mCostTimeMap[roundId] ?: mLastSpeech2TextTime)
                mNewLineMessageModel.value = Triple(lastChatMessageModel, false, lastIndex)
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
        mDownloadResFinish.postValue(true)
    }

    override fun onCheckDownloadResResult(totalSize: Long, fileCount: Int) {
        Log.i(TAG, "onCheckDownloadResResult totalSize:$totalSize,fileCount:$fileCount")
        if (totalSize == 0L) { // totalSize:0 目前就表示不用下载的
            mDownloadResFinish.postValue(true)
        } else {
            mDownloadRes.postValue(DownloadResModel(totalSize, fileCount))
        }

    }

    override fun onDownloadResProgress(progress: Int, index: Int, count: Int) {
        Log.i(TAG, "onDownloadResProgress progress:$progress,index:$index,count:$count")
        mDownloadProgress.postValue(DownloadProgressModel(progress, index, count))
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
                mAiEngine = null
                KeyCenter.userName = ""
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
    }

    override fun onVirtualHumanStop(code: Int, msg: String?) {
        Log.i(TAG, "onVirtualHumanStop code:$code,msg:$msg")
    }

    fun getAiName(): String {
        return mAiEngine?.currentRole?.roleName ?: MainApplication.mGlobalApplication.getString(R.string.role_foodie)
    }

    fun isEnglishTeacher(aiRole: AIRole): Boolean {
        return mAiEngineConfig.mLanguage == Language.EN_US && (aiRole.roleName == "Wendy" || aiRole.roleName == "Cindy")
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
        mAiEngine?.setRole(aiRole.roleId)
        mAiEngine?.updateConfig(mAiEngineConfig)
    }

    // 设置 texture
    fun setTexture(activity: Activity, texture: TextureView) {
        mAiEngineConfig.mTextureView = texture
        mAiEngineConfig.mActivity = activity
        mAiEngine?.updateConfig(mAiEngineConfig)
    }

    // 开启语聊
    fun startVoiceChat() {
        mAiEngine?.startVoiceChat()
    }

    // 结束语聊
    fun stopVoiceChat() {
        mAiEngine?.stopVoiceChat()
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
        when (command) {
            Constant.COMMAND_TOPIC -> {
                mAiEngine?.pushText("$command $text")
            }

            Constant.COMMAND_EVALUATE -> {
                mAiEngine?.pushText("$command")
            }

            else -> {
                Log.d(TAG, "not support command：$command")
            }
        }

    }

    fun releaseEngine() {
        AIEngine.destroy()
        mAiEngine = null
    }
}