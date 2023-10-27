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
import io.agora.gpt.R
import io.agora.gpt.databinding.CreateRoomFragmentBinding
import io.agora.gpt.ui.base.BaseFragment
import io.agora.gpt.ui.view.CustomDialog.Companion.getCustomView
import io.agora.gpt.ui.view.CustomDialog.Companion.showDownloadingChooser
import io.agora.gpt.ui.view.CustomDialog.Companion.showDownloadingProgress
import io.agora.gpt.ui.view.CustomDialog.Companion.showLoadingProgress
import io.agora.gpt.ui.view.OnFastClickListener
import io.agora.gpt.utils.KeyCenter
import io.agora.gpt.utils.LanguageUtil
import java.util.Locale
import java.util.Random

class CreateRoomFragment : BaseFragment() {


    private var binding: CreateRoomFragmentBinding? = null
    private val random = Random()
    private lateinit var nicknameArray: Array<String>
    private var downloadProgress = 0
    private var progressbarDialog: MaterialDialog? = null
    private var downloadingChooserDialog: MaterialDialog? = null
    private var progressLoadingDialog: AlertDialog? = null
    private var mTotalSize: Long = 0

    private val aiShareViewModel: AiShareViewModel by activityViewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return binding!!.root
    }

    override fun initContentView(inflater: LayoutInflater, container: ViewGroup?, attachToParent: Boolean) {
        super.initContentView(inflater, container, attachToParent)
        binding = CreateRoomFragmentBinding.inflate(inflater, container, attachToParent)
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

        downloadProgress = -1
        nicknameArray = resources.getStringArray(R.array.user_nickname)

        aiShareViewModel.mDownloadRes.observe(viewLifecycleOwner) {
            mTotalSize = it.totalSize
            downloadingChooserDialog?.let { dialog ->
                if (dialog.isShowing) dialog.dismiss()
                downloadingChooserDialog = null
            }
            downloadingChooserDialog = showDownloadingChooser(requireContext(), mTotalSize,
                { dialog: MaterialDialog? ->
                    aiShareViewModel.downloadRes()
                    downloadingChooserDialog = null
                },
                { dialog: MaterialDialog? ->
                    downloadProgress = -1
                    aiShareViewModel.cancelDownloadRes()
                    downloadingChooserDialog = null
                    progressLoadingDialog?.let { dialog ->
                        if (dialog.isShowing) dialog.dismiss()
                        progressLoadingDialog = null
                    }
                })
        }

        aiShareViewModel.mDownloadProgress.observe(viewLifecycleOwner) {
            if (it.progress >= 0) downloadProgress = it.progress
            if (progressbarDialog == null) {
                progressbarDialog = showDownloadingProgress(requireContext(), mTotalSize) {
                    downloadProgress = -1
                    aiShareViewModel.cancelDownloadRes()
                    progressLoadingDialog?.let { dialog ->
                        if (dialog.isShowing) dialog.dismiss()
                        progressLoadingDialog = null
                    }
                }
            }
            progressbarDialog?.let { dialog ->
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

        aiShareViewModel.mDownloadResFinish.observe(viewLifecycleOwner) {
            progressbarDialog?.let { dialog ->
                if (dialog.isShowing) dialog.dismiss()
                progressbarDialog = null
            }
            progressLoadingDialog?.let { dialog ->
                if (dialog.isShowing) dialog.dismiss()
                progressLoadingDialog = null
            }
            findNavController().navigate(R.id.action_createRoomFragment_to_aiRoomFragment)
        }
    }

    override fun initView() {
        super.initView()
        binding?.apply {
            btnAiPartner.isActivated = true
            etNickname.doAfterTextChanged {
                KeyCenter.setUserName(it.toString())
            }
            if (aiShareViewModel.currentLanguage() == Language.ZH_CN) {
                btnSwitchLanguage.setImageResource(R.drawable.icon_zh_to_en)
            } else {
                btnSwitchLanguage.setImageResource(R.drawable.icon_en_to_zh)
            }
        }
    }

    override fun initClickEvent() {
        super.initClickEvent()
        //防止多次频繁点击异常处理
        binding?.btnEnterRoom?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                if (TextUtils.isEmpty(KeyCenter.getUserName())) {
                    Toast.makeText(requireActivity(), R.string.enter_nickname, Toast.LENGTH_LONG).show()
                } else {
                    if (progressLoadingDialog == null) {
                        progressLoadingDialog = showLoadingProgress(requireContext())
                    }
                    progressLoadingDialog?.show()
                    aiShareViewModel.initAiEngine()
                }
            }
        })
        binding?.tvNicknameRandom?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                val nameIndex = random.nextInt(nicknameArray.size)
                binding?.etNickname?.setText(nicknameArray[nameIndex])
            }
        })

        binding?.btnSwitchLanguage?.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                aiShareViewModel.switchLanguage { language ->
                    if (language == Language.ZH_CN) {
                        binding?.btnSwitchLanguage?.setImageResource(R.drawable.icon_zh_to_en)
                        LanguageUtil.changeLanguage(requireContext(), "zh", "CN")
                    } else {
                        binding?.btnSwitchLanguage?.setImageResource(R.drawable.icon_en_to_zh)
                        LanguageUtil.changeLanguage(requireContext(), "en", "US")
                    }
                    activity?.recreate()
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onStart() {
        super.onStart()
        progressLoadingDialog?.let { dialog ->
            if (dialog.isShowing) dialog.dismiss()
            progressLoadingDialog = null
        }
        progressbarDialog?.let { dialog ->
            if (dialog.isShowing) dialog.dismiss()
            progressbarDialog = null
        }
        downloadingChooserDialog?.let { dialog ->
            if (dialog.isShowing) dialog.dismiss()
            downloadingChooserDialog = null
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }
}