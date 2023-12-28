package io.agora.gpt.ui.base

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
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

    protected open fun hideKeyboard(editText: EditText) {
        editText.clearFocus()
        val context: Activity = ownerActivity ?: return
        // 隐藏软键盘
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editText.windowToken, 0)
    }

    protected open fun showKeyboard(activity: Activity?, editText: EditText?) {
        val context: Activity = activity ?: ownerActivity ?: return
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, 0)
    }
}