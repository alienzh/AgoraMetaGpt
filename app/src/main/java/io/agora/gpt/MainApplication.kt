package io.agora.gpt

import android.app.Application
import android.util.Log
import io.agora.gpt.utils.Utils
import java.io.IOException

class MainApplication : Application() {

    companion object {
        lateinit var mGlobalApplication: MainApplication
    }
    override fun onCreate() {
        super.onCreate()
        mGlobalApplication = this
        initFiles()
    }

    private fun initFiles() {
        try {
            Utils.copyAssetFile(this, "ai_avatar1.png")
            Utils.copyAssetFile(this, "ai_avatar2.png")
            Utils.copyAssetFile(this, "ai_avatar3.png")
        } catch (e: IOException) {
            Log.d("MainApplication", "copyAssetFile error " + e.message)
        }
    }
}