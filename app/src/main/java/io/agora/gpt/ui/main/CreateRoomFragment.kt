package io.agora.gpt.ui.main

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import io.agora.aigc.sdk.constants.Language
import io.agora.gpt.R
import io.agora.gpt.databinding.CreateRoomFragmentBinding
import io.agora.gpt.ui.base.BaseFragment
import io.agora.gpt.ui.view.CustomDialog.Companion.getCustomView
import io.agora.gpt.ui.view.CustomDialog.Companion.showDownloadingChooser
import io.agora.gpt.ui.view.CustomDialog.Companion.showDownloadingProgress
import io.agora.gpt.ui.view.CustomDialog.Companion.showLoadingProgress
import io.agora.gpt.ui.view.OnFastClickListener
import io.agora.gpt.ui.view.PickerChooseDialog
import io.agora.gpt.utils.Constant
import io.agora.gpt.utils.KeyCenter
import io.agora.gpt.utils.LanguageUtil
import io.agora.gpt.utils.ToastUtils
import io.agora.gpt.utils.statusBarHeight
import java.util.Locale
import java.util.Random

class CreateRoomFragment : BaseFragment() {


    private var mBinding: CreateRoomFragmentBinding? = null
    private val mRandom = Random()
    private lateinit var mNicknameArray: Array<String>
    private var mDownloadProgress = 0
    private var mProgressbarDialog: MaterialDialog? = null
    private var mDownloadingChooserDialog: MaterialDialog? = null
    private var mProgressLoadingDialog: AlertDialog? = null
    private var mTotalSize: Long = 0

    private val mAiShareViewModel: AiShareViewModel by activityViewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return mBinding!!.root
    }

    override fun initContentView(inflater: LayoutInflater, container: ViewGroup?, attachToParent: Boolean) {
        super.initContentView(inflater, container, attachToParent)
        mBinding = CreateRoomFragmentBinding.inflate(inflater, container, attachToParent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                activity?.finish()
            }
        })
        mBinding?.apply {
            val titleParams: ViewGroup.MarginLayoutParams = tvTitle.layoutParams as ViewGroup.MarginLayoutParams
            titleParams.topMargin = tvTitle.context.statusBarHeight
            tvTitle.layoutParams = titleParams
        }
    }

    override fun initData() {
        super.initData()

        mDownloadProgress = -1
        mNicknameArray = resources.getStringArray(R.array.user_nickname)

        mAiShareViewModel.mDownloadRes.observe(this) {
            mTotalSize = it.totalSize
            mDownloadingChooserDialog?.let { dialog ->
                if (dialog.isShowing) dialog.dismiss()
                mDownloadingChooserDialog = null
            }
            mDownloadingChooserDialog = showDownloadingChooser(requireContext(), mTotalSize,
                { dialog: MaterialDialog? ->
                    mAiShareViewModel.downloadRes()
                    mDownloadingChooserDialog = null
                },
                { dialog: MaterialDialog? ->
                    mDownloadProgress = -1
                    mAiShareViewModel.cancelDownloadRes()
                    mDownloadingChooserDialog = null
                    mProgressLoadingDialog?.let { dialog ->
                        if (dialog.isShowing) dialog.dismiss()
                        mProgressLoadingDialog = null
                    }
                })
        }

        mAiShareViewModel.mDownloadProgress.observe(this) {
            if (it.progress >= 0) mDownloadProgress = it.progress
            if (mProgressbarDialog == null) {
                mProgressbarDialog = showDownloadingProgress(requireContext(), mTotalSize) {
                    mDownloadProgress = -1
                    mAiShareViewModel.cancelDownloadRes()
                    mProgressLoadingDialog?.let { dialog ->
                        if (dialog.isShowing) dialog.dismiss()
                        mProgressLoadingDialog = null
                    }
                }
            }
            mProgressbarDialog?.let { dialog ->
                if (!dialog.isShowing) dialog.show()
                val constraintLayout = getCustomView<ConstraintLayout>(dialog)
                val progressBar = constraintLayout.findViewById<ProgressBar>(R.id.progressBar)
                val textView = constraintLayout.findViewById<TextView>(R.id.textView)
                val countView = constraintLayout.findViewById<TextView>(R.id.count)
                progressBar.progress = it.progress
                textView.text = String.format(Locale.getDefault(), "%d%%", it.progress)
                countView.text = "${it.index}/${it.count}"
            }
        }

        mAiShareViewModel.mDownloadResFinish.observe(this) {
            mProgressbarDialog?.let { dialog ->
                if (dialog.isShowing) dialog.dismiss()
                mProgressbarDialog = null
            }
            mProgressLoadingDialog?.let { dialog ->
                if (dialog.isShowing) dialog.dismiss()
                mProgressLoadingDialog = null
            }
            if (findNavController().currentDestination?.id == R.id.crateRoomFragment) {
                findNavController().navigate(R.id.action_createRoomFragment_to_aiRoomFragment)
            }
        }

        mAiShareViewModel.mAIEngineInit.observe(this) {
            if (it) {
                setupRestRole()
            }
        }
        mAiShareViewModel.initAiEngine()
    }

    private fun setupScene(currentScene: String) {
        mBinding?.apply {
            btnAiPartner.isActivated = false
            btnAiGame.isActivated = false
            when (currentScene) {
                Constant.Scene_AI_Game -> {
                    btnAiGame.isActivated = true
                    groupGame.isVisible = true
                }

                else -> {
                    btnAiPartner.isActivated = true
                    groupGame.isVisible = false
                }
            }
        }

    }

    override fun initView() {
        super.initView()
        mBinding?.apply {
            etNickname.doAfterTextChanged {
                KeyCenter.mUserName = it.toString()
            }
            if (mAiShareViewModel.currentLanguage() == Language.ZH_CN) {
                btnSwitchLanguage.setImageResource(R.drawable.icon_zh_to_en)
                groupGame.isVisible = true
                btnAiGame.isVisible = true
            } else {
                btnSwitchLanguage.setImageResource(R.drawable.icon_en_to_zh)
                groupGame.isVisible = false
                btnAiGame.isVisible = false
                // 英文只有 AI 伴侣
                mAiShareViewModel.mCurrentScene = Constant.Scene_AI_Partner
            }
            setupScene(mAiShareViewModel.mCurrentScene)
        }
        mBinding?.btnAiPartner?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                mAiShareViewModel.mCurrentScene = Constant.Scene_AI_Partner
                setupScene(mAiShareViewModel.mCurrentScene)
                setupRestRole()
            }
        })
        mBinding?.btnAiGame?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                mAiShareViewModel.mCurrentScene = Constant.Scene_AI_Game
                setupScene(mAiShareViewModel.mCurrentScene)
                setupRestRole()
            }
        })
        mBinding?.btnEnterRoom?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                if (TextUtils.isEmpty(KeyCenter.mUserName)) {
                    Toast.makeText(requireActivity(), R.string.enter_nickname, Toast.LENGTH_LONG).show()
                } else {
                    if (mAiShareViewModel.isAIGCEngineInit()) {
                        if (mProgressLoadingDialog == null) {
                            mProgressLoadingDialog = showLoadingProgress(requireContext())
                        }
                        mProgressLoadingDialog?.show()
                        mAiShareViewModel.checkDownloadRes()
                    } else {
                        ToastUtils.showToast("Wait for AIGC engine initialization to complete!")
                    }
                }
            }
        })
        mBinding?.tvNicknameRandom?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                val nameIndex = mRandom.nextInt(mNicknameArray.size)
                mBinding?.etNickname?.setText(mNicknameArray[nameIndex])
            }
        })

        mBinding?.btnSwitchLanguage?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                if (mAiShareViewModel.isAIGCEngineInit()) {
                    mAiShareViewModel.switchLanguage { language ->
                        if (language == Language.ZH_CN) {
                            LanguageUtil.changeLanguage(requireContext(), "zh", "CN")
                        } else {
                            LanguageUtil.changeLanguage(requireContext(), "en", "US")
                        }
                        setupRestRole()
                        activity?.recreate()
                    }
                } else {
                    ToastUtils.showToast("Wait for AIGC engine initialization to complete!")
                }
            }
        })
        mBinding?.btnChooseGame?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                val aiRoles = mAiShareViewModel.getUsableAiRoles()
                val chooseDialog = PickerChooseDialog(requireContext())
                chooseDialog.setDatas(aiRoles.map { it.profession })
                chooseDialog.setConfirmCallback { selected ->
                    mBinding?.tvChooseGameContent?.text = selected
                    aiRoles.find { it.profession == selected }?.let { aiRole ->
                        mAiShareViewModel.setAiRole(aiRole)
                    }
                }
                chooseDialog.show()
            }
        })
    }

    private fun setupRestRole() {
        val aiRoles = mAiShareViewModel.getUsableAiRoles()
        if (aiRoles.isEmpty()) {
            ToastUtils.showToast("No games are available!")
        } else {
            val aiRole = if (mAiShareViewModel.isAiGame()) mAiShareViewModel.mLastAiRole ?: aiRoles[0] else aiRoles[0]
            mAiShareViewModel.setAiRole(aiRole)
            if (mAiShareViewModel.isAiGame()) {
                mBinding?.tvChooseGameContent?.text = aiRole.profession
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mProgressLoadingDialog?.let { dialog ->
            if (dialog.isShowing) dialog.dismiss()
            mProgressLoadingDialog = null
        }
        mProgressbarDialog?.let { dialog ->
            if (dialog.isShowing) dialog.dismiss()
            mProgressbarDialog = null
        }
        mDownloadingChooserDialog?.let { dialog ->
            if (dialog.isShowing) dialog.dismiss()
            mDownloadingChooserDialog = null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }
}