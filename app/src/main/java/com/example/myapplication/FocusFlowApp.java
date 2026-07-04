package com.example.myapplication;

import android.app.Application;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.util.NotificationHelper;

/**
 * FocusFlowApp - 应用根组件
 *
 * 初始化 Room 数据库单例和通知渠道
 */
public class FocusFlowApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化数据库（首次调用触发 Room 建表）
        AppDatabase.getInstance(this);
        // 创建通知渠道（Android 8.0+）
        new NotificationHelper(this).createChannel();
    }
}