package io.agora.metagpt.ui.aiPartner

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.lifecycle.ViewModel
import io.agora.metagpt.R
import io.agora.metagpt.chat.ChatRobotManager
import io.agora.metagpt.context.GameContext
import io.agora.metagpt.context.MetaContext
import io.agora.metagpt.inf.ChatCallback
import io.agora.metagpt.inf.SttCallback
import io.agora.metagpt.inf.TtsCallback
import io.agora.metagpt.stt.SttRobotManager
import io.agora.metagpt.tts.TtsRobotManager
import io.agora.metagpt.utils.Constants
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

    private var mRequestChatKeyInfoIndex = 0
    private var mExecutorService: ExecutorService? = null

    private val mSttResults: StringBuilder = java.lang.StringBuilder()
    private var mFirstRecordAudio = false
    private var mIsSpeaking = false
    private var mChatRobotManager: ChatRobotManager? = null

    private var mTtsRobotManager: TtsRobotManager? = null
    private var mSttRobotManager: SttRobotManager? = null


    fun initData() {
        if (null == mExecutorService) {
            mExecutorService = ThreadPoolExecutor(
                0, Int.MAX_VALUE,
                60L, TimeUnit.SECONDS,
                SynchronousQueue(), Executors.defaultThreadFactory(), ThreadPoolExecutor.AbortPolicy()
            )
        }
    }

    fun isSpeaking():Boolean = mIsSpeaking

    fun startSpeaking(callback: ((isSpeaking: Boolean) -> Unit)) {
        if (mIsSpeaking) {
            mIsSpeaking = false
            mSttRobotManager?.requestStt(null)
            MetaContext.getInstance().updateRoleSpeak(false)
            mTtsRobotManager?.clearData()
        } else {
            mIsSpeaking = true
            mFirstRecordAudio = true
            mSttResults.delete(0, mSttResults.length)
            mTtsRobotManager?.setIsSpeaking(mIsSpeaking)
            MetaContext.getInstance().updateRoleSpeak(true)
            mHandler.sendEmptyMessageDelayed(MESSAGE_REQUEST_CHAT_KEY_INFO, (60 * 1000).toLong())
        }
        callback.invoke(mIsSpeaking)
    }

    fun exit() {
        mIsSpeaking = false
        mSttRobotManager?.close()
        mTtsRobotManager?.close()
        mChatRobotManager?.close()
        MetaContext.getInstance().leaveScene()
        mHandler.removeMessages(MESSAGE_REQUEST_CHAT_KEY_INFO)
        mHandler.removeCallbacksAndMessages(null)
    }

    override fun onSttResult(text: String?, isFinish: Boolean) {
        Log.d(TAG, "onSttResult text:$text isFinish:$isFinish")
    }

    override fun onSttFail(errorCode: Int, message: String?) {
        Log.d(TAG, "onSttFail errorCode:$errorCode message:$message")
    }

    override fun onTtsStart(text: String?, isOnSpeaking: Boolean) {
        Log.d(TAG, "onTtsStart text:$text isOnSpeaking:$isOnSpeaking")
    }

    override fun onTtsFinish(isOnSpeaking: Boolean) {
        Log.d(TAG, "onTtsFinish isOnSpeaking:$isOnSpeaking")
    }

    override fun updateTtsHistoryInfo(message: String?) {
        Log.d(TAG, "updateTtsHistoryInfo message:$message")
    }
}