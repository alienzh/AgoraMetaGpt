package io.agora.gpt.ui.main

import android.graphics.SurfaceTexture
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.jakewharton.rxbinding2.view.RxView
import io.agora.ai.sdk.AIEngine
import io.agora.ai.sdk.AIEngineAction
import io.agora.ai.sdk.AIEngineCode
import io.agora.gpt.R
import io.agora.gpt.databinding.FragmentAiPartnerBinding
import io.agora.gpt.ui.base.BaseFragment
import io.agora.gpt.ui.view.ChooseRoleDialog
import io.agora.gpt.ui.view.CustomDialog
import io.agora.gpt.utils.KeyCenter
import io.agora.gpt.utils.TextureVideoViewOutlineProvider
import io.agora.gpt.utils.Utils
import io.reactivex.disposables.Disposable
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiPartnerFragment : BaseFragment() {

    private var binding: FragmentAiPartnerBinding? = null

    private var mTextureView: TextureView? = null

    private var mIsSpeaking = false

    private var selectIndex: Int = 0

    private val aiShareViewModel: AiShareViewModel by activityViewModels()

    override fun initContentView(inflater: LayoutInflater, container: ViewGroup?, attachToParent: Boolean) {
        super.initContentView(inflater, container, attachToParent)
        binding = FragmentAiPartnerBinding.inflate(inflater, container, attachToParent)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun initData() {
        super.initData()
        aiShareViewModel.actionResultModel.observe(viewLifecycleOwner) {
            if (AIEngineAction.DOWNLOAD == it.vcAction) {
                if (AIEngineCode.SUCCESS == it.vcEngineCode) {
                    // 下载完成
                } else if (AIEngineCode.DOWNLOAD_RES == it.vcEngineCode) {
                    // 需要下载
                }
            } else if (AIEngineAction.PREPARE == it.vcAction && AIEngineCode.SUCCESS == it.vcEngineCode) {
                binding?.btnCalling?.text = resources.getString(R.string.calling,aiShareViewModel.getAvatarName())
            } else if (AIEngineAction.RELEASED == it.vcAction && AIEngineCode.SUCCESS == it.vcEngineCode) {

            }
        }
    }

    override fun initView() {
        super.initView()
        initUnityView()
        binding?.apply {
            tvUserName.text = KeyCenter.getUserName()
        }
    }

    override fun initClickEvent() {
        super.initClickEvent()
        binding?.apply {
            var disposable: Disposable = RxView.clicks(btnExit)
                .throttleFirst(1, TimeUnit.SECONDS)
                .subscribe {
                    if (ivHangUp.visibility == View.VISIBLE) {
                        btnCalling.visibility = View.VISIBLE
                        ivVoice.visibility = View.INVISIBLE
                        ivHangUp.visibility = View.INVISIBLE
                        aiShareViewModel.stopVoiceChat()
                    }
                    findNavController().popBackStack()
//                    aiShareViewModel.releaseEngine()
                }
            compositeDisposable.add(disposable)

            // 切换角色
            disposable = RxView.clicks(tvSwitchRole)
                .throttleFirst(1, TimeUnit.SECONDS)
                .subscribe {
                    if (mIsSpeaking) {
                        Toast.makeText(requireContext(), "正在对话中，无法切换角色！", Toast.LENGTH_LONG).show()
                    } else {
                        showChooseRoleDialog()
                    }
                }
            compositeDisposable.add(disposable)

            // 变声
            disposable = RxView.clicks(tvVoiceChange)
                .throttleFirst(1, TimeUnit.SECONDS)
                .subscribe {
                    aiShareViewModel.enableVoiceChange { isVoiceChange ->
                        if (isVoiceChange) {
                            tvVoiceChange.setCompoundDrawablesRelative(
                                null,
                                AppCompatResources.getDrawable(requireContext(), R.drawable.icon_voice_change),
                                null,
                                null
                            )
                        } else {
                            tvVoiceChange.setCompoundDrawablesRelative(
                                null,
                                AppCompatResources.getDrawable(requireContext(), R.drawable.icon_voice_default),
                                null,
                                null
                            )
                        }
                    }
                }
            compositeDisposable.add(disposable)

            // 开启语聊
            disposable = RxView.clicks(btnCalling)
                .throttleFirst(1, TimeUnit.SECONDS)
                .subscribe {
                    btnCalling.visibility = View.INVISIBLE
                    ivVoice.visibility = View.VISIBLE
                    ivHangUp.visibility = View.VISIBLE
                    aiShareViewModel.startVoiceChat()
                }
            compositeDisposable.add(disposable)

            // 静音
            disposable = RxView.clicks(ivVoice)
                .throttleFirst(1, TimeUnit.SECONDS)
                .subscribe {
                    aiShareViewModel.mute { isMute ->
                        if (isMute) {
                            ivVoice.setImageResource(R.drawable.ic_mute)
                        } else {
                            ivVoice.setImageResource(R.drawable.ic_unmute)
                        }
                    }
                }
            compositeDisposable.add(disposable)

            // 关闭语聊
            disposable = RxView.clicks(ivHangUp)
                .throttleFirst(1, TimeUnit.SECONDS)
                .subscribe {
                    btnCalling.visibility = View.VISIBLE
                    ivVoice.visibility = View.INVISIBLE
                    ivHangUp.visibility = View.INVISIBLE
                    aiShareViewModel.stopVoiceChat()
                }
            compositeDisposable.add(disposable)
        }

    }

    private fun initUnityView() {
        mTextureView = TextureView(requireContext())
        mTextureView?.outlineProvider = TextureVideoViewOutlineProvider(Utils.dip2px(requireContext(), 24f).toFloat());
        mTextureView?.clipToOutline = true
        mTextureView?.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, i: Int, i1: Int) {
                aiShareViewModel.setTexture(requireActivity(), mTextureView!!)
                val allAIRoles = aiShareViewModel.getAllAiRoles()
                if (allAIRoles.isNotEmpty()) {
                    aiShareViewModel.setAiRole(allAIRoles[1])
                }
                aiShareViewModel.prepare()
            }

            override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, i: Int, i1: Int) {
                Log.d("alien", "onSurfaceTextureSizeChanged")
            }

            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
        }
        val layoutParams =
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        binding?.layoutUnityContainer?.removeAllViews()
        binding?.layoutUnityContainer?.addView(mTextureView, 0, layoutParams)
    }

    private fun showChooseRoleDialog() {
        val chooseRoleDialog = ChooseRoleDialog(requireContext())
        chooseRoleDialog.setSelectRoleCallback {
            selectIndex = it
            binding?.apply {
                btnCalling.visibility = View.VISIBLE
                ivVoice.visibility = View.INVISIBLE
                ivHangUp.visibility = View.INVISIBLE
            }
            val allAIRoles = aiShareViewModel.getAllAiRoles()
            if (allAIRoles.isNotEmpty()) {
                aiShareViewModel.setAiRole(allAIRoles[it])
                aiShareViewModel.prepare()
//                binding?.btnCalling?.text = resources.getString(R.string.calling,aiShareViewModel.getAvatarName())
            }
        }
        chooseRoleDialog.setupAiRoles(selectIndex, aiShareViewModel.getAllAiRoles())
        chooseRoleDialog.show()
    }
}