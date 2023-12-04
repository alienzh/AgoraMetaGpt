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
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import io.agora.aigc.sdk.constants.Language
import io.agora.aigc.sdk.constants.ServiceCode
import io.agora.aigc.sdk.constants.ServiceEvent
import io.agora.gpt.R
import io.agora.gpt.databinding.CreateRoomFragmentBinding
import io.agora.gpt.ui.base.BaseFragment
import io.agora.gpt.ui.view.ChooseDialog
import io.agora.gpt.ui.view.CustomDialog.Companion.getCustomView
import io.agora.gpt.ui.view.CustomDialog.Companion.showDownloadingChooser
import io.agora.gpt.ui.view.CustomDialog.Companion.showDownloadingProgress
import io.agora.gpt.ui.view.CustomDialog.Companion.showLoadingProgress
import io.agora.gpt.ui.view.OnFastClickListener
import io.agora.gpt.utils.KeyCenter
import io.agora.gpt.utils.LanguageUtil
import io.agora.gpt.utils.ToastUtils
import java.util.Locale
import java.util.Random

class CreateRoomFragment : BaseFragment() {

    private var mBinding: CreateRoomFragmentBinding? = null
    private val mRandom = Random()
    private lateinit var mNicknameArray: Array<String>
    private lateinit var mLanguageArray: Array<String>

    private var mDownloadProgress = 0
    private var mProgressbarDialog: MaterialDialog? = null
    private var mDownloadingChooserDialog: MaterialDialog? = null
    private var mProgressLoadingDialog: AlertDialog? = null
    private var mTotalSize: Long = 0
    private var mChangeLanguage = false

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
    }

    override fun initData() {
        super.initData()

        mDownloadProgress = -1
        mNicknameArray = resources.getStringArray(R.array.user_nickname)
        mLanguageArray = resources.getStringArray(R.array.choose_language)

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
                val aiRoles = mAiShareViewModel.getUsableAiRoles()
                if (aiRoles.isEmpty()) {
                    ToastUtils.showToast("No roles are available!")
                } else {
                    val aiRole = aiRoles[0]
                    mAiShareViewModel.setAiRole(aiRole)
                    mBinding?.tvChooseRoleContent?.text = aiRole.profession
                }
            }
        }
        mAiShareViewModel.mUpdateConfigResult.observe(this) {
            if (it && mChangeLanguage) {
                val aiRoles = mAiShareViewModel.getUsableAiRoles()
                if (aiRoles.isEmpty()) {
                    ToastUtils.showToast("No roles are available!")
                } else {
                    val aiRole = aiRoles[0]
                    mAiShareViewModel.setAiRole(aiRole)
                    mBinding?.tvChooseRoleContent?.text = aiRole.profession
                }
                mChangeLanguage = false
            }
        }

        mAiShareViewModel.initAiEngine()
    }

    private fun getLanguageText(language: Language): String {
        mLanguageArray.forEach {
            if (language == Language.ZH_CN && it.startsWith("Ch")) {
                return it
            } else if (language == Language.EN_US && it.startsWith("En")) {
                return it
            } else if (language == Language.JA_JP && it.startsWith("Ja")) {
                return it
            }
        }

        return mLanguageArray[0]
    }

    override fun initView() {
        super.initView()
        mBinding?.apply {
            btnAiPartner.isActivated = true
            etNickname.doAfterTextChanged {
                KeyCenter.userName = it.toString()
            }
            val language = mAiShareViewModel.currentLanguage()
            tvChooseLanguageContent.text = getLanguageText(language)
        }

        mBinding?.btnEnterRoom?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                if (TextUtils.isEmpty(KeyCenter.userName)) {
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

        mBinding?.btnChooseRole?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                val aiRoles = mAiShareViewModel.getUsableAiRoles()
                val chooseDialog = ChooseDialog(requireContext())
                chooseDialog.setDatas(aiRoles.map { it.profession })
                chooseDialog.setConfirmCallback { selected ->
                    mBinding?.tvChooseRoleContent?.text = selected
                    aiRoles.find { it.roleName == selected }?.let { aiRole ->
                        mAiShareViewModel.setAiRole(aiRole)
                    }
                }
                chooseDialog.show()
            }
        })
        mBinding?.btnChooseLanguage?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                val languages = mLanguageArray.asList()
                val chooseDialog = ChooseDialog(requireContext())
                chooseDialog.setDatas(languages)
                chooseDialog.setConfirmCallback { selected ->
                    mBinding?.tvChooseLanguageContent?.text = selected
                    val language = when (selected) {
                        "Chinese" -> Language.ZH_CN
                        "English" -> Language.EN_US
                        else -> Language.JA_JP
                    }
                    mChangeLanguage = true
                    mAiShareViewModel.switchLanguage(language) {

                    }
                }
                chooseDialog.show()

            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
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

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }
}