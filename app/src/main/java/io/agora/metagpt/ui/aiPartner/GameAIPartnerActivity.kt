package io.agora.metagpt.ui.aiPartner

import android.content.Context
import android.content.Intent
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.alibaba.fastjson.JSONObject
import com.jakewharton.rxbinding2.view.RxView
import io.agora.meta.IMetaScene
import io.agora.metagpt.R
import io.agora.metagpt.context.GameContext
import io.agora.metagpt.context.MetaContext
import io.agora.metagpt.databinding.GameAiPartnerBinding
import io.agora.metagpt.models.UnityMessage
import io.agora.metagpt.ui.base.BaseActivity
import io.agora.metagpt.ui.main.CreateRoomActivity
import io.agora.metagpt.ui.view.ChooseRoleDialog
import io.agora.metagpt.utils.Config
import io.agora.metagpt.utils.Constants
import io.reactivex.disposables.Disposable
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class GameAIPartnerActivity : BaseActivity() {

    companion object {
        private val TAG = io.agora.metagpt.utils.Constants.TAG + "-" + GameAIPartnerActivity::class.java.simpleName

        @JvmStatic
        fun startActivity(activity: Context) {
            val intent = Intent(activity, GameAIPartnerActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            activity.startActivity(intent)
        }
    }

    private lateinit var binding: GameAiPartnerBinding

    private var mReCreateScene = false
    private var mTextureView: TextureView? = null

    private var _isEnterScene: AtomicBoolean = AtomicBoolean(false)
    private val isEnterScene get() = _isEnterScene.get()

    private lateinit var aiPartnerViewModel: AiPartnerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.keepScreenOn = true
        ViewCompat.setOnApplyWindowInsetsListener(binding.superLayout) { v: View?, insets: WindowInsetsCompat ->
            val inset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.setPaddingRelative(inset.left, inset.top, inset.right, inset.bottom)
            WindowInsetsCompat.CONSUMED
        }
        initUnityView()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        mReCreateScene = true
        maybeCreateScene()
    }

    override fun initContentView() {
        super.initContentView()
        binding = GameAiPartnerBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun initData() {
        super.initData()
        aiPartnerViewModel = ViewModelProvider(this)[AiPartnerViewModel::class.java]
        aiPartnerViewModel.initData()
    }

    override fun initView() {
        super.initView()
        binding.apply {
            tvUserName.text = GameContext.getInstance().userName
            tvRoomId.text = GameContext.getInstance().roomName
            GameContext.getInstance().currentChatBotRole?.let { chatRole ->
                btnCalling.text = getString(R.string.calling, chatRole.chatBotName)
            }
        }
    }

    override fun initClickEvent() {
        super.initClickEvent()
        var disposable: Disposable = RxView.clicks(binding.btnExit)
            .throttleFirst(1, TimeUnit.SECONDS)
            .subscribe {
                aiPartnerViewModel.exit()
                Toast.makeText(this, "btnExit", Toast.LENGTH_LONG).show()
            }
        compositeDisposable.add(disposable)

        disposable = RxView.clicks(binding.tvSwitchRole)
            .throttleFirst(1, TimeUnit.SECONDS)
            .subscribe {
                if (aiPartnerViewModel.isSpeaking()) {
                    Toast.makeText(this, "正在对话中，无法切换角色！", Toast.LENGTH_LONG).show()
                } else {
                    showChooseRoleDialog()
                }
            }
        compositeDisposable.add(disposable)

        disposable = RxView.clicks(binding.btnCalling)
            .throttleFirst(1, TimeUnit.SECONDS)
            .subscribe {
                binding.btnCalling.visibility = View.INVISIBLE
                binding.ivVoice.visibility = View.VISIBLE
                binding.ivHangUp.visibility = View.VISIBLE
            }
        compositeDisposable.add(disposable)

        disposable = RxView.clicks(binding.ivVoice)
            .throttleFirst(1, TimeUnit.SECONDS)
            .subscribe {
                aiPartnerViewModel.startSpeaking { isSpeaking ->
                    if (isSpeaking) {
                        binding.ivVoice.setImageResource(R.drawable.ic_unmute)
                    } else {
                        binding.ivVoice.setImageResource(R.drawable.ic_mute)
                    }
                }
            }
        compositeDisposable.add(disposable)

        disposable = RxView.clicks(binding.ivHangUp)
            .throttleFirst(1, TimeUnit.SECONDS)
            .subscribe {
                binding.btnCalling.visibility = View.VISIBLE
                binding.ivVoice.visibility = View.INVISIBLE
                binding.ivHangUp.visibility = View.INVISIBLE
            }
        compositeDisposable.add(disposable)
    }

    private fun showChooseRoleDialog() {
        val chooseRoleDialog = ChooseRoleDialog(this)
        chooseRoleDialog.setSelectRoleCallback {
            binding.btnCalling.visibility = View.VISIBLE
            binding.ivVoice.visibility = View.INVISIBLE
            binding.ivHangUp.visibility = View.INVISIBLE
            GameContext.getInstance().currentChatBotRole?.let { chatRole ->
                binding.btnCalling.text = getString(R.string.calling, chatRole.chatBotName)
            }
        }
        chooseRoleDialog.show()
    }

    private fun initUnityView() {
        mTextureView = TextureView(this)
        mTextureView?.surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, i: Int, i1: Int) {
                mReCreateScene = true
                maybeCreateScene()
            }

            override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, i: Int, i1: Int) {
                Log.d(TAG, "onSurfaceTextureSizeChanged")
            }

            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
        }
        val layoutParams =
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        binding.layoutUnityContainer.addView(mTextureView, 0, layoutParams)
    }

    @Synchronized
    private fun maybeCreateScene() {
        Log.d(TAG, "maybeCreateScene")
        if (mReCreateScene && mIsFront) {
            registerMeta()
            registerRtc()
            if (Config.ENABLE_SHARE_CHAT) {
                MetaContext.getInstance().setPlaybackAudioFrameParameters(
                    Constants.RTC_AUDIO_SAMPLE_RATE,
                    Constants.RTC_AUDIO_SAMPLE_NUM_OF_CHANNEL,
                    io.agora.rtc2.Constants.RAW_AUDIO_FRAME_OP_MODE_READ_WRITE,
                    Constants.RTC_AUDIO_SAMPLES
                )
                //MetaContext.getInstance().setExternalAudioSink(true, Constants.RTC_AUDIO_SAMPLE_RATE, Constants.RTC_AUDIO_SAMPLE_NUM_OF_CHANNEL);
                MetaContext.getInstance().setRecordingAudioFrameParameters(
                    Constants.RTC_AUDIO_SAMPLE_RATE,
                    Constants.RTC_AUDIO_SAMPLE_NUM_OF_CHANNEL,
                    io.agora.rtc2.Constants.RAW_AUDIO_FRAME_OP_MODE_READ_WRITE,
                    Constants.RTC_AUDIO_SAMPLES
                )
            }
            MetaContext.getInstance().updatePublishCustomAudioTrackChannelOptions(
                true,
                Constants.STT_SAMPLE_RATE,
                Constants.STT_SAMPLE_NUM_OF_CHANNEL,
                Constants.STT_SAMPLE_NUM_OF_CHANNEL,
                false,
                true
            )
            mReCreateScene = false
            MetaContext.getInstance().createScene(this, mTextureView)
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume")
        maybeCreateScene()
    }

    override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
        super.onJoinChannelSuccess(channel, uid, elapsed)
        if (Config.ENABLE_SHARE_CHAT) {
            MetaContext.getInstance().registerAudioFrameObserver(this)
            //pullPlaybackAudioFrame();
            if (Config.ENABLE_META_VOICE_DRIVER) {
                MetaContext.getInstance().enableVoiceDriveAvatar(true)
            }
        }
    }

    override fun onRecordAudioFrame(
        channelId: String?,
        type: Int,
        samplesPerChannel: Int,
        bytesPerSample: Int,
        channels: Int,
        samplesPerSec: Int,
        buffer: ByteBuffer?,
        renderTimeMs: Long,
        avsync_type: Int
    ): Boolean {
        return super.onRecordAudioFrame(
            channelId,
            type,
            samplesPerChannel,
            bytesPerSample,
            channels,
            samplesPerSec,
            buffer,
            renderTimeMs,
            avsync_type
        )
    }

    override fun onEnterSceneResult(errorCode: Int) {
        super.onEnterSceneResult(errorCode)
        runOnUiThread {
            _isEnterScene.set(true)
            updateViewMode()
        }
    }

    private fun updateViewMode() {
        val message = UnityMessage()
        message.key = "setCamera"
        val valueJson = JSONObject()
        valueJson["viewMode"] = 1
        message.value = valueJson.toJSONString()
        MetaContext.getInstance().sendSceneMessage(JSONObject.toJSONString(message))
    }

    override fun onLeaveSceneResult(errorCode: Int) {
        super.onLeaveSceneResult(errorCode)
        runOnUiThread {
            _isEnterScene.set(false)
        }
    }

    override fun onReleasedScene(status: Int) {
        super.onReleasedScene(status)
        if (status == 0) {
            runOnUiThread {
                MetaContext.getInstance().destroy()
                initData()
            }
            unregister()
            val intent: Intent = Intent(this@GameAIPartnerActivity, CreateRoomActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
        }
    }

    override fun onCreateSceneResult(scene: IMetaScene?, errorCode: Int) {
        super.onCreateSceneResult(scene, errorCode)
        if (errorCode == 0) {
            //异步线程回调需在主线程处理
            runOnUiThread {
                MetaContext.getInstance().enterScene()
            }
        }
    }
}