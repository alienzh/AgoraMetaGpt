package io.agora.gpt.ui.aiPartner

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
import io.agora.gpt.R
import io.agora.gpt.context.GameContext
import io.agora.gpt.context.MetaContext
import io.agora.gpt.databinding.GameAiPartnerBinding
import io.agora.gpt.models.UnityMessage
import io.agora.gpt.ui.base.BaseActivity
import io.agora.gpt.ui.main.CreateRoomActivity
import io.agora.gpt.ui.view.ChooseRoleDialog
import io.agora.gpt.utils.Config
import io.agora.gpt.utils.Constants
import io.agora.gpt.utils.TextureVideoViewOutlineProvider
import io.agora.gpt.utils.Utils
import io.agora.rtc2.DataStreamConfig
import io.reactivex.disposables.Disposable
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class GameAIPartnerActivity : BaseActivity() {

    companion object {
        private val TAG = io.agora.gpt.utils.Constants.TAG + "-" + GameAIPartnerActivity::class.java.simpleName

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

    private var mJoinChannelSuccess = false
    private var mStreamId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.keepScreenOn = true
        ViewCompat.setOnApplyWindowInsetsListener(binding.superLayout) { v: View?, insets: WindowInsetsCompat ->
            val inset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.setPaddingRelative(inset.left, 0, inset.right, inset.bottom)
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
                aiPartnerViewModel.setChatBotRole(chatRole)
                if (Config.ENABLE_SHARE_CHAT) {
                    MetaContext.getInstance()
                        .setAvatarType(resources.getStringArray(R.array.avatar_model_value)[GameContext.getInstance().currentChatBotRoleIndex])
                }
            }
        }
    }

    override fun initClickEvent() {
        super.initClickEvent()
        var disposable: Disposable = RxView.clicks(binding.btnExit)
            .throttleFirst(1, TimeUnit.SECONDS)
            .subscribe {
                if (binding.ivHangUp.visibility==View.VISIBLE){
                    binding.btnCalling.visibility = View.VISIBLE
                    binding.ivVoice.visibility = View.INVISIBLE
                    binding.ivHangUp.visibility = View.INVISIBLE
                    aiPartnerViewModel.hangUp()
                }
                aiPartnerViewModel.exit()
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
                aiPartnerViewModel.startCalling()
            }
        compositeDisposable.add(disposable)

        disposable = RxView.clicks(binding.ivVoice)
            .throttleFirst(1, TimeUnit.SECONDS)
            .subscribe {
                aiPartnerViewModel.mute { isMute ->
                    if (isMute) {
                        binding.ivVoice.setImageResource(R.drawable.ic_mute)
                    } else {
                        binding.ivVoice.setImageResource(R.drawable.ic_unmute)
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
                aiPartnerViewModel.hangUp()
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
                aiPartnerViewModel.setChatBotRole(chatRole)
                MetaContext.getInstance()
                    .setAvatarType(resources.getStringArray(R.array.avatar_model_value)[GameContext.getInstance().currentChatBotRoleIndex])
                MetaContext.getInstance().updateAvatar()
                updateAvatarBg()
                aiPartnerViewModel.resetRequestChatKeyInfo()
            }
        }
        chooseRoleDialog.show()
    }

    private fun initUnityView() {
        mTextureView = TextureView(this)
        mTextureView?.outlineProvider = TextureVideoViewOutlineProvider(Utils.dip2px(this, 24f).toFloat());
        mTextureView?.clipToOutline = true
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
                    io.agora.rtc2.Constants.RAW_AUDIO_FRAME_OP_MODE_READ_ONLY,
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

    override fun onDestroy() {
        super.onDestroy()
        aiPartnerViewModel.onDestroy()
    }

    override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
        super.onJoinChannelSuccess(channel, uid, elapsed)
        mJoinChannelSuccess = true
        if (-1 == mStreamId) {
            val cfg = DataStreamConfig()
            cfg.syncWithAudio = false
            cfg.ordered = true
            mStreamId = MetaContext.getInstance().createDataStream(cfg)
        }
        if (Config.ENABLE_SHARE_CHAT) {
            MetaContext.getInstance().registerAudioFrameObserver(this)
            //pullPlaybackAudioFrame();
            if (Config.ENABLE_META_VOICE_DRIVER) {
                MetaContext.getInstance().enableVoiceDriveAvatar(true)
            }
        }
        aiPartnerViewModel.onJoinSuccess(mStreamId)
    }

    override fun onRecordAudioFrame(
        channelId: String,
        type: Int,
        samplesPerChannel: Int,
        bytesPerSample: Int,
        channels: Int,
        samplesPerSec: Int,
        buffer: ByteBuffer,
        renderTimeMs: Long,
        avsync_type: Int
    ): Boolean {
        val length = buffer.remaining()
        val origin = ByteArray(length)
        buffer[origin]
        buffer.flip()
        return aiPartnerViewModel.onRecordAudioFrame(origin)
    }

    override fun onPlaybackAudioFrame(
        channelId: String?,
        type: Int,
        samplesPerChannel: Int,
        bytesPerSample: Int,
        channels: Int,
        samplesPerSec: Int,
        buffer: ByteBuffer,
        renderTimeMs: Long,
        avsync_type: Int
    ): Boolean {
        aiPartnerViewModel.onPlaybackAudioFrame(buffer)
        return true
    }

    override fun onEnterSceneResult(errorCode: Int) {
        super.onEnterSceneResult(errorCode)
        runOnUiThread {
            _isEnterScene.set(true)
            updateViewMode()
            updateAvatarBg()
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

    private fun updateAvatarBg() {
        GameContext.getInstance().currentChatBotRole?.let {
            var avatarBgPath = externalCacheDir!!.path + File.separator
            avatarBgPath += if (it.chatBotRole.contains("男")) {
                "bg_ai_male.png"
            } else {
                "bg_ai_female.png"
            }
            val message = UnityMessage()
            message.key = "updateBg"
            message.value = avatarBgPath
            MetaContext.getInstance().sendSceneMessage(JSONObject.toJSONString(message))
        }
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