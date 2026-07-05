package com.example.myapplication.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.myapplication.R;
import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.dao.FocusSessionDao;
import com.example.myapplication.util.TimeFormatter;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

/**
 * TimerService - 前台计时服务
 *
 * 通过 Foreground Service + WakeLock 保证后台计时不被系统杀死。
 * 通知栏显示实时计时状态，支持暂停/继续/停止操作。
 */
public class TimerService extends Service {

    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "timer_channel";
    private static final String CHANNEL_NAME = "计时通知";

    private PowerManager.WakeLock wakeLock;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private long taskId;
    private String taskTitle;
    private long startTime;
    private long elapsedSeconds;
    private boolean isRunning = false;
    private boolean isPaused = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;

        String action = intent.getAction();
        if (action == null) return START_STICKY;

        switch (action) {
            case "START":
                taskId = intent.getLongExtra("task_id", 0);
                taskTitle = intent.getStringExtra("task_title");
                startTime = intent.getLongExtra("start_time", System.currentTimeMillis());
                elapsedSeconds = intent.getLongExtra("elapsed_seconds", 0);
                startTimer();
                break;
            case "STOP":
                stopTimerAndSave();
                break;
            case "PAUSE":
                pauseTimer();
                break;
            case "RESUME":
                resumeTimer();
                break;
        }

        return START_STICKY;
    }

    private void startTimer() {
        isRunning = true;
        isPaused = false;
        acquireWakeLock();

        Notification notification = buildNotification(
            TimeFormatter.formatSeconds(elapsedSeconds), false);
        startForeground(NOTIFICATION_ID, notification);

        Disposable timerDisposable = Observable.interval(1, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                tick -> {
                    elapsedSeconds++;
                    String timeStr = TimeFormatter.formatSeconds(elapsedSeconds);
                    updateNotification(timeStr);
                },
                throwable -> { /* ignore */ }
            );
        disposables.add(timerDisposable);
    }

    private void pauseTimer() {
        isRunning = false;
        isPaused = true;
        disposables.clear(); // 停止当前计时 Observable
        String timeStr = TimeFormatter.formatSeconds(elapsedSeconds);
        Notification notification = buildNotification(timeStr + " (已暂停)", true);
        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, notification);
    }

    private void resumeTimer() {
        isRunning = true;
        isPaused = false;
        disposables.clear(); // 清除旧的计时 Observable（如果有的话）

        Notification notification = buildNotification(
            TimeFormatter.formatSeconds(elapsedSeconds), false);
        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, notification);

        Disposable timerDisposable = Observable.interval(1, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                tick -> {
                    elapsedSeconds++;
                    String timeStr = TimeFormatter.formatSeconds(elapsedSeconds);
                    updateNotification(timeStr);
                },
                throwable -> { /* ignore */ }
            );
        disposables.add(timerDisposable);
    }

    /**
     * 停止计时：释放 WakeLock 并停止前台服务。
     * 安全网：在后台线程保存 FocusSession，防止 ViewModel 未保存时数据丢失。
     */
    private void stopTimerAndSave() {
        isRunning = false;
        releaseWakeLock();
        stopForeground(STOP_FOREGROUND_REMOVE);

        // 安全网：在后台线程保存 session 数据
        long finalElapsed = elapsedSeconds;
        long finalTaskId = taskId;
        long finalStartTime = startTime;
        new Thread(() -> {
            try {
                FocusSessionDao dao = AppDatabase.getInstance(this).focusSessionDao();
                dao.completeSessionByTaskAndStartTime(
                    finalTaskId, finalStartTime, (int) finalElapsed, System.currentTimeMillis()
                ).blockingAwait();
            } catch (Exception ignored) {
                // ViewModel 可能已经保存过了，或数据库不可用
            }
        }).start();

        stopSelf();
    }

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "FocusFlow:TimerWakeLock"
        );
        wakeLock.acquire(24 * 60 * 60 * 1000); // 最长 24 小时
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    // ── 通知相关 ──

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("用于显示专注计时状态");
            NotificationManager nm =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String timeStr, boolean isPaused) {
        PendingIntent toggleIntent = buildActionIntent(isPaused ? "RESUME" : "PAUSE");
        PendingIntent stopIntent = buildActionIntent("STOP");

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentTitle(taskTitle)
            .setContentText(timeStr)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, isPaused ? "继续" : "暂停", toggleIntent)
            .addAction(0, "停止", stopIntent)
            .build();
    }

    private PendingIntent buildActionIntent(String action) {
        Intent intent = new Intent(this, TimerService.class);
        intent.setAction(action);
        return PendingIntent.getService(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private void updateNotification(String timeStr) {
        Notification notification = buildNotification(timeStr, false);
        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, notification);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 如果服务被系统杀死时仍在运行，尝试保存数据
        if (isRunning) {
            long finalElapsed = elapsedSeconds;
            long finalTaskId = taskId;
            long finalStartTime = startTime;
            new Thread(() -> {
                try {
                    FocusSessionDao dao = AppDatabase.getInstance(this).focusSessionDao();
                    dao.completeSessionByTaskAndStartTime(
                        finalTaskId, finalStartTime, (int) finalElapsed, System.currentTimeMillis()
                    ).blockingAwait();
                } catch (Exception ignored) {
                }
            }).start();
        }
        disposables.clear();
        releaseWakeLock();
    }
}