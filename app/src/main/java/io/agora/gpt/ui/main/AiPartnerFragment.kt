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
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import io.agora.aigc.sdk.constants.ServiceCode
import io.agora.aigc.sdk.constants.ServiceEvent
import io.agora.gpt.R
import io.agora.gpt.databinding.FragmentAiPartnerBinding
import io.agora.gpt.ui.adapter.ChatMessageAdapter
import io.agora.gpt.ui.base.BaseFragment
import io.agora.gpt.ui.view.OnFastClickListener
import io.agora.gpt.ui.view.WrapContentLinearLayoutManager
import io.agora.gpt.utils.KeyCenter
import io.agora.gpt.utils.TextureVideoViewOutlineProvider
import io.agora.gpt.utils.dp

class AiPartnerFragment : BaseFragment() {

    private var mBinding: FragmentAiPartnerBinding? = null

    private var mTextureView: TextureView? = null

    private var mIsSpeaking = false

    private var mSelectIndex: Int = 0

    private val mAiShareViewModel: AiShareViewModel by activityViewModels()

    private var mHistoryListAdapter: ChatMessageAdapter? = null

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

    override fun initView() {
        super.initView()
        initUnityView()

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
                mAiShareViewModel.releaseEngine()
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
    }

    private fun initUnityView() {
        val requireContext = context ?: return
        mTextureView = TextureView(requireContext)
        mTextureView?.outlineProvider = TextureVideoViewOutlineProvider(64.dp);
        mTextureView?.clipToOutline = true
        mTextureView?.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, i: Int, i1: Int) {
                mAiShareViewModel.setTexture(requireActivity(), mTextureView!!)
                mAiShareViewModel.setAvatarModel(mAiShareViewModel.getCurAiRole())
                mAiShareViewModel.setServiceVendor()
                mBinding?.btnCalling?.text = resources.getString(R.string.calling, mAiShareViewModel.getCurAiRole().getRoleName())
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
}