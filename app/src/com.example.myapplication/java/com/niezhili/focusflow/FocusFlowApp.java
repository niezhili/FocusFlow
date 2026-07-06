package com.niezhili.focusflow;

import android.app.Application;

import com.niezhili.focusflow.data.local.AppDatabase;
import com.niezhili.focusflow.data.preferences.UserPreferences;
import com.niezhili.focusflow.util.NotificationHelper;

/**
 * FocusFlowApp - 应用根组件
 *
 * 初始化 Room 数据库单例、DataStore 和通知渠道。
 * UserPreferences 在此作为单例持有，避免多个 DataStore 实例冲突。
 */
public class FocusFlowApp extends Application {

    private UserPreferences userPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化数据库（首次调用触发 Room 建表）
        AppDatabase.getInstance(this);
        // 创建通知渠道（Android 8.0+）
        new NotificationHelper(this).createChannel();
        // 初始化 DataStore 单例（整个应用共享一个实例）
        userPreferences = new UserPreferences(this);
    }

    public UserPreferences getUserPreferences() {
        return userPreferences;
    }
}