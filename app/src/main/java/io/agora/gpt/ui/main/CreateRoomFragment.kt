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
import com.jakewharton.rxbinding2.view.RxView
import io.agora.ai.sdk.AIEngineAction
import io.agora.ai.sdk.AIEngineCode
import io.agora.ai.sdk.Constants
import io.agora.gpt.R
import io.agora.gpt.databinding.CreateRoomFragmentBinding
import io.agora.gpt.ui.base.BaseFragment
import io.agora.gpt.ui.view.CustomDialog.Companion.getCustomView
import io.agora.gpt.ui.view.CustomDialog.Companion.showDownloadingChooser
import io.agora.gpt.ui.view.CustomDialog.Companion.showDownloadingProgress
import io.agora.gpt.ui.view.CustomDialog.Companion.showLoadingProgress
import io.agora.gpt.utils.KeyCenter
import io.agora.gpt.utils.LanguageUtil
import io.reactivex.disposables.Disposable
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale
import java.util.Random
import java.util.concurrent.TimeUnit

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

        aiShareViewModel.downloadProgress.observe(viewLifecycleOwner) {
            if (it.progress >= 0) downloadProgress = it.progress
            if (progressbarDialog == null) {
                progressbarDialog = showDownloadingProgress(requireContext(), mTotalSize) {
                    downloadProgress = -1
                    aiShareViewModel.cancelDownloadRes()
                    progressLoadingDialog?.let { dialog ->
                        if (dialog.isShowing) dialog.dismiss()
                        progressLoadingDialog = null
                    }
                    null
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
                if (it.index == it.count && it.progress == 100) { // 下载完成
                    dialog.dismiss()
                    progressbarDialog = null
                }
            }

        }
        aiShareViewModel.actionResultModel.observe(viewLifecycleOwner) {
            if (AIEngineAction.DOWNLOAD == it.vcAction) {
                if (AIEngineCode.SUCCESS == it.vcEngineCode) {
                    progressbarDialog?.let { dialog ->
                        if (dialog.isShowing) dialog.dismiss()
                        progressbarDialog = null
                    }
                    progressLoadingDialog?.let { dialog ->
                        if (dialog.isShowing) dialog.dismiss()
                        progressLoadingDialog = null
                    }
                    findNavController().navigate(R.id.action_createRoomFragment_to_aiRoomFragment)
                } else if (AIEngineCode.DOWNLOAD_RES == it.vcEngineCode) {
                    try {
                        it.extraInfo?.apply {
                            val jsonObject = JSONObject(this)
                            if (jsonObject.has("fileTotalSize")) {
                                mTotalSize = jsonObject.getLong("fileTotalSize")
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        mTotalSize = 99999
                    }
                    downloadingChooserDialog?.let { dialog ->
                        if (dialog.isShowing) dialog.dismiss()
                        downloadingChooserDialog = null
                    }
                    downloadingChooserDialog =
                        showDownloadingChooser(requireContext(), mTotalSize,
                            { dialog: MaterialDialog? ->
                                aiShareViewModel.downloadRes()
                                downloadingChooserDialog = null
                                null
                            },
                            { dialog: MaterialDialog? ->
                                downloadProgress = -1
                                aiShareViewModel.cancelDownloadRes()
                                downloadingChooserDialog = null
                                progressLoadingDialog?.let { dialog ->
                                    if (dialog.isShowing) dialog.dismiss()
                                    progressLoadingDialog = null
                                }
                                null
                            })
                }
            } else {

            }
        }
    }

    override fun initView() {
        super.initView()
        binding?.apply {
            btnAiPartner.isActivated = true
            etNickname.doAfterTextChanged {
                KeyCenter.setUserName(it.toString())
            }
            if (aiShareViewModel.currentLanguage() == Constants.LANG_ZH_CN) {
                btnSwitchLanguage.setImageResource(R.drawable.icon_zh_to_en)
            } else {
                btnSwitchLanguage.setImageResource(R.drawable.icon_en_to_zh)
            }
        }
    }

    override fun initClickEvent() {
        super.initClickEvent()
        //防止多次频繁点击异常处理
        binding?.apply {
            var disposable: Disposable = RxView.clicks(btnEnterRoom)
                .throttleFirst(1, TimeUnit.SECONDS)
                .subscribe { o: Any? ->
                    if (TextUtils.isEmpty(KeyCenter.getUserName())) {
                        Toast.makeText(requireActivity(), R.string.enter_nickname, Toast.LENGTH_LONG).show()
                    } else {
                        if (progressLoadingDialog == null) {
                            progressLoadingDialog = showLoadingProgress(requireContext())
                        }
                        progressLoadingDialog?.show()
                        aiShareViewModel.initAiEngine(requireActivity())
                        aiShareViewModel.checkDownloadRes()
                    }
                }
            compositeDisposable.add(disposable)

            disposable = RxView.clicks(tvNicknameRandom)
                .throttleFirst(1, TimeUnit.SECONDS).subscribe { o: Any? ->
                    val nameIndex = random.nextInt(nicknameArray.size)
                    etNickname.setText(nicknameArray[nameIndex])
                }
            compositeDisposable.add(disposable)

            disposable = RxView.clicks(btnSwitchLanguage)
                .throttleFirst(1, TimeUnit.SECONDS).subscribe { o: Any? ->
                    aiShareViewModel.switchLanguage { language ->
                        if (language == Constants.LANG_ZH_CN) {
                            btnSwitchLanguage.setImageResource(R.drawable.icon_zh_to_en)
                            LanguageUtil.changeLanguage(requireContext(), "zh", "CN")
                        } else {
                            btnSwitchLanguage.setImageResource(R.drawable.icon_en_to_zh)
                            LanguageUtil.changeLanguage(requireContext(), "en", "US")
                        }
                        activity?.recreate()
                    }
                }
            compositeDisposable.add(disposable)
        }

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