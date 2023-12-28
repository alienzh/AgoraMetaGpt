package io.agora.gpt.ui.view

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import io.agora.gpt.databinding.TopicInputDialogBinding
import io.agora.gpt.ui.base.BaseDialog

class TopicInputDialog constructor(val context: Activity) : BaseDialog(context) {

    private lateinit var binding: TopicInputDialogBinding


    private var inputTextCallback: ((text: String) -> Unit)? = null

    fun setInputTextCallback(callback: ((text: String) -> Unit)) {
        this.inputTextCallback = callback
    }

    override fun initContentView() {
        binding = TopicInputDialogBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun setContentView(view: View) {
        super.setContentView(view)
        window?.let { window ->
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.setDimAmount(0f)
//            window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_PANEL)
//            window.setFlags(
//                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
//
//            )
//            window.setFlags(
//                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
//            )
            window.attributes.apply {
                val lp = WindowManager.LayoutParams()
                lp.copyFrom(window.attributes)
                lp.width = WindowManager.LayoutParams.MATCH_PARENT
                lp.height = WindowManager.LayoutParams.MATCH_PARENT
                window.attributes = lp
            }
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        }
        setCanceledOnTouchOutside(true)
        setOnShowListener {
            binding.root.postDelayed({
                showKeyboard(context, binding.etTopic)
            }, 200)
        }
    }

    override fun initView() {
        binding.btnOk.setOnClickListener {
            inputTextCallback?.invoke(binding.etTopic.text.toString())
            dismiss()
        }


    }

    override fun setGravity() {
        window?.attributes?.gravity = Gravity.BOTTOM
    }
}
