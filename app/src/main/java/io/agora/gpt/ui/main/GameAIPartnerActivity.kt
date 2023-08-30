package io.agora.gpt.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.agora.gpt.databinding.MainActivityBinding
import io.agora.gpt.ui.base.BaseActivity
import io.agora.gpt.utils.Utils

class GameAIPartnerActivity : BaseActivity() {

    companion object {
        private val TAG = "GameAIPartnerActivity"
    }

    private lateinit var binding: MainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.keepScreenOn = true
        ViewCompat.setOnApplyWindowInsetsListener(binding.navMchatMain) { v: View?, insets: WindowInsetsCompat ->
            val inset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.setPaddingRelative(inset.left, 0, inset.right, inset.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }

    override fun initContentView() {
        super.initContentView()
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun initData() {
        super.initData()
    }

    override fun initView() {
        super.initView()
    }

    override fun initClickEvent() {
        super.initClickEvent()
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "onResume")
        Utils.getScreenHeight(this)
        handlePermission()
    }
    private fun handlePermission() {

        // 需要动态申请的权限
        val permission = Manifest.permission.READ_EXTERNAL_STORAGE

        //查看是否已有权限
        val checkSelfPermission = ActivityCompat.checkSelfPermission(applicationContext, permission)
        if (checkSelfPermission == PackageManager.PERMISSION_GRANTED) {
            //已经获取到权限  获取用户媒体资源
        } else {

            //没有拿到权限  是否需要在第二次请求权限的情况下
            // 先自定义弹框说明 同意后在请求系统权限(就是是否需要自定义DialogActivity)
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            } else {
                appRequestPermission()
            }
        }
    }

    private fun appRequestPermission() {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )
        requestPermissions(permissions, 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy")
    }
}