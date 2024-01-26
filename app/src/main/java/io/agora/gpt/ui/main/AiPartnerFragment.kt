package io.agora.gpt.ui.main

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.ScrollView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ScrollingView
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import io.agora.aiengine.ServiceConfig
import io.agora.aigc.sdk.constants.ServiceCode
import io.agora.aigc.sdk.constants.ServiceEvent
import io.agora.gpt.R
import io.agora.gpt.databinding.FragmentAiPartnerBinding
import io.agora.gpt.ui.adapter.ChatMessageAdapter
import io.agora.gpt.ui.base.BaseFragment
import io.agora.gpt.ui.view.ChooseRoleDialog
import io.agora.gpt.ui.view.CustomDialog
import io.agora.gpt.ui.view.OnFastClickListener
import io.agora.gpt.ui.view.TopicInputDialog
import io.agora.gpt.ui.view.WrapContentLinearLayoutManager
import io.agora.gpt.utils.Constant
import io.agora.gpt.utils.KeyCenter
import io.agora.gpt.utils.TextureVideoViewOutlineProvider
import io.agora.gpt.utils.ToastUtils
import io.agora.gpt.utils.Utils
import io.agora.gpt.utils.dp
import io.agora.gpt.utils.statusBarHeight

class AiPartnerFragment : BaseFragment() {

    private var mBinding: FragmentAiPartnerBinding? = null

    private var mTextureView: TextureView? = null

    private var mSelectIndex: Int = 0

    private val mAiShareViewModel: AiShareViewModel by activityViewModels()

    private var mHistoryListAdapter: ChatMessageAdapter? = null

    private var mProgressLoadingDialog: AlertDialog? = null

    private val mMainHandler by lazy {
        Handler(Looper.getMainLooper())
    }

    override fun initContentView(inflater: LayoutInflater, container: ViewGroup?, attachToParent: Boolean) {
        super.initContentView(inflater, container, attachToParent)
        mBinding = FragmentAiPartnerBinding.inflate(inflater, container, attachToParent)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return mBinding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

            }
        })
        mBinding?.apply {
            val titleParams: ViewGroup.MarginLayoutParams = layoutUser.layoutParams as ViewGroup.MarginLayoutParams
            titleParams.topMargin = layoutUser.context.statusBarHeight
            layoutUser.layoutParams = titleParams
        }
    }

    override fun initData() {
        super.initData()
        mAiShareViewModel.mPrepareResult.observe(this, object : Observer<Boolean> {
            override fun onChanged(t: Boolean?) {
                mProgressLoadingDialog?.let { dialog ->
                    if (dialog.isShowing) dialog.dismiss()
                    mProgressLoadingDialog = null
                }
            }

        })

        mAiShareViewModel.mEventResultModel.observe(this) { eventResult ->
            if (eventResult.event == ServiceEvent.DESTROY && eventResult.code == ServiceCode.SUCCESS) {
                findNavController().popBackStack(R.id.crateRoomFragment, false)
            } else if (eventResult.event == ServiceEvent.START) {
                mBinding?.apply {
                    btnCalling.isEnabled = true
                    btnCalling.alpha = 1.0f
                    if (eventResult.code==ServiceCode.SUCCESS){
                        btnCalling.visibility = View.INVISIBLE
                        ivVoice.visibility = View.VISIBLE
                        ivHangUp.visibility = View.VISIBLE
                        if (mAiShareViewModel.isAiGame() && mAiShareViewModel.isFirstEnterRoom()) {
                            mBinding?.layoutPressTips?.isVisible = true
                            mAiShareViewModel.setFirstEnterRoom(false)
                            mMainHandler.postDelayed(mHideLongPressTipsTask, 5000)
                        } else {
                            mBinding?.layoutPressTips?.isVisible = false
                        }
                    }else{
                        btnCalling.visibility = View.VISIBLE
                        ivVoice.visibility = View.INVISIBLE
                        ivHangUp.visibility = View.INVISIBLE
                        ToastUtils.showToast(R.string.start_failed)
                    }
                }
            } else if (eventResult.event == ServiceEvent.STOP) {
                mBinding?.apply {
                    btnCalling.visibility = View.VISIBLE
                    ivVoice.visibility = View.INVISIBLE
                    ivHangUp.visibility = View.INVISIBLE
                }
            }
        }

        mAiShareViewModel.mNewLineMessageModel.observe(this) {
            mHistoryListAdapter?.let { chatMessageAdapter ->
                if (it.second) {
                    chatMessageAdapter.notifyItemInserted(mAiShareViewModel.mChatMessageDataList.size - 1)
                } else {
                    chatMessageAdapter.notifyItemChanged(it.third)
                }
                mBinding?.aiHistoryList?.scrollToPosition(chatMessageAdapter.dataList.size - 1)
            }
        }

        mAiShareViewModel.mUserSttContent.observe(this) {
            mBinding?.apply {
                tvSttContent.text = it
                scrollSttContent.post {
                    scrollSttContent.fullScroll(ScrollView.FOCUS_DOWN)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mProgressLoadingDialog?.let { dialog ->
            if (dialog.isShowing) dialog.dismiss()
            mProgressLoadingDialog = null
        }
    }

    private val mHideLongPressTipsTask: Runnable by lazy {
        Runnable {
            mBinding?.layoutPressTips?.isVisible = false
        }
    }

    override fun initView() {
        super.initView()

        if (mProgressLoadingDialog == null) {
            mProgressLoadingDialog = CustomDialog.showLoadingProgress(requireContext())
        }
        mProgressLoadingDialog?.show()
        initUnityView()

        mBinding?.apply {
            val requireContext = context ?: return
            tvUserName.text = KeyCenter.mUserName
            if (mHistoryListAdapter == null) {
                mHistoryListAdapter = ChatMessageAdapter(requireContext, mAiShareViewModel.mChatMessageDataList)
                aiHistoryList.layoutManager = WrapContentLinearLayoutManager(requireContext)
                aiHistoryList.adapter = mHistoryListAdapter
            } else {
                Log.d("AiPartnerFragment", "clear history messages")
                mAiShareViewModel.mChatMessageDataList.clear()
                mHistoryListAdapter?.notifyDataSetChanged()
            }
        }
        mBinding?.btnExit?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                mBinding?.apply {
                    if (ivHangUp.visibility == View.VISIBLE) {
                        btnCalling.visibility = View.VISIBLE
                        ivVoice.visibility = View.INVISIBLE
                        ivHangUp.visibility = View.INVISIBLE
                        mAiShareViewModel.stopVoiceChat()
                    }
                }
                if (mProgressLoadingDialog == null) {
                    mProgressLoadingDialog = CustomDialog.showLoadingProgress(requireContext())
                }
                mProgressLoadingDialog?.show()
                mAiShareViewModel.releaseEngine()
                mMainHandler.removeCallbacksAndMessages(null)
            }
        })

        mBinding?.tvSwitchRole?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                showChooseRoleDialog()
            }
        })

        mBinding?.tvVoiceChange?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                mAiShareViewModel.enableVoiceChange { isVoiceChange ->
                    mBinding?.apply {
                        if (isVoiceChange) {
                            mBinding?.tvVoiceChange?.setCompoundDrawablesRelative(
                                null,
                                root.context.getDrawable(R.drawable.icon_voice_change)?.apply {
                                    setBounds(0, 0, intrinsicWidth, intrinsicHeight)
                                },
                                null,
                                null
                            )
                        } else {
                            mBinding?.tvVoiceChange?.setCompoundDrawablesRelative(
                                null,
                                root.context.getDrawable(R.drawable.icon_voice_default)?.apply {
                                    setBounds(0, 0, intrinsicWidth, intrinsicHeight)
                                },
                                null,
                                null
                            )
                        }
                    }
                }
            }
        })

        mBinding?.tvTopic?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                if (mAiShareViewModel.enableEnglishTeacher()) {
                    showTopicDialog()
                } else {
                    ToastUtils.showToast(R.string.send_command_error)
                }
            }
        })

        mBinding?.tvEvaluate?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                if (mAiShareViewModel.enableEnglishTeacher()) {
                    mAiShareViewModel.pushText(Constant.COMMAND_EVALUATE)
                } else {
                    ToastUtils.showToast(R.string.send_command_error)
                }
            }
        })

        mBinding?.btnCalling?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                mBinding?.apply {
                    btnCalling.isEnabled = false
                    btnCalling.alpha = 0.3f
                    mAiShareViewModel.startVoiceChat()
                }
            }
        })
        mBinding?.ivVoice?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                if (mAiShareViewModel.isAiGame()) {
                    mBinding?.tvSttContent?.text = ""
                    mAiShareViewModel.startUserSpeak {
                        mBinding?.groupStt?.isVisible = true
                    }
                } else {
                    mAiShareViewModel.mute { isMute ->
                        mBinding?.apply {
                            if (isMute) {
                                ivVoice.setImageResource(R.drawable.ic_mute)
                            } else {
                                ivVoice.setImageResource(R.drawable.ic_unmute)
                            }
                        }
                    }
                }
            }
        })

        mBinding?.ivHangUp?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                mBinding?.apply {
                    mAiShareViewModel.stopVoiceChat()
                }
            }
        })

        mBinding?.btnSendText?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                mBinding?.apply {
                    mAiShareViewModel.mute {
                        groupStt.isVisible = false
                        val content = tvSttContent.text.toString()
                        if (content.isEmpty()) {
                            ToastUtils.showToast(R.string.the_recording_was_not_identified)
                        } else {
                            // 发送后展示消息
                            mAiShareViewModel.checkAddUserSttContent()
                            mAiShareViewModel.pushText(null, content)
                        }
                    }
                }
            }
        })
        mBinding?.btnCancelText?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                mBinding?.apply {
                    mAiShareViewModel.mute {
                        groupStt.isVisible = false
                        tvSttContent.text = ""
                    }
                }
            }
        })
    }

    private fun initUnityView() {
        val requireContext = context ?: return
        mTextureView = TextureView(requireContext)
        mTextureView?.outlineProvider = TextureVideoViewOutlineProvider(64.dp);
        mTextureView?.clipToOutline = true
        mTextureView?.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, i: Int, i1: Int) {
                var tempAiRole = mAiShareViewModel.currentRole()
                if (tempAiRole == null) {
                    val aiRoles = mAiShareViewModel.getUsableAiRoles()
                    if (aiRoles.isEmpty()) {
                        ToastUtils.showToast("No roles are available!")
                        return
                    } else {
                        tempAiRole = aiRoles[0]
                    }
                }

                mAiShareViewModel.setTexture(requireActivity(), mTextureView!!)
                // updateConfig 会重置，需要重新设置 aiRole
                mAiShareViewModel.setAvatarModel(tempAiRole)
                mAiShareViewModel.setAiRole(tempAiRole)
                mAiShareViewModel.setServiceVendor(tempAiRole)
                mBinding?.btnCalling?.text = resources.getString(R.string.calling, tempAiRole.getRoleName())
                mBinding?.tvTopic?.isVisible = mAiShareViewModel.isEnglishTeacher(tempAiRole)
                mBinding?.tvEvaluate?.isVisible = mAiShareViewModel.isEnglishTeacher(tempAiRole)
                mBinding?.tvVoiceChange?.isVisible = ServiceConfig.SERVICE_VOICE_CHANGE_ENABLE
                mBinding?.tvSwitchRole?.isVisible = !mAiShareViewModel.isAiGame()
                mAiShareViewModel.prepare()
            }

            override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, i: Int, i1: Int) {
                Log.d("alien", "onSurfaceTextureSizeChanged")
            }

            override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
        }
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        mBinding?.layoutUnityContainer?.removeAllViews()
        mBinding?.layoutUnityContainer?.addView(mTextureView, 0, layoutParams)
    }

    private fun showChooseRoleDialog() {
        val requireContext = context ?: return
        val chooseRoleDialog = ChooseRoleDialog(requireContext, mAiShareViewModel.currentLanguage())
        chooseRoleDialog.setSelectRoleCallback {
            mSelectIndex = it
            mBinding?.apply {
                btnCalling.visibility = View.VISIBLE
                ivVoice.visibility = View.INVISIBLE
                ivHangUp.visibility = View.INVISIBLE
            }
            val usableAIRoles = mAiShareViewModel.getUsableAiRoles()
            mAiShareViewModel.stopVoiceChat()
            if (usableAIRoles.isNotEmpty()) {
                val aiRole = usableAIRoles[it]
                mAiShareViewModel.setAvatarModel(aiRole)
                mAiShareViewModel.setAiRole(aiRole)
                mAiShareViewModel.setServiceVendor(aiRole)
                mBinding?.btnCalling?.text = resources.getString(R.string.calling, aiRole.getRoleName())
                mBinding?.tvTopic?.isVisible = mAiShareViewModel.isEnglishTeacher(aiRole)
                mBinding?.tvEvaluate?.isVisible = mAiShareViewModel.isEnglishTeacher(aiRole)
            }
        }
        chooseRoleDialog.setupAiRoles(mSelectIndex, mAiShareViewModel.getUsableAiRoles())
        chooseRoleDialog.show()
    }

    private fun showTopicDialog() {
        val requireContext = activity ?: return
        val topicInputDialog = TopicInputDialog(requireContext)
        topicInputDialog.setInputTextCallback {
            if (it.isNotEmpty()) {
                mAiShareViewModel.pushText(Constant.COMMAND_TOPIC, it)
            }
        }
        topicInputDialog.setOnShowListener {

        }
        topicInputDialog.show()
    }

    private fun updateTextureSize(full: Boolean) {
        val requireContext = context ?: return
        mBinding?.layoutUnityContainer?.let { unityContainer ->
            if (full) {
                val containerParams =
                    ConstraintLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                mTextureView?.outlineProvider =
                    TextureVideoViewOutlineProvider(Utils.dip2px(requireContext, 0f).toFloat());
                mTextureView?.clipToOutline = false
                unityContainer.layoutParams = containerParams
            } else {
                val containerParams = ConstraintLayout.LayoutParams(132.dp.toInt(), 132.dp.toInt())
                mTextureView?.outlineProvider =
                    TextureVideoViewOutlineProvider(64.dp);
                mTextureView?.clipToOutline = true
            }
        }

    }
}