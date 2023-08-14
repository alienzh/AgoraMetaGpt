package io.agora.gpt.ui.aiPartner

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.ViewModel
import io.agora.gpt.BuildConfig
import io.agora.gpt.MainApplication
import io.agora.gpt.R
import io.agora.gpt.agora_server.AgoraGptServerManager
import io.agora.gpt.chat.ChatRobotManager
import io.agora.gpt.context.GameContext
import io.agora.gpt.context.MetaContext
import io.agora.gpt.inf.ChatCallback
import io.agora.gpt.inf.SttCallback
import io.agora.gpt.inf.TtsCallback
import io.agora.gpt.models.UserInfo
import io.agora.gpt.models.chat.ChatBotRole
import io.agora.gpt.stt.SttRobotManager
import io.agora.gpt.tts.TtsRobotManager
import io.agora.gpt.utils.Config
import io.agora.gpt.utils.Constants
import io.agora.gpt.utils.EncryptUtil
import io.agora.gpt.utils.ErrorCode
import io.agora.gpt.utils.KeyCenter
import io.agora.gpt.utils.Utils
import io.agora.gpt.voice.DubbingVoiceEngine
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.Random
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class AiPartnerViewModel : ViewModel(), ChatCallback, SttCallback, TtsCallback {

    companion object {
        private val TAG = Constants.TAG + "-" + AiPartnerViewModel::class.java.simpleName
        private const val MESSAGE_REQUEST_CHAT_KEY_INFO = 1
    }


    private val mHandler: Handler = object : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                MESSAGE_REQUEST_CHAT_KEY_INFO -> {
                    if (mChatRobotManager != null) {
                        mRequestChatKeyInfoIndex++
                        mChatRobotManager!!.requestChatKeyInfo()
                    }
                    sendEmptyMessageDelayed(MESSAGE_REQUEST_CHAT_KEY_INFO, (60 * 1000).toLong())
                }

                else -> {}
            }
        }
    }

    private var mTempPcmFilePath: String? = null

    private var mTtsInputStream: InputStream? = null
    private var mTtsStartTime: Long = 0
    private var mSttStartTime: Long = 0
    private var mChatStartTime: Long = 0
    private var mChatIdleStartTime: Long = 0


    private var mJoinChannelSuccess = false
    private var mStreamId = -1

    private var mExecutorService: ExecutorService = ThreadPoolExecutor(
        0, Int.MAX_VALUE,
        60L, TimeUnit.SECONDS,
        SynchronousQueue(), Executors.defaultThreadFactory(), ThreadPoolExecutor.AbortPolicy()
    )

    // 语音转文字
    private val mSttResults: StringBuilder = java.lang.StringBuilder()

    // gpt 回答
    private val mChatAnswerPending: StringBuilder = java.lang.StringBuilder()

    private var mIsMute = false
    private var mFirstRecordAudio = false
    private var mIsSpeaking = false
    private var mCancelRequest = false
    private var mGptResponseCount = 0
    private var mGptRequestStart = false
    private var mRequestChatKeyInfoIndex = 0

    private var mChatRobotManager: ChatRobotManager? = null

    private var mTtsRobotManager: TtsRobotManager? = null
    private var mSttRobotManager: SttRobotManager? = null

    private var mAgoraGptServerManager: AgoraGptServerManager? = null

    private var mDubbingEngine: DubbingVoiceEngine? = null

    private var mDubbingInitSuccess = false

    private var mVoiceChangeEnable = false
    fun initData() {
        mTempPcmFilePath = MainApplication.mGlobalApplication.externalCacheDir!!.path + "/temp/tempTts.pcm"
        val tempTtsFile = File(mTempPcmFilePath)
        if (!tempTtsFile.exists()) {
            try {
                File(tempTtsFile.parent).mkdirs()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (null == mChatRobotManager) {
            mChatRobotManager = ChatRobotManager(MainApplication.mGlobalApplication)
            mChatRobotManager!!.setChatCallback(this)
        }
        mChatRobotManager?.setChatRobotPlatformIndex(Constants.AI_PLATFORM_MINIMAX_CHAT_COMPLETION_PRO_55)
        if (null == mTtsRobotManager) {
            mTtsRobotManager = TtsRobotManager()
            mTtsRobotManager!!.setTtsCallback(this)
        }
        mTtsRobotManager?.init(MainApplication.mGlobalApplication, mTempPcmFilePath)
        if (null == mSttRobotManager) {
            mSttRobotManager = SttRobotManager()
            mSttRobotManager!!.setSttCallback(this)
        }
        mSttRobotManager!!.setAiSttPlatformIndex(Constants.STT_PLATFORM_XF_IST)
        if (Config.ENABLE_AGORA_GPT_SERVER) {
            if (null == mAgoraGptServerManager) {
                mAgoraGptServerManager = AgoraGptServerManager()
                mAgoraGptServerManager!!.init(
                    mTempPcmFilePath,
                    UserInfo(KeyCenter.getAiUid(), MetaContext.getInstance().userName)
                )
            }
        }

        mSttResults.clear()
        mChatAnswerPending.clear()
        mIsSpeaking = false
        mIsMute = false
        mSttStartTime = 0
        mChatIdleStartTime = 0
        mCancelRequest = true
        mGptResponseCount = 0
        mGptRequestStart = true
        mRequestChatKeyInfoIndex = 0

        val chatTipMessageArray =
            MainApplication.mGlobalApplication.applicationContext.resources.getStringArray(R.array.chat_tip_message_array)
        mTtsRobotManager?.setChatTipMessages(chatTipMessageArray)
        mTtsRobotManager?.setAiTtsPlatformIndex(Constants.TTS_PLATFORM_MS)
    }

    fun isSpeaking(): Boolean = mIsSpeaking

    fun startCalling() {
        if (mIsSpeaking) {
            Log.e(TAG, "calling already...")
            return
        }
        mIsSpeaking = true
        mFirstRecordAudio = true
        mSttResults.delete(0, mSttResults.length)
        mTtsRobotManager?.setIsSpeaking(mIsSpeaking)
        MetaContext.getInstance().updateRoleSpeak(true)
        mHandler.sendEmptyMessageDelayed(MESSAGE_REQUEST_CHAT_KEY_INFO, (60 * 1000).toLong())
    }

    fun hangUp() {
        if (!mIsSpeaking) {
            Log.e(TAG, "hang up already...")
            return
        }
        mIsSpeaking = false
        if (Config.ENABLE_AGORA_GPT_SERVER) {
            mAgoraGptServerManager?.sendPcmData(null)
        } else {
            mSttRobotManager?.requestStt(null)
        }
        MetaContext.getInstance().updateRoleSpeak(false)
        mTtsRobotManager?.clearData()
    }

    fun mute(callback: ((isSpeaking: Boolean) -> Unit)) {
        if (mIsMute) {
            mIsMute = false
            MetaContext.getInstance().enableLocalAudio(true)
        } else {
            mIsMute = true
            MetaContext.getInstance().enableLocalAudio(false)
        }
        callback.invoke(mIsMute)
    }

    fun setChatBotRole(activity: Activity, chatBotRole: ChatBotRole) {
        if (chatBotRole.chatBotName == "芋泥啵啵") {
            // 芋泥啵啵改成大饼 ai 变声
            mVoiceChangeEnable = true
            initDubbingEngine(activity)
        }else{
            mVoiceChangeEnable = false
            mDubbingEngine?.let {
                it.stop()
                it.release()
            }
        }
        mChatRobotManager?.setChatBotRole(chatBotRole)
        mChatRobotManager?.clearChatMessage()
        mTtsRobotManager?.setChatBotRole(chatBotRole)
    }

    fun exit() {
        mIsSpeaking = false
        mSttRobotManager?.close()
        mTtsRobotManager?.close()
        mChatRobotManager?.close()
        if (Config.ENABLE_META_VOICE_DRIVER) {
            MetaContext.getInstance().leaveScene()
        } else {
            MetaContext.getInstance().leaveRtcChannel()
        }
        mHandler.removeMessages(MESSAGE_REQUEST_CHAT_KEY_INFO)
        mHandler.removeCallbacksAndMessages(null)

        mDubbingEngine?.stop()
        mDubbingEngine?.release()
        mDubbingInitSuccess = false
    }

    fun onJoinSuccess(streamId: Int) {
        this.mStreamId = streamId
        this.mJoinChannelSuccess = true
    }

    fun onDestroy() {
        mHandler.removeCallbacksAndMessages(null)
    }

    override fun updateChatHistoryInfo(message: String?) {
        super.updateChatHistoryInfo(message)
        Log.d(TAG, "updateChatHistoryInfo $message")
    }

    override fun onChatKeyInfoUpdate(text: String?) {
        super.onChatKeyInfoUpdate(text)
        Log.d(
            TAG,
            "第" + mRequestChatKeyInfoIndex + "次请求用户关键信息" + "\n" +
                    "===============" + "\n" +
                    text +
                    "\n" +
                    "==============="
        )
    }

    override fun onChatRequestStart(text: String?) {
        super.onChatRequestStart(text)
        Log.d(TAG, "onChatRequestStart $text")
        mChatStartTime = System.currentTimeMillis()
        mGptRequestStart = true
        mGptResponseCount++
        mChatAnswerPending.delete(0, mChatAnswerPending.length)
    }

    override fun onChatAnswer(code: Int, answer: String?) {
        super.onChatAnswer(code, answer)
        if (!mIsSpeaking) return
        val costTime = System.currentTimeMillis() - mChatStartTime
        runOnUiThread {
            if (ErrorCode.ERROR_NONE == code) {
                if (TextUtils.isEmpty(answer)) return@runOnUiThread
                mChatRobotManager?.appendChatMessage(Constants.GPT_ROLE_ASSISTANT, answer)
                mChatAnswerPending.append(answer)
                Log.d(TAG, "GPT回答(" + costTime + "ms):" + mChatAnswerPending.toString())
                if (mCancelRequest) {
                    mCancelRequest = false
                    mTtsRobotManager?.cancelTtsRequest(false)

                    if (mVoiceChangeEnable) {
                        mDubbingEngine?.stop()
                    }
                }
                if (Config.ENABLE_SHARE_CHAT) {
                    var helloAnswer = answer!!
                    if (mGptRequestStart && mGptResponseCount % Constants.MAX_COUNT_GPT_RESPONSE_HELLO == 0) {
                        mGptRequestStart = false
                        mGptResponseCount = 0
                        if (null != GameContext.getInstance().gptResponseHello && GameContext.getInstance().gptResponseHello.isNotEmpty()) {
                            val index = Random().nextInt(GameContext.getInstance().gptResponseHello.size)
                            helloAnswer = GameContext.getInstance().gptResponseHello[index].replace(
                                Constants.ASSETS_REPLACE_GPT_RESPONSE_HELLO_USERNAME_LABEL,
                                GameContext.getInstance().userName
                            ) + helloAnswer
                        }
                    }
                    mTtsRobotManager?.requestTts(helloAnswer)
                } else {
//                    handleChatGptAnswer(answer)
                }
            } else {
                mChatRobotManager?.deleteLastChatMessage()
                if (ErrorCode.ERROR_CHAT_LENGTH_LIMIT == code) {
                    Log.d(TAG, "GPT回答长度限制截止")
                } else {
                    if (answer == "timeout") {
                        Log.d(TAG, "GPT请求超时(" + costTime + "ms)")
                        mChatRobotManager?.requestChat()
                    } else {
                        Log.d(TAG, "GPT请求错误:$code,$answer")
                    }
                }
            }
        }
    }

    override fun onChatRequestFinish() {
        super.onChatRequestFinish()
        mTtsRobotManager?.requestTtsFinish()
    }

    override fun onSttResult(text: String, isFinish: Boolean) {
        Log.d(TAG, "onSttResult text:$text isFinish:$isFinish")
        if (isFinish) {
            mSttRobotManager?.close()
            mTtsRobotManager?.setIsSpeaking(false)
        }
        if (!mIsSpeaking) return
        if (text.length <= Constants.STT_FILTER_NUMBER) {
            Log.d(TAG, "过滤短句:$text")
            return
        }
        val coastTime: Long = System.currentTimeMillis() - mSttStartTime
        Log.d(TAG, "(识别Stt句子: " + coastTime + "ms)" + text)
        runOnUiThread {
            if (Config.XF_STT_IDENTIFY_QUESTION) {
                if (GameContext.getInstance().isQuestionSentence(text)) {
                    mChatRobotManager?.cancelChatRequest(true)
                    mCancelRequest = true
                    mSttResults.append(text)
                    mChatRobotManager?.requestChat(Constants.GPT_ROLE_USER, mSttResults.toString())
                    mTtsRobotManager?.cancelTtsRequest(true)
                    mSttResults.delete(0, mSttResults.length)
                } else {
                    mSttResults.append(text)
                }
            } else {
                mChatRobotManager?.cancelChatRequest(true)
                mCancelRequest = true
                mSttResults.append(text)
                mChatRobotManager?.requestChat(Constants.GPT_ROLE_USER, text)
                mTtsRobotManager?.cancelTtsRequest(true)
            }
        }
    }

    override fun onSttFail(errorCode: Int, message: String?) {
        Log.d(TAG, "onSttFail errorCode:$errorCode message:$message")
    }

    override fun onTtsStart(text: String?, isOnSpeaking: Boolean) {
        Log.d(TAG, "onTtsStart text:$text isOnSpeaking:$isOnSpeaking")
        if (isOnSpeaking) {
            mTtsInputStream = null
            mTtsStartTime = System.currentTimeMillis()
        }
    }

    override fun onFirstTts() {
        Log.d(TAG, "onFirstTts text")
    }

    override fun onTtsFinish(isOnSpeaking: Boolean) {
        Log.d(TAG, "onTtsFinish isOnSpeaking:$isOnSpeaking ${(System.currentTimeMillis() - mTtsStartTime)}ms")
    }

    override fun updateTtsHistoryInfo(message: String?) {
        Log.d(TAG, "updateTtsHistoryInfo message:$message")
    }

    private fun runOnUiThread(action: Runnable) {
        if (Thread.currentThread() !== Looper.getMainLooper().thread) {
            mHandler.post(action)
        } else {
            action.run()
        }
    }

    fun onRecordAudioFrame(origin: ByteArray): Boolean {
        if (Utils.isByteArrayAllZero(origin)) {
            mFirstRecordAudio = true
            return false
        }
        if (mFirstRecordAudio) {
            mFirstRecordAudio = false
            mSttStartTime = System.currentTimeMillis()
        }
        if (mIsSpeaking) {
            if (Config.ENABLE_AGORA_GPT_SERVER) {
                mAgoraGptServerManager?.sendPcmData(origin)
            } else {
                mSttRobotManager?.requestStt(origin)
            }
        }
        return false
    }

    fun onPlaybackAudioFrame(buffer: ByteBuffer): Boolean {
        if (Config.ENABLE_SHARE_CHAT) {
            var bytes: ByteArray? = null
            if (Config.ENABLE_AGORA_GPT_SERVER) {
                if (null != mAgoraGptServerManager) {
                    bytes = mAgoraGptServerManager!!.getBuffer(buffer.capacity())
                }
            } else {
                if (null != mTtsRobotManager) {
                    bytes = mTtsRobotManager!!.getTtsBuffer(buffer.capacity())
                }
            }
            if (mVoiceChangeEnable && mDubbingInitSuccess) {
                if (null != bytes) {
                    mDubbingEngine!!.putDubbingVoiceBuffer(bytes)
                }
                bytes = mDubbingEngine!!.getDubbingVoiceBuffer(buffer.capacity())
            }
            if (null != bytes) {
                buffer.put(bytes, 0, buffer.capacity())
                if (0L != mChatIdleStartTime) {
                    mChatIdleStartTime = 0L
                }
                val audioBytes: ByteArray = bytes
                mExecutorService.execute {
                    MetaContext.getInstance().pushAudioToDriveAvatar(audioBytes, System.currentTimeMillis())
                }
            } else {
                if (mIsSpeaking) {
                    if (mChatIdleStartTime == 0L) {
                        mChatIdleStartTime = System.currentTimeMillis()
                    } else {
                        val now = System.currentTimeMillis()
                        if (now - mChatIdleStartTime >= Constants.INTERVAL_CHAT_IDLE_TIME) {
                            mTtsRobotManager?.requestChatTip()
                        }
                    }
                }
            }
        }
        return true
    }

    fun resetRequestChatKeyInfo() {
        mHandler.removeMessages(MESSAGE_REQUEST_CHAT_KEY_INFO)
        mHandler.removeCallbacksAndMessages(null)
        mHandler.sendEmptyMessageDelayed(MESSAGE_REQUEST_CHAT_KEY_INFO, (60 * 1000).toLong())
    }

    private fun initDubbingEngine(activity: Activity) {
        if (null == mDubbingEngine) {
            mDubbingEngine = DubbingVoiceEngine().also {
                it.setCallBack {
                    mDubbingInitSuccess = true
                    it.start()
                }
            }
        }
        mDubbingEngine?.initEngine(
            activity,
            EncryptUtil.buildDubbingToken(
                KeyCenter.getAiUid(),
                BuildConfig.DUBBING_SECRET_KEY,
                BuildConfig.DUBBING_ACCESS_KEY
            ),
            Constants.RTC_AUDIO_SAMPLE_RATE,
            Constants.RTC_AUDIO_SAMPLE_RATE,
            Constants.RTC_AUDIO_SAMPLES * Constants.STT_BITS_PER_SAMPLE / 8,
            false
        )
    }
}