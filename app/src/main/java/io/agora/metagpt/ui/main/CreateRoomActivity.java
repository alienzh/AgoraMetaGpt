package io.agora.metagpt.ui.main;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;

import io.agora.metagpt.R;
import io.agora.metagpt.utils.Constants;
import io.agora.metagpt.utils.Utils;

public class CreateRoomActivity extends AppCompatActivity {

    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog. Save the return value, an instance of
    // ActivityResultLauncher, as an instance variable.
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // features requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, CreateRoomFragment.newInstance())
                    .commitNow();
        }

        initFiles();
    }

    private void initFiles() {
        try {
            Utils.copyAssetFile(this, "bg_ai_female.png");
            Utils.copyAssetFile(this, "bg_ai_male.png");
        } catch (IOException e) {
            Log.d(Constants.TAG, "copyAssetFile error " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.getScreenHeight(this);
        handlePermission();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }

    private void handlePermission() {

        // 需要动态申请的权限
        String permission = Manifest.permission.READ_EXTERNAL_STORAGE;

        //查看是否已有权限
        int checkSelfPermission = ActivityCompat.checkSelfPermission(getApplicationContext(), permission);

        if (checkSelfPermission == PackageManager.PERMISSION_GRANTED) {
            //已经获取到权限  获取用户媒体资源

        } else {

            //没有拿到权限  是否需要在第二次请求权限的情况下
            // 先自定义弹框说明 同意后在请求系统权限(就是是否需要自定义DialogActivity)
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {

            } else {
                appRequestPermission();
            }
        }

    }

    private void appRequestPermission() {
        String[] permissions = new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
        };
        requestPermissions(permissions, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

        }
    }
}
