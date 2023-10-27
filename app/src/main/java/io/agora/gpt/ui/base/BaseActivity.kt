package io.agora.gpt.ui.base

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import io.agora.gpt.utils.Constant
import io.agora.gpt.utils.LanguageUtil
import io.agora.gpt.utils.SPUtil

open class BaseActivity : AppCompatActivity() {

    protected var mIsFront = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTransparent(window)
        adaptAndroidP(window)
        setLanguage()
        initContentView()
        initData()
        initView()
        initListener()
        initClickEvent()
    }

    private fun setLanguage() {
        val currentCountry = SPUtil.get(Constant.CURRENT_COUNTRY, "CN") as String
        val currentLanguage = SPUtil.get(Constant.CURRENT_LANGUAGE, "zh") as String
        LanguageUtil.changeLanguage(this, currentLanguage, currentCountry)
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        initData()
        initView()
    }

    protected open fun initClickEvent() {}
    protected open fun initContentView() {}
    protected open fun initView() {}
    protected open fun initData() {
        mIsFront = false
    }

    protected fun initListener() {}
    override fun onResume() {
        super.onResume()
        mIsFront = true
    }

    override fun onPause() {
        super.onPause()
        mIsFront = false
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    //重写Activity该方法，当窗口焦点变化时自动隐藏system bar，这样可以排除在弹出dialog和menu时，
    //system bar会重新显示的问题(弹出dialog时似乎还是可以重新显示的0.0)。
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        val decorView = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY //(修改这个选项，可以设置不同模式)
                //使用下面三个参数，可以使内容显示在system bar的下面，防止system bar显示或
                //隐藏时，Activity的大小被resize。
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN // 隐藏导航栏和状态栏
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    companion object {
        /**
         * 1.使状态栏透明
         */
        private fun setTransparent(window: Window) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            window.statusBarColor = Color.TRANSPARENT
        }

        private fun adaptAndroidP(window: Window) {
            // 适配刘海屏,全屏去黑状态栏
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val layoutParams = window.attributes
                layoutParams.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                window.attributes = layoutParams
            }
        }
    }
}