package io.agora.gpt.utils;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.InputMethodManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.agora.gpt.MainApplication;

public class Utils {
    /**
     * 将dip或dp值转换为px值，保证尺寸大小不变
     */

    private static int mScreenHeight;

    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public static String getFromAssets(Context context, String fileName) {
        if (null == context || TextUtils.isEmpty(fileName)) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        try (InputStreamReader inputReader = new InputStreamReader(context.getResources().getAssets().open(fileName));
             BufferedReader bufReader = new BufferedReader(inputReader)) {

            String line;
            while ((line = bufReader.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    public static void showSoftInput(View view) {
        if (view == null)
            return;
        InputMethodManager inputMethodManager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.showSoftInput(view, 0);
        }
    }

    public static void hideSoftInput(View view) {
        if (view == null)
            return;
        InputMethodManager inputMethodManager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * 拷贝asset目录下所有文件到指定路径
     *
     * @param context    context
     * @param assetsPath asset目录
     * @param savePath   目标目录
     */
    public static void copyFilesFromAssets(Context context, String assetsPath, String savePath) {
        try {
            // 获取assets指定目录下的所有文件
            String[] fileList = context.getAssets().list(assetsPath);
            if (fileList != null && fileList.length > 0) {
                File file = new File(savePath);
                // 如果目标路径文件夹不存在，则创建
                if (!file.exists()) {
                    if (!file.mkdirs()) {
                        return;
                    }
                }
                for (String fileName : fileList) {
                    copyFileFromAssets(context, assetsPath + "/" + fileName, savePath, fileName);
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 拷贝asset文件到指定路径，可变更文件名
     *
     * @param context   context
     * @param assetName asset文件
     * @param savePath  目标路径
     * @param saveName  目标文件名
     */
    public static void copyFileFromAssets(Context context, String assetName, String savePath, String saveName) {
        // 若目标文件夹不存在，则创建
        File dir = new File(savePath);
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                return;
            }
        }

        // 拷贝文件
        String filename = savePath + "/" + saveName;
        File file = new File(filename);
        if (!file.exists()) {
            try (InputStream inStream = context.getAssets().open(assetName);
                 FileOutputStream fileOutputStream = new FileOutputStream(filename)) {
                int byteread;
                byte[] buffer = new byte[1024];
                while ((byteread = inStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, byteread);
                }
                fileOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //获取虚拟按键的高度
    public static int getNavigationBarHeight(Context context) {
        int result = 0;
        if (hasNavBar(context)) {
            Resources res = context.getResources();
            int resourceId = res.getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                result = res.getDimensionPixelSize(resourceId);
            }
        }
        return result;
    }

    /**
     * 检查是否存在虚拟按键栏
     */
    public static boolean hasNavBar(Context context) {
        Resources res = context.getResources();
        int resourceId = res.getIdentifier("config_showNavigationBar", "bool", "android");
        if (resourceId != 0) {
            boolean hasNav = res.getBoolean(resourceId);
            // check override flag
            String sNavBarOverride = getNavBarOverride();
            if ("1".equals(sNavBarOverride)) {
                hasNav = false;
            } else if ("0".equals(sNavBarOverride)) {
                hasNav = true;
            }
            return hasNav;
        } else { // fallback
            return !ViewConfiguration.get(context).hasPermanentMenuKey();
        }
    }

    /**
     * 判断虚拟按键栏是否重写
     */
    private static String getNavBarOverride() {
        String sNavBarOverride = null;
        try {
            @SuppressLint("PrivateApi") Class<?> c = Class.forName("android.os.SystemProperties");
            Method m = c.getDeclaredMethod("get", String.class);
            m.setAccessible(true);
            sNavBarOverride = (String) m.invoke(null, "qemu.hw.mainkeys");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return sNavBarOverride;
    }

    public static int getScreenHeight(Activity activity) {
        if (null == activity) {
            return mScreenHeight;
        }
        if (0 != mScreenHeight) {
            return mScreenHeight;
        }
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        @SuppressWarnings("rawtypes")
        Class c;
        try {
            c = Class.forName("android.view.Display");
            @SuppressWarnings("unchecked")
            Method method = c.getMethod("getRealMetrics", DisplayMetrics.class);
            method.invoke(display, dm);
            mScreenHeight = dm.heightPixels;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mScreenHeight;
    }

    public static int getResId(String name, Context context) {
        Resources r = context.getResources();
        return r.getIdentifier(name, "drawable", context.getPackageName());
    }

    public static String[] stringToStringArray(String src, int length) {
        //检查参数是否合法
        if (TextUtils.isEmpty(src)) {
            return new String[]{""};
        }

        int n = (src.length() + length - 1) / length;
        String[] split = new String[n];
        for (int i = 0; i < n; i++) {
            if (i < (n - 1)) {
                split[i] = src.substring(i * length, (i + 1) * length);
            } else {
                split[i] = src.substring(i * length);
            }
        }
        return split;
    }

    public static int getNumberFromStr(String str) {
        int number = 0;
        String pattern = "\\d+";
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(str);
        if (matcher.find()) {
            try {
                number = Integer.parseInt(matcher.group());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return number;
        }
        return number;
    }

    public static boolean isByteArrayAllZero(byte[] bytes) {
        for (byte b : bytes) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    public static byte[] readFileToByteArray(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return null;
        }
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            long inSize = in.getChannel().size();
            if (inSize == 0) {
                return null;
            }

            byte[] buffer = new byte[in.available()];
            in.read(buffer);
            return buffer;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (null != in) {
                    in.close();
                }
            } catch (IOException e) {
                return null;
            }
        }
    }

    public static String stringArrayToString(String[] stringArray) {
        StringBuilder sb = new StringBuilder();
        for (String s : stringArray) {
            sb.append(s);
        }
        return sb.toString();
    }

    public static String removeDuplicates(String input, String target) {
        StringBuilder result = new StringBuilder();
        int index = input.indexOf(target);
        boolean isFirst = true;
        while (index >= 0) {
            if (isFirst) {
                result.append(input, 0, index + target.length());
                input = input.substring(index + target.length());
            } else {
                result.append(input, 0, index);
                if (input.length() >= index + target.length() + 1) {
                    input = input.substring(index + target.length() + 1);
                } else {
                    input = input.substring(index + target.length());
                }
            }
            index = input.indexOf(target);
            isFirst = false;
        }
        result.append(input);
        return result.toString();
    }


    public static String getRandomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(62);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    public static void copyAssetFile(Context context, String fileName) throws IOException {
        InputStream inputStream = context.getResources().getAssets().open(fileName);
        File out = new File(context.getExternalCacheDir().getPath() + File.separator + fileName);
        OutputStream outputStream = new FileOutputStream(out);
        byte[] buffer = new byte[10240];
        while (true) {
            int len = inputStream.read(buffer);
            if (len < 0) break;
            outputStream.write(buffer, 0, len);
        }
        outputStream.flush();
        outputStream.close();
        inputStream.close();
    }

    public static String getSessionId() {
        return UUID.randomUUID().toString();
    }

    public static String getCacheFilePath(String fileName){
       return MainApplication.mGlobalApplication.getExternalCacheDir().getPath() + File.separator + fileName;
    }
}
