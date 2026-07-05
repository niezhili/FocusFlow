package com.example.myapplication.ui.timer;

import android.app.Application;
import android.content.Intent;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.data.entity.FocusSession;
import com.example.myapplication.data.repository.FocusRepository;
import com.example.myapplication.data.repository.TaskRepository;
import com.example.myapplication.service.TimerService;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * TimerViewModel - 计时页的状态管理
 *
 * 职责：
 * - 管理计时状态（IDLE → RUNNING → STOPPED）
 * - RxJava Observable.interval 驱动计时器
 * - 创建/更新 FocusSession 记录
 * - 启动/停止前台计时服务
 * - 恢复未完成的计时
 */
public class TimerViewModel extends ViewModel {

    /**
     * 计时状态枚举
     */
    public enum TimerState {
        IDLE,    // 未开始
        RUNNING, // 计时中
        STOPPED  // 已停止
    }

    // 环形颜色常量
    public static final long COLOR_IDLE = 0xFFFFFFFF;     // 白色
    public static final long COLOR_RUNNING = 0xFF4CAF50;  // 绿色

    private final FocusRepository focusRepository;
    private final TaskRepository taskRepository;
    private final Application application;
    private final CompositeDisposable disposables = new CompositeDisposable();

    // ── LiveData ──

    private final MutableLiveData<TimerState> timerState = new MutableLiveData<>(TimerState.IDLE);
    private final MutableLiveData<Long> elapsedSeconds = new MutableLiveData<>(0L);
    private final MutableLiveData<String> taskTitle = new MutableLiveData<>("");
    private final MutableLiveData<Long> ringColor = new MutableLiveData<>(COLOR_IDLE);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private long taskId;
    private long sessionStartTime;
    private volatile long currentSessionId = -1;
    private long offsetSeconds = 0; // 本次计时开始前的累计秒数，用于计算 actualDuration
    private Disposable timerDisposable;

    public TimerViewModel(FocusRepository focusRepository,
                          TaskRepository taskRepository,
                          Application application) {
        this.focusRepository = focusRepository;
        this.taskRepository = taskRepository;
        this.application = application;
    }

    // ── 初始化 ──

    /**
     * 初始化计时页
     * - 加载任务标题和累计专注时间
     * - 检查是否有未完成的 session 需要恢复
     */
    public void init(long taskId) {
        this.taskId = taskId;

        // 加载任务信息（标题 + 累计专注秒数）
        disposables.add(
            taskRepository.getTaskById(taskId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    task -> {
                        taskTitle.setValue(task.getTitle());
                        // 如果之前有专注记录，显示累计时间作为起点
                        if (task.getTotalFocusSeconds() > 0) {
                            elapsedSeconds.setValue(task.getTotalFocusSeconds());
                            offsetSeconds = task.getTotalFocusSeconds();
                            timerState.setValue(TimerState.STOPPED);
                        }
                    },
                    throwable -> errorMessage.setValue("加载任务失败")
                )
        );

        // 检查是否有未完成的计时需要恢复（优先级高于累计时间）
        disposables.add(
            focusRepository.getUnfinishedSession()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    session -> {
                        // 只恢复同一任务的未完成 session
                        if (session.getTaskId() == taskId) {
                            resumeSession(session);
                        }
                    },
                    throwable -> { /* 无未完成 session，正常情况 */ }
                )
        );
    }

    // ── 计时控制 ──

    /**
     * 切换计时状态（环形区域点击）
     */
    public void toggleTimer() {
        TimerState current = timerState.getValue();
        if (current == TimerState.IDLE || current == TimerState.STOPPED) {
            startTimer();
        } else if (current == TimerState.RUNNING) {
            stopTimer();
        }
    }

    /**
     * 开始计时
     */
    private void startTimer() {
        TimerState previousState = timerState.getValue();
        timerState.setValue(TimerState.RUNNING);
        ringColor.setValue(COLOR_RUNNING);
        sessionStartTime = System.currentTimeMillis();

        long startOffset = 0;
        if (previousState == TimerState.STOPPED && elapsedSeconds.getValue() != null) {
            // 从 STOPPED 重新开始：保留之前的 elapsed 作为起始偏移
            startOffset = elapsedSeconds.getValue();
        }
        offsetSeconds = startOffset;

        final long offset = startOffset;
        timerDisposable = Observable.interval(1, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                tick -> elapsedSeconds.setValue(offset + tick + 1),
                throwable -> errorMessage.setValue("计时出错")
            );
        disposables.add(timerDisposable);

        // 创建 FocusSession 记录写入数据库
        FocusSession session = new FocusSession();
        session.setTaskId(taskId);
        session.setStartTime(sessionStartTime);
        disposables.add(
            focusRepository.startSession(session)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    sessionId -> currentSessionId = sessionId,
                    throwable -> errorMessage.setValue("记录创建失败")
                )
        );

        // 启动前台服务
        startForegroundService();
    }

    /**
     * 停止计时
     */
    private void stopTimer() {
        if (timerDisposable != null && !timerDisposable.isDisposed()) {
            timerDisposable.dispose();
        }

        timerState.setValue(TimerState.STOPPED);
        ringColor.setValue(COLOR_IDLE);

        long elapsed = elapsedSeconds.getValue() != null ? elapsedSeconds.getValue() : 0;
        long sessionId = currentSessionId;
        // 本次实际专注时长 = 总计时 - 起始偏移
        long actualDuration = elapsed - offsetSeconds;
        long endTime = System.currentTimeMillis();

        if (sessionId > 0) {
            // 正常路径：通过 session ID 更新
            FocusSession session = new FocusSession();
            session.setId(sessionId);
            session.setTaskId(taskId);
            session.setStartTime(sessionStartTime);
            session.setActualDuration((int) actualDuration);
            session.setEndTime(endTime);
            session.setCompleted(true);

            disposables.add(
                focusRepository.completeSession(session)
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                        () -> {},
                        throwable -> errorMessage.setValue("保存记录失败")
                    )
            );
        } else {
            // 安全网路径：currentSessionId 尚未从异步回调返回（竞态条件）
            // 通过 taskId + startTime 匹配并完成 session
            if (actualDuration > 0 && sessionStartTime > 0) {
                disposables.add(
                    focusRepository.completeSessionByTaskAndStartTime(
                        taskId, sessionStartTime, (int) actualDuration, endTime
                    )
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                            () -> {},
                            throwable -> errorMessage.setValue("保存记录失败")
                        )
                );
            }
        }

        currentSessionId = -1;

        // 停止前台服务
        stopForegroundService();
    }

    /**
     * 恢复未完成的计时
     */
    private void resumeSession(FocusSession session) {
        long elapsed = (System.currentTimeMillis() - session.getStartTime()) / 1000;
        elapsedSeconds.setValue(elapsed);
        timerState.setValue(TimerState.RUNNING);
        ringColor.setValue(COLOR_RUNNING);
        sessionStartTime = session.getStartTime();
        currentSessionId = session.getId();
        offsetSeconds = 0; // 恢复已有 session，偏移为 0

        timerDisposable = Observable.interval(1, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                tick -> elapsedSeconds.setValue(elapsed + tick + 1),
                throwable -> errorMessage.setValue("计时出错")
            );
        disposables.add(timerDisposable);

        // 启动前台服务，确保后台计时不被系统杀死
        startForegroundService();
    }

    // ── 前台服务控制 ──

    private void startForegroundService() {
        Intent intent = new Intent(application, TimerService.class);
        intent.setAction("START");
        intent.putExtra("task_id", taskId);
        intent.putExtra("task_title", taskTitle.getValue());
        intent.putExtra("start_time", sessionStartTime);
        Long elapsed = elapsedSeconds.getValue();
        intent.putExtra("elapsed_seconds", elapsed != null ? elapsed : 0);
        application.startForegroundService(intent);
    }

    private void stopForegroundService() {
        Intent intent = new Intent(application, TimerService.class);
        intent.setAction("STOP");
        application.startService(intent);
    }

    /**
     * 停止计时并保存（用于返回/退出时调用）
     */
    public void stopAndSave() {
        if (timerState.getValue() == TimerState.RUNNING) {
            stopTimer();
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // ViewModel 销毁时自动停止计时
        if (timerState.getValue() == TimerState.RUNNING) {
            stopTimer();
        }
        // NOTE: 不要在这里调用 disposables.clear() — stopTimer() 会向 disposables
        // 中添加异步数据库保存操作，必须让它们执行完毕。timerDisposable 已在 stopTimer() 中单独 dispose。
    }

    // ── Getters ──

    public LiveData<TimerState> getTimerState() {
        return timerState;
    }

    public LiveData<Long> getElapsedSeconds() {
        return elapsedSeconds;
    }

    public LiveData<String> getTaskTitle() {
        return taskTitle;
    }

    public LiveData<Long> getRingColor() {
        return ringColor;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
}