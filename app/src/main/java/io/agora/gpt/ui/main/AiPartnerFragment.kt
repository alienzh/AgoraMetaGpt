package io.agora.gpt.ui.main

import android.graphics.SurfaceTexture
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.SeekBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import io.agora.aigc.sdk.constants.ServiceCode
import io.agora.aigc.sdk.constants.ServiceEvent
import io.agora.gpt.R
import io.agora.gpt.databinding.FragmentAiPartnerBinding
import io.agora.gpt.ui.adapter.ChatMessageAdapter
import io.agora.gpt.ui.base.BaseFragment
import io.agora.gpt.ui.view.CustomDialog
import io.agora.gpt.ui.view.OnFastClickListener
import io.agora.gpt.ui.view.WrapContentLinearLayoutManager
import io.agora.gpt.utils.Constant
import io.agora.gpt.utils.KeyCenter
import io.agora.gpt.utils.ToastUtils
import io.agora.gpt.utils.formatDurationTime
import io.agora.gpt.utils.statusBarHeight

class AiPartnerFragment : BaseFragment() {

    private var mBinding: FragmentAiPartnerBinding? = null

    private var mTextureView: TextureView? = null
    private var mMediaTextureView: TextureView? = null

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
                    if (eventResult.code == ServiceCode.SUCCESS) {
                        btnCalling.visibility = View.INVISIBLE
                        ivVoice.visibility = View.VISIBLE
                        ivHangUp.visibility = View.VISIBLE
                        mMediaTextureView?.let {
                            mAiShareViewModel.startOpenVideo(it, Constant.Sport_Video_Url)
                        }
                    } else {
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

        mAiShareViewModel.mVideoDurationData.observe(this) { duration ->
            if (duration > 0) {
                mBinding?.apply {
                    videoProgress.max = (duration / 1000).toInt()
                    videoProgress.progress = 0
                    videoLoading.isVisible = false
                }
            }
        }
        mAiShareViewModel.mVideoCurrentPosition.observe(this) { currentPosition ->
            mBinding?.apply {
                val progress: Int = (currentPosition / 1000).toInt()
                updateVideoText(progress)
            }
        }
        mAiShareViewModel.mVideoStartData.observe(this) { startPlay ->
            if (startPlay) {
                mBinding?.apply {
                    videoProgress.isEnabled = true
                    videoLoading.isVisible = false
                    updateVideoText(0)
                }
            }
        }
    }

    private fun updateVideoText(progress: Int) {
        mBinding?.apply {
            videoProgress.progress = progress
            tvVideoDuration.text =
                "${progress.formatDurationTime}/${(mAiShareViewModel.mVideoDuration / 1000).formatDurationTime}"
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
            btnExit.setOnClickListener(object : OnFastClickListener() {
                override fun onClickJacking(view: View) {
                    mBinding?.apply {
                        if (ivHangUp.visibility == View.VISIBLE) {
                            btnCalling.visibility = View.VISIBLE
                            ivVoice.visibility = View.INVISIBLE
                            ivHangUp.visibility = View.INVISIBLE
                        }
                    }
                    if (mProgressLoadingDialog == null) {
                        mProgressLoadingDialog = CustomDialog.showLoadingProgress(requireContext())
                    }
                    mProgressLoadingDialog?.show()
                    mAiShareViewModel.mayReleaseEngine()
                    mMainHandler.removeCallbacksAndMessages(null)
                }
            })
            btnCalling.setOnClickListener(object : OnFastClickListener() {
                override fun onClickJacking(view: View) {
                    mBinding?.apply {
                        mAiShareViewModel.startVoiceChat()
                    }
                }
            })
            ivVoice.setOnClickListener(object : OnFastClickListener() {
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
            ivHangUp.setOnClickListener(object : OnFastClickListener() {
                override fun onClickJacking(view: View) {
                    mAiShareViewModel.stopVoiceChat {
                        mBinding?.apply {
                            videoProgress.isEnabled = false
                        }
                    }
                }
            })
            videoProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    seekBar?.progress?.let { progress ->
                        updateVideoText(progress)
                        mAiShareViewModel.seekPlay(progress * 1000)
                    }
                }
            })

            videoProgress.isEnabled = false
            videoLoading.isVisible = true
        }
    }

    private fun initUnityView() {
        val requireContext = context ?: return
        mTextureView = TextureView(requireContext)
        mMediaTextureView = TextureView(requireContext)
        mBinding?.layoutVideoContainer?.removeAllViews()
        val mediaLayoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        mBinding?.layoutVideoContainer?.removeAllViews()
        mBinding?.layoutVideoContainer?.addView(mMediaTextureView, 0, mediaLayoutParams)

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
                mAiShareViewModel.setVirtualHumanVendors()
                mBinding?.btnCalling?.text = resources.getString(R.string.calling, "AI主播")
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
}