package com.example.myapplication.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.myapplication.R;
import com.example.myapplication.service.TimerService;

/**
 * NotificationHelper - 通知辅助工具类
 *
 * 创建通知渠道、构建计时通知（含暂停/停止按钮）
 */
public class NotificationHelper {

    private static final String CHANNEL_ID = "timer_channel";
    private static final String CHANNEL_NAME = "计时通知";
    private static final int NOTIFICATION_ID = 1001;

    private final Context context;
    private final NotificationManager notificationManager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * 创建通知渠道（Android 8.0+）
     */
    public void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("用于显示专注计时状态");
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * 构建计时通知
     *
     * @param taskTitle 任务标题
     * @param timeStr   已用时间字符串
     * @param isPaused  是否暂停状态
     */
    public Notification buildTimerNotification(String taskTitle, String timeStr, boolean isPaused) {
        PendingIntent toggleIntent = buildActionIntent(isPaused ? "RESUME" : "PAUSE");
        PendingIntent stopIntent = buildActionIntent("STOP");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentTitle(taskTitle)
            .setContentText(isPaused ? timeStr + " (已暂停)" : timeStr)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, isPaused ? "继续" : "暂停", toggleIntent)
            .addAction(0, "停止", stopIntent);

        return builder.build();
    }

    /**
     * 构建 PendingIntent，指向 TimerService 的指定 action
     */
    private PendingIntent buildActionIntent(String action) {
        Intent intent = new Intent(context, TimerService.class);
        intent.setAction(action);
        return PendingIntent.getService(
            context, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    /**
     * 更新通知栏显示
     */
    public void updateNotification(String taskTitle, String timeStr) {
        Notification notification = buildTimerNotification(taskTitle, timeStr, false);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    /**
     * 取消通知
     */
    public void cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }
}