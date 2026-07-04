package com.example.myapplication.ui.stats;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.data.dao.DailyStats;
import com.example.myapplication.data.dao.TaskDistribution;
import com.example.myapplication.data.repository.FocusRepository;
import com.example.myapplication.data.repository.TaskRepository;
import com.example.myapplication.util.TimeFormatter;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * StatsViewModel - 统计页的状态管理
 *
 * 职责：
 * - 加载今日概览数据（专注时长、番茄钟数、完成任务数）
 * - 加载日/周/月统计数据
 * - 加载任务分布数据
 * - 计算连续打卡天数
 */
public class StatsViewModel extends ViewModel {

    /**
     * 时间维度枚举
     */
    public enum TimeRange {
        DAY,    // 日视图
        WEEK,   // 周视图
        MONTH   // 月视图
    }

    private final FocusRepository focusRepository;
    private final TaskRepository taskRepository;
    private final Application application;
    private final CompositeDisposable disposables = new CompositeDisposable();

    // ── LiveData ──

    private final MutableLiveData<Long> todayTotalFocusSeconds = new MutableLiveData<>(0L);
    private final MutableLiveData<Integer> todaySessionCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> todayCompletedTaskCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> consecutiveDays = new MutableLiveData<>(0);
    private final MutableLiveData<List<DailyStats>> dailyStats = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<TaskDistribution>> taskDistribution = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private TimeRange currentTimeRange = TimeRange.WEEK;

    public StatsViewModel(FocusRepository focusRepository, TaskRepository taskRepository,
                          Application application) {
        this.focusRepository = focusRepository;
        this.taskRepository = taskRepository;
        this.application = application;
        loadAllStats();
    }

    // ── 数据加载 ──

    private void loadAllStats() {
        isLoading.setValue(true);
        loadTodayOverview();
        loadDailyStats(currentTimeRange);
        loadTaskDistribution();
        loadConsecutiveDays();
    }

    private void loadTodayOverview() {
        long now = System.currentTimeMillis();
        long dayStart = TimeFormatter.getDayStart(now);
        long dayEnd = TimeFormatter.getDayEnd(now);

        // 今日总专注时长
        disposables.add(
            focusRepository.getTodayTotalFocusSeconds(dayStart, dayEnd)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    seconds -> {
                        todayTotalFocusSeconds.setValue(seconds);
                        checkLoadingDone();
                    },
                    throwable -> {
                        errorMessage.setValue("加载今日专注时长失败");
                        checkLoadingDone();
                    }
                )
        );

        // 今日番茄钟数
        disposables.add(
            focusRepository.getTodaySessionCount(dayStart, dayEnd)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    count -> {
                        todaySessionCount.setValue(count);
                        checkLoadingDone();
                    },
                    throwable -> {
                        errorMessage.setValue("加载今日番茄钟数失败");
                        checkLoadingDone();
                    }
                )
        );

        // 今日完成任务数（统计 tasks 表中标记为完成的任务）
        disposables.add(
            taskRepository.getTodayCompletedTaskCount(dayStart, dayEnd)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    count -> {
                        todayCompletedTaskCount.setValue(count);
                        checkLoadingDone();
                    },
                    throwable -> {
                        errorMessage.setValue("加载今日完成任务数失败");
                        checkLoadingDone();
                    }
                )
        );
    }

    private void loadDailyStats(TimeRange range) {
        loadDailyStats(range, false);
    }

    /**
     * @param range 时间维度
     * @param isTabSwitch true = 用户切换 Tab，直接控制 isLoading；false = 初始加载，使用计数器
     */
    private void loadDailyStats(TimeRange range, boolean isTabSwitch) {
        long now = System.currentTimeMillis();
        currentTimeRange = range;

        switch (range) {
            case DAY:
                long dayStart = TimeFormatter.getDayStart(now);
                long dayEnd = TimeFormatter.getDayEnd(now);
                disposables.add(
                    focusRepository.getWeeklyStats(dayStart, dayEnd)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            stats -> {
                                dailyStats.setValue(stats);
                                if (isTabSwitch) isLoading.setValue(false);
                                else checkLoadingDone();
                            },
                            throwable -> {
                                errorMessage.setValue("加载日统计失败");
                                if (isTabSwitch) isLoading.setValue(false);
                                else checkLoadingDone();
                            }
                        )
                );
                break;
            case WEEK:
                long weekStart = TimeFormatter.getWeekStart(now);
                long weekEnd = TimeFormatter.getWeekEnd(now);
                disposables.add(
                    focusRepository.getWeeklyStats(weekStart, weekEnd)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            stats -> {
                                dailyStats.setValue(stats);
                                if (isTabSwitch) isLoading.setValue(false);
                                else checkLoadingDone();
                            },
                            throwable -> {
                                errorMessage.setValue("加载周统计失败");
                                if (isTabSwitch) isLoading.setValue(false);
                                else checkLoadingDone();
                            }
                        )
                );
                break;
            case MONTH:
                long monthStart = TimeFormatter.getMonthStart(now);
                long monthEnd = TimeFormatter.getMonthEnd(now);
                disposables.add(
                    focusRepository.getMonthlyStats(monthStart, monthEnd)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                            stats -> {
                                dailyStats.setValue(stats);
                                if (isTabSwitch) isLoading.setValue(false);
                                else checkLoadingDone();
                            },
                            throwable -> {
                                errorMessage.setValue("加载月统计失败");
                                if (isTabSwitch) isLoading.setValue(false);
                                else checkLoadingDone();
                            }
                        )
                );
                break;
        }
    }

    private void loadTaskDistribution() {
        disposables.add(
            focusRepository.getTaskDistribution()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    distribution -> {
                        taskDistribution.setValue(distribution);
                        checkLoadingDone();
                    },
                    throwable -> {
                        errorMessage.setValue("加载任务分布失败");
                        checkLoadingDone();
                    }
                )
        );
    }

    private void loadConsecutiveDays() {
        disposables.add(
            focusRepository.getFocusDates()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    dates -> {
                        consecutiveDays.setValue(calculateConsecutiveDays(dates));
                        checkLoadingDone();
                    },
                    throwable -> {
                        errorMessage.setValue("加载连续打卡失败");
                        checkLoadingDone();
                    }
                )
        );
    }

    /**
     * 计算连续打卡天数
     */
    private int calculateConsecutiveDays(List<String> dates) {
        if (dates == null || dates.isEmpty()) return 0;

        int consecutive = 0;
        String today = TimeFormatter.formatDate(System.currentTimeMillis());

        for (String date : dates) {
            if (date.equals(today)) {
                consecutive++;
            } else if (consecutive > 0) {
                // 检查是否是前一天
                break;
            }
        }

        // 简单计算：从最近日期开始连续的天数
        consecutive = 0;
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());

        for (String date : dates) {
            String expectedDate = TimeFormatter.formatDate(cal.getTimeInMillis());
            if (date.equals(expectedDate)) {
                consecutive++;
                cal.add(java.util.Calendar.DAY_OF_MONTH, -1);
            } else {
                break;
            }
        }

        return consecutive;
    }

    private int loadingCount = 0;
    private static final int TOTAL_LOADS = 6;

    private void checkLoadingDone() {
        loadingCount++;
        if (loadingCount >= TOTAL_LOADS) {
            isLoading.setValue(false);
            loadingCount = 0;
        }
    }

    // ── 公共方法 ──

    /**
     * 手动刷新所有统计数据（用于下拉刷新）
     */
    public void refresh() {
        disposables.clear();
        loadingCount = 0;
        loadAllStats();
    }

    public void switchTimeRange(TimeRange range) {
        isLoading.setValue(true);
        loadDailyStats(range, true);
    }

    // ─ Getters ──

    public LiveData<Long> getTodayTotalFocusSeconds() {
        return todayTotalFocusSeconds;
    }

    public LiveData<Integer> getTodaySessionCount() {
        return todaySessionCount;
    }

    public LiveData<Integer> getTodayCompletedTaskCount() {
        return todayCompletedTaskCount;
    }

    public LiveData<Integer> getConsecutiveDays() {
        return consecutiveDays;
    }

    public LiveData<List<DailyStats>> getDailyStats() {
        return dailyStats;
    }

    public LiveData<List<TaskDistribution>> getTaskDistribution() {
        return taskDistribution;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}