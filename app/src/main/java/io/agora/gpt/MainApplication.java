package io.agora.gpt;

import android.app.Application;

import com.elvishew.xlog.LogConfiguration;
import com.elvishew.xlog.LogLevel;
import com.elvishew.xlog.XLog;
import com.elvishew.xlog.printer.AndroidPrinter;
import com.elvishew.xlog.printer.Printer;
import com.elvishew.xlog.printer.file.FilePrinter;
import com.elvishew.xlog.printer.file.backup.NeverBackupStrategy;
import com.elvishew.xlog.printer.file.naming.DateFileNameGenerator;
import com.tencent.mmkv.MMKV;

import io.agora.gpt.utils.Constants;

public class MainApplication extends Application {

    public static MainApplication mGlobalApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        mGlobalApplication = this;

        init();
    }

    private void init() {
        MMKV.initialize(mGlobalApplication);


        LogConfiguration config = new LogConfiguration.Builder()
                .logLevel(BuildConfig.DEBUG ? LogLevel.ALL
                        : LogLevel.NONE)
                .tag(Constants.TAG)
                .build();

        Printer androidPrinter = new AndroidPrinter(true);
        Printer filePrinter = new FilePrinter
                .Builder(mGlobalApplication.getExternalCacheDir().getPath() + "/log/")
                .fileNameGenerator(new DateFileNameGenerator())
                .backupStrategy(new NeverBackupStrategy())
                .build();

        XLog.init(config, androidPrinter, filePrinter);

    }

}
