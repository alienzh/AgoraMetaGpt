package io.agora.gpt

import android.app.Application
import android.util.Log
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.LogLevel
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.AndroidPrinter
import com.elvishew.xlog.printer.Printer
import com.elvishew.xlog.printer.file.FilePrinter
import com.elvishew.xlog.printer.file.backup.NeverBackupStrategy
import com.elvishew.xlog.printer.file.naming.DateFileNameGenerator
import io.agora.gpt.utils.Utils
import java.io.IOException

class MainApplication : Application() {

    companion object {
        lateinit var mGlobalApplication: MainApplication
    }
    override fun onCreate() {
        super.onCreate()
        mGlobalApplication = this
        init()
    }

    private fun init() {
        val config = LogConfiguration.Builder()
            .logLevel(if (BuildConfig.DEBUG) LogLevel.ALL else LogLevel.NONE)
            .tag("MainApplication")
            .build()
        val androidPrinter: Printer = AndroidPrinter(true)
        val filePrinter: Printer = FilePrinter.Builder(
            mGlobalApplication.externalCacheDir!!.path + "/log/"
        )
            .fileNameGenerator(DateFileNameGenerator())
            .backupStrategy(NeverBackupStrategy())
            .build()
        XLog.init(config, androidPrinter, filePrinter)
        initFiles()
    }

    private fun initFiles() {
        try {
            Utils.copyAssetFile(this, "bg_ai_female.png")
            Utils.copyAssetFile(this, "bg_ai_male.png")
        } catch (e: IOException) {
            Log.d("MainApplication", "copyAssetFile error " + e.message)
        }
    }
}