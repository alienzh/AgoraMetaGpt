package io.agora.gpt.ui.main

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import io.agora.aigc.sdk.constants.ServiceCode
import io.agora.aigc.sdk.constants.ServiceEvent
import io.agora.gpt.R
import io.agora.gpt.databinding.FragmentAiPartnerBinding
import io.agora.gpt.ui.adapter.ChatMessageAdapter
import io.agora.gpt.ui.base.BaseFragment
import io.agora.gpt.ui.view.ChooseRoleDialog
import io.agora.gpt.ui.view.OnFastClickListener
import io.agora.gpt.ui.view.WrapContentLinearLayoutManager
import io.agora.gpt.utils.Constant
import io.agora.gpt.utils.KeyCenter
import io.agora.gpt.utils.TextureVideoViewOutlineProvider
import io.agora.gpt.utils.ToastUtils
import io.agora.gpt.utils.Utils

class AiPartnerFragment : BaseFragment() {

    private var binding: FragmentAiPartnerBinding? = null

    private var mTextureView: TextureView? = null

    private var mIsSpeaking = false

    private var selectIndex: Int = 0

    private val aiShareViewModel: AiShareViewModel by activityViewModels()

    private var mHistoryListAdapter: ChatMessageAdapter? = null

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
        activity?.onBackPressedDispatcher?.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

            }
        })
    }

    override fun initData() {
        super.initData()
        aiShareViewModel.mPrepareResult.observe(viewLifecycleOwner) {
            if (it) {
//                val avatarName = when (aiShareViewModel.getAiRoleName()) {
//                    Constant.ROLE_FOODIE -> requireContext().getString(R.string.role_foodie)
//                    Constant.ROLE_LATTE_LOVE -> requireContext().getString(R.string.role_latte_love)
//                    else -> requireContext().getString(R.string.role_foodie)
//                }
                binding?.btnCalling?.text = resources.getString(R.string.calling, aiShareViewModel.getAiRoleName())
            }
        }

        aiShareViewModel.mEventResultModel.observe(viewLifecycleOwner) { eventResult ->
            if (eventResult.event == ServiceEvent.DESTROY && eventResult.code == ServiceCode.SUCCESS) {
                findNavController().popBackStack(R.id.crateRoomFragment, false)
            }
        }
        aiShareViewModel.mNewLineMessageModel.observe(viewLifecycleOwner) {
            mHistoryListAdapter?.let { chatMessageAdapter ->
                if (it.second) {
                    chatMessageAdapter.notifyItemInserted(aiShareViewModel.mChatMessageDataList.size - 1)
                } else {
                    chatMessageAdapter.notifyItemChanged(it.third)
                }
                binding?.aiHistoryList?.scrollToPosition(chatMessageAdapter.dataList.size - 1)
            }
        }
    }

    override fun initView() {
        super.initView()
        initUnityView()
        binding?.apply {
            binding?.btnCalling?.text =
                resources.getString(R.string.calling, requireContext().getString(R.string.role_foodie))
            tvUserName.text = KeyCenter.getUserName()
            if (mHistoryListAdapter == null) {
                mHistoryListAdapter = ChatMessageAdapter(requireContext(), aiShareViewModel.mChatMessageDataList)
                aiHistoryList.layoutManager = WrapContentLinearLayoutManager(requireContext())
//                aiHistoryList.addItemDecoration(ChatMessageAdapter.SpacesItemDecoration(10))
                aiHistoryList.adapter = mHistoryListAdapter
            } else {
                Log.d("AiPartnerFragment", "clear history messages")
                aiShareViewModel.mChatMessageDataList.clear()
                mHistoryListAdapter?.notifyDataSetChanged()
            }
        }
    }

    override fun initClickEvent() {
        super.initClickEvent()
        binding?.btnExit?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                binding?.apply {
                    if (ivHangUp.visibility == View.VISIBLE) {
                        btnCalling.visibility = View.VISIBLE
                        ivVoice.visibility = View.INVISIBLE
                        ivHangUp.visibility = View.INVISIBLE
                        aiShareViewModel.stopVoiceChat()
                    }
                }
                aiShareViewModel.releaseEngine()
            }
        })

        binding?.tvSwitchRole?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                if (mIsSpeaking) {
                    ToastUtils.showToast(R.string.switch_role_tips)
                } else {
                    showChooseRoleDialog()
                }
            }
        })

        binding?.tvVoiceChange?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                aiShareViewModel.enableVoiceChange { isVoiceChange ->
                    binding?.apply {
                        if (isVoiceChange) {
                            binding?.tvVoiceChange?.setCompoundDrawablesRelative(
                                null,
                                root.context.getDrawable(R.drawable.icon_voice_change)?.apply {
                                    setBounds(0, 0, intrinsicWidth, intrinsicHeight)
                                },
                                null,
                                null
                            )
                        } else {
                            binding?.tvVoiceChange?.setCompoundDrawablesRelative(
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

        binding?.btnCalling?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                binding?.apply {
                    btnCalling.visibility = View.INVISIBLE
                    ivVoice.visibility = View.VISIBLE
                    ivHangUp.visibility = View.VISIBLE
                    aiShareViewModel.startVoiceChat()
                }
            }
        })
        binding?.ivVoice?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                aiShareViewModel.mute { isMute ->
                    binding?.apply {
                        if (isMute) {
                            ivVoice.setImageResource(R.drawable.ic_mute)
                        } else {
                            ivVoice.setImageResource(R.drawable.ic_unmute)
                        }
                    }
                }
            }
        })

        binding?.ivHangUp?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                binding?.apply {
                    btnCalling.visibility = View.VISIBLE
                    ivVoice.visibility = View.INVISIBLE
                    ivHangUp.visibility = View.INVISIBLE
                    aiShareViewModel.stopVoiceChat()
                }
            }
        })
    }

    private fun initUnityView() {
        mTextureView = TextureView(requireContext())
        mTextureView?.outlineProvider = TextureVideoViewOutlineProvider(Utils.dip2px(requireContext(), 64f).toFloat());
        mTextureView?.clipToOutline = true
        mTextureView?.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, i: Int, i1: Int) {
                aiShareViewModel.setTexture(requireActivity(), mTextureView!!)
                val usableAIRoles = aiShareViewModel.getUsableAiRoles()
                if (usableAIRoles.isNotEmpty()) {
                    aiShareViewModel.setAvatarModel(usableAIRoles[0])
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
            val usableAIRoles = aiShareViewModel.getUsableAiRoles()
            aiShareViewModel.stopVoiceChat()
            if (usableAIRoles.isNotEmpty()) {
                aiShareViewModel.setAvatarModel(usableAIRoles[it])
//                val avatarName = when (aiShareViewModel.getAiRoleName()) {
//                    Constant.ROLE_FOODIE -> requireContext().getString(R.string.role_foodie)
//                    Constant.ROLE_LATTE_LOVE -> requireContext().getString(R.string.role_latte_love)
//                    else -> requireContext().getString(R.string.role_foodie)
//                }
                binding?.btnCalling?.text = resources.getString(R.string.calling, aiShareViewModel.getAiRoleName())
            }
        }
        chooseRoleDialog.setupAiRoles(selectIndex, aiShareViewModel.getUsableAiRoles())
        chooseRoleDialog.show()
    }
}