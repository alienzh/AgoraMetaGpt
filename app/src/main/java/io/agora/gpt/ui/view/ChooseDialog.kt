package io.agora.gpt.ui.view

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import io.agora.aigc.sdk.model.AIRole
import io.agora.gpt.databinding.DialogChooseRoleBinding
import io.agora.gpt.ui.base.BaseDialog

class ChooseDialog constructor(context: Context) : BaseDialog(context) {

    private lateinit var binding: DialogChooseRoleBinding


    private var confirmCallback: ((aiRole: AIRole) -> Unit)? = null

    fun setConfirmCallback(callback: ((aiRole: AIRole) -> Unit)) {
        this.confirmCallback = callback
    }

    private var mRole:AIRole?=null

    override fun initContentView() {
        binding = DialogChooseRoleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.pickScrollview.setOnSelectListener {
            mRole = it
            Log.d("zhangw", "pick select $it")
        }
    }

    fun setDatas(aiRoles:List<AIRole>){
        mRole = aiRoles[0]
        binding.pickScrollview.setData(aiRoles)
        binding.pickScrollview.setSelected(0)
    }

    override fun setContentView(view: View) {
        super.setContentView(view)
        window?.let { window ->
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.setDimAmount(0f)
            window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_PANEL)
//            window.setFlags(
//                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
//            )
//            window.setFlags(
//                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
//            )
            window.attributes.apply {
                val lp = WindowManager.LayoutParams()
                lp.copyFrom(window.attributes)
                lp.width = WindowManager.LayoutParams.MATCH_PARENT
                lp.height = WindowManager.LayoutParams.WRAP_CONTENT
                window.attributes = lp
            }
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        }
        setCanceledOnTouchOutside(true)
    }

    override fun initView() {
        binding.btnOk.setOnClickListener {
            mRole?.let { aiRole ->
                confirmCallback?.invoke(aiRole)
            }
            dismiss()
        }
    }

    override fun setGravity() {
        window?.attributes?.gravity = Gravity.BOTTOM
    }
}
