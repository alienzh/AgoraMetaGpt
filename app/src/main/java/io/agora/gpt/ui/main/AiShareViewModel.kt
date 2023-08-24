package io.agora.gpt.ui.main

import android.app.Activity
import android.util.Log
import android.view.TextureView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.agora.ai.sdk.AIEngine
import io.agora.ai.sdk.AIEngineAction
import io.agora.ai.sdk.AIEngineCallback
import io.agora.ai.sdk.AIEngineCode
import io.agora.ai.sdk.AIRole
import io.agora.ai.sdk.Constants
import io.agora.gpt.MainApplication
import io.agora.gpt.R
import io.agora.gpt.utils.Constant
import io.agora.gpt.utils.KeyCenter

class AiShareViewModel : ViewModel(), AIEngineCallback {

    companion object {
        private val TAG = "AiShareViewModel"
    }

    private var aiEngine: AIEngine? = null

    val downloadProgress: MutableLiveData<DownloadProgressModel> = MutableLiveData()
    val actionResultModel: MutableLiveData<ActionResultModel> = MutableLiveData()
    val newLineMessageModel: MutableLiveData<Pair<ChatMessageModel, Boolean>> = MutableLiveData()

    private var mute: Boolean = false
    private var voiceChange: Boolean = false
    private var language: String = Constants.LANG_ZH_CN

    val mChatMessageDataList = mutableListOf<ChatMessageModel>()

    private var onSpeechRecognitionTime: Long = 0

    fun initAiEngine(activity: Activity) {
        if (aiEngine == null) {
            aiEngine = AIEngine.Builder(MainApplication.mGlobalApplication.applicationContext)
                .callback(this)
                .enableLog(true)
                .initRtc(
                    KeyCenter.getUserUid(), KeyCenter.getRoomName(), KeyCenter.APP_ID,
                    KeyCenter.getRtcToken(KeyCenter.getRoomName(), KeyCenter.getUserUid()),
                    KeyCenter.getRtmToken(KeyCenter.getUserUid())
                )
                .activity(activity)
                .userName(KeyCenter.getUserName())
                .language(Constants.LANG_ZH_CN)
                .enableChatConversation(false)
                .speechRecognitionFiltersLength(3)
                .autoDownloadResource(false)
                .build()
        }
    }

    override fun onDownloadResProgress(progress: Int, index: Int, count: Int) {
        Log.i(TAG, "onDownloadResProgress: $progress $index $count")
        downloadProgress.postValue(DownloadProgressModel(progress, index, count))
    }

    override fun onActionResult(vcAction: AIEngineAction, vcEngineCode: AIEngineCode, extraInfo: String?) {
        Log.i(TAG, "onActionResult: $vcAction $vcEngineCode $extraInfo")
        if (AIEngineAction.RELEASED == vcAction && AIEngineCode.SUCCESS == vcEngineCode) {
            // released 后需要重新创建
            aiEngine = null
            KeyCenter.setUserName("")
        }
        actionResultModel.postValue(ActionResultModel(vcAction, vcEngineCode, extraInfo))
    }

    // 语音转文字的回调
    override fun onSpeechRecognitionResults(result: String) {
        onSpeechRecognitionTime = System.currentTimeMillis()
        Log.i(TAG, "onSpeechRecognitionResults: $result")
        val messageModel = ChatMessageModel(
            isAiMessage = false,
            sid = "",
            name = KeyCenter.getUserName(),
            message = result,
            costTime = 0
        )
        Log.i(TAG, "onSpeechRecognitionResults: $messageModel")
        mChatMessageDataList.add(messageModel)
        newLineMessageModel.postValue(Pair(messageModel, true))
    }

    // gpt的回答，是流式的，同一个回答，是同一个sid
    override fun onAiChatAnswer(sid: String, answer: String) {
        Log.i(TAG, "onAiChatAnswer: $sid $answer")
        val lastChatMessageModel = mChatMessageDataList.getOrNull(mChatMessageDataList.size - 1)
        if (lastChatMessageModel?.sid == sid) {
            lastChatMessageModel.message = lastChatMessageModel.message.plus(answer)
            lastChatMessageModel.costTime = System.currentTimeMillis() - onSpeechRecognitionTime
            Log.i(TAG, "onAiChatAnswer: $lastChatMessageModel")
            newLineMessageModel.postValue(Pair(lastChatMessageModel, false))
        } else {
            val messageModel = ChatMessageModel(
                isAiMessage = true,
                sid = sid,
                name = getAvatarName(),
                message = answer,
                costTime = System.currentTimeMillis() - onSpeechRecognitionTime
            )
            Log.i(TAG, "onAiChatAnswer: $messageModel")
            mChatMessageDataList.add(messageModel)
            newLineMessageModel.postValue(Pair(messageModel, true))
        }
    }

    private fun getAvatarName(): String {
        val avatarName = when (getAiRoleName()) {
            Constant.ROLE_FOODIE -> MainApplication.mGlobalApplication.getString(R.string.role_foodie)
            Constant.ROLE_LATTE_LOVE -> MainApplication.mGlobalApplication.getString(R.string.role_latte_love)
            else -> MainApplication.mGlobalApplication.getString(R.string.role_foodie)
        }
        return avatarName
    }

    override fun onStartPlayAiChatAnswer() {
        Log.i(TAG, "onStartPlayAiChatAnswer ${System.currentTimeMillis()}")
    }

    fun cancelDownloadRes() {
        aiEngine?.cancelDownloadRes()
    }

    fun prepare() {
        aiEngine?.prepare()
    }

    fun downloadRes() {
        aiEngine?.downloadRes()
    }

    fun checkDownloadRes() {
        aiEngine?.checkDownloadRes()
    }

    // 获取SDK预制的所有AIRole数组
    fun getAllAiRoles(): Array<AIRole> {
        return aiEngine?.allAiRoles?.copyOfRange(0, 2) ?: emptyArray()
    }

    // 设置SDK使用的AIRol
    fun setAiRole(aiRole: AIRole) {
        if (aiRole.gender == Constants.GENDER_MALE) {
            aiRole.avatar.bgFilePath = "bg_ai_male.png"
        } else {
            aiRole.avatar.bgFilePath = "bg_ai_female.png"
        }
        aiEngine?.setAiRole(aiRole)
    }

    fun getAiRoleName(): String {
        return aiEngine?.currentAiRole?.aiRoleId ?: ""
    }

    // 设置 texture
    fun setTexture(activity: Activity, texture: TextureView) {
        aiEngine?.setActivity(activity)
        aiEngine?.setTexture(texture)
    }

    // 开启语聊
    fun startVoiceChat() {
        aiEngine?.startVoiceChat()
    }

    // 结束语聊
    fun stopVoiceChat() {
        aiEngine?.stopVoiceChat()
    }

    // 开启/关闭 变声
    fun enableVoiceChange(callback: (Boolean) -> Unit) {
        voiceChange = !voiceChange
        aiEngine?.enableVoiceChange(voiceChange)
        callback.invoke(voiceChange)
    }

    // 静音/取消静音
    fun mute(callback: (Boolean) -> Unit) {
        mute = !mute
        aiEngine?.mute(mute)
        callback.invoke(mute)
    }

    // 设置AI语言环境,此步骤后需要重新prepare。
    fun switchLanguage(callback: (String) -> Unit) {
        language = if (language == Constants.LANG_ZH_CN) {
            Constants.LANG_EN_US
        } else {
            Constants.LANG_ZH_CN
        }
        aiEngine?.setLanguage(language)
        callback.invoke(language)
    }

    fun releaseEngine() {
        aiEngine?.releaseEngine()
    }


}