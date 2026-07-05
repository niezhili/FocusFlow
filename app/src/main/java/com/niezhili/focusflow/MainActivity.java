package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.core.content.ContextCompat;

/**
 * MainActivity - 入口 Activity
 *
 * 请求通知权限（Android 13+），启动 Compose 主界面
 */
public class MainActivity extends ComponentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Android 13+ 请求通知权限
        requestNotificationPermissionIfNeeded();

        // 启动 Compose 界面
        ComposeEntryKt.setFocusFlowContent(this);
    }

    /**
     * Android 13 (API 33) 及以上请求通知权限
     */
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    0
                );
            }
        }
    }
}