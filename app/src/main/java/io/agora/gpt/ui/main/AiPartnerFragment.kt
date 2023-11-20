package io.agora.gpt.ui.main

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import io.agora.aigc.sdk.constants.ServiceCode
import io.agora.aigc.sdk.constants.ServiceEvent
import io.agora.aigc.sdk.model.ServiceVendor
import io.agora.aigc.sdk.model.ServiceVendorGroup
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

class AiPartnerFragment : BaseFragment() {

    private var mBinding: FragmentAiPartnerBinding? = null

    private var mTextureView: TextureView? = null

    private var mIsSpeaking = false

    private var mSelectIndex: Int = 0

    private val mAiShareViewModel: AiShareViewModel by activityViewModels()

    private var mHistoryListAdapter: ChatMessageAdapter? = null

    private var mProgressLoadingDialog: AlertDialog? = null

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mProgressLoadingDialog?.let { dialog ->
            if (dialog.isShowing) dialog.dismiss()
            mProgressLoadingDialog = null
        }
    }

    override fun initView() {
        super.initView()
        if (mProgressLoadingDialog == null) {
            mProgressLoadingDialog = CustomDialog.showLoadingProgress(requireContext())
        }
        mProgressLoadingDialog?.show()
        initUnityView()

//        mAiShareViewModel.setServiceVendor()
        mBinding?.apply {
            val requireContext = context ?: return
            tvUserName.text = KeyCenter.userName
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
            }
        })

        mBinding?.tvSwitchRole?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                if (mIsSpeaking) {
                    ToastUtils.showToast(R.string.switch_role_tips)
                } else {
                    showChooseRoleDialog()
                }
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
                    btnCalling.visibility = View.INVISIBLE
                    ivVoice.visibility = View.VISIBLE
                    ivHangUp.visibility = View.VISIBLE
                    mAiShareViewModel.startVoiceChat()
                }
            }
        })
        mBinding?.ivVoice?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
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
        })

        mBinding?.ivHangUp?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                mBinding?.apply {
                    btnCalling.visibility = View.VISIBLE
                    ivVoice.visibility = View.INVISIBLE
                    ivHangUp.visibility = View.INVISIBLE
                    mAiShareViewModel.stopVoiceChat()
                }
            }
        })

//        mBinding?.tvEnableVirtual?.setOnClickListener(object : OnFastClickListener() {
//            override fun onClickJacking(view: View) {
//                mAiShareViewModel.enableVirtualHuman {
//                    updateTextureSize(it)
//                }
//            }
//        })
    }

    private fun initUnityView() {
        val requireContext = context ?: return
        mTextureView = TextureView(requireContext)
        mTextureView?.outlineProvider = TextureVideoViewOutlineProvider(64.dp);
        mTextureView?.clipToOutline = true
        mTextureView?.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, i: Int, i1: Int) {
                mAiShareViewModel.setTexture(requireActivity(), mTextureView!!)
                val usableAIRoles = mAiShareViewModel.getUsableAiRoles()
                if (usableAIRoles.isNotEmpty()) {
                    val aiRole = usableAIRoles[0]
                    mAiShareViewModel.setAvatarModel(aiRole)
                    mAiShareViewModel.setServiceVendor(aiRole)
                    mBinding?.btnCalling?.text = resources.getString(R.string.calling, aiRole.getRoleName())
                    mBinding?.groupOralEnglishTeacher?.isVisible = mAiShareViewModel.isEnglishTeacher(aiRole)
                }
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
        val layoutParams =
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
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
                mAiShareViewModel.setServiceVendor(aiRole)
                mBinding?.btnCalling?.text = resources.getString(R.string.calling, aiRole.getRoleName())
                mBinding?.groupOralEnglishTeacher?.isVisible = mAiShareViewModel.isEnglishTeacher(aiRole)
            }
        }
        chooseRoleDialog.setupAiRoles(mSelectIndex, mAiShareViewModel.getUsableAiRoles())
        chooseRoleDialog.show()
    }

    private fun showTopicDialog() {
        val requireContext = context ?: return
        val topicInputDialog = TopicInputDialog(requireContext)
        topicInputDialog.setInputTextCallback {
            if (it.isNotEmpty()) {
                mAiShareViewModel.pushText(Constant.COMMAND_TOPIC, it)
            }
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