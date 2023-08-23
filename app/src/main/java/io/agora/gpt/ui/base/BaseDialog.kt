package io.agora.gpt.ui.base

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.Window
import io.agora.gpt.R

abstract class BaseDialog constructor(context: Context) : Dialog(context, R.style.dialog_complete) {
    init {
        initConfig()
    }


    private fun initConfig() {
        setCanceledOnTouchOutside(false)
        setCancelable(true)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        initContentView()
        setGravity()
        initView()
    }


    protected abstract fun initContentView()

    protected abstract fun initView()

    protected open fun setGravity() {
        window?.attributes?.gravity = Gravity.CENTER
    }

    override fun dismiss() {
        if (isShowing) {
            super.dismiss()
        }
    }
}