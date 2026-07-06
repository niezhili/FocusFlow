package com.niezhili.focusflow.ui.stats;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.niezhili.focusflow.data.dao.DailyStats;
import com.niezhili.focusflow.data.dao.TaskDistribution;
import com.niezhili.focusflow.data.repository.FocusRepository;
import com.niezhili.focusflow.data.repository.TaskRepository;
import com.niezhili.focusflow.util.TimeFormatter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * StatsViewModel - 统计页的核心状态管理器
 *
 * 职责：
 * 1. 一次加载 6 项数据：今日总时长、番茄钟数、完成任务数、图表统计、任务分布、连续天数
 * 2. 用 loadingCount 计数器跟踪所有数据加载完成状态
 * 3. 日/周/月 Tab 切换时重新加载对应的图表数据
 * 4. 下拉刷新时静默更新（不显示 loading 动画）
 *
 * 数据加载策略：
 * - 构造时：loadAllStats() 并行发出 6 个异步查询
 * - Tab 切换：switchTimeRange() 只重新加载图表数据（其他数据不变）
 * - 下拉刷新：refresh() 重新加载所有数据（但不设置 isLoading=true）
 * - 每次加载完成调用 checkLoadingDone()，计数器到达 TOTAL_LOADS(6) 时关闭 loading
 *
 * 日期补齐逻辑：
 * 周/月视图从数据库查询到的数据可能缺少某些天的记录（当天没有专注），
 * fillWeekDays() / fillMonthDays() 负责将缺失的天补上（total=0），
 * 这样图表才能完整显示所有日期。
 */
public class StatsViewModel extends ViewModel {

    /**
     * 时间维度枚举，对应统计页的 Tab 标签
     */
    public enum TimeRange {
        DAY,    // 日视图：按任务维度展示今日各任务的专注时长
        WEEK,   // 周视图：按天展示本周每日专注时长（周一～周日）
        MONTH   // 月视图：按天展示本月每日专注时长趋势
    }

    private final FocusRepository focusRepository;
    private final TaskRepository taskRepository;
    private final Application application;
    private final CompositeDisposable disposables = new CompositeDisposable();

    // ── LiveData（暴露给 UI 层观察） ──

    private final MutableLiveData<Long> todayTotalFocusSeconds = new MutableLiveData<>(0L);   // 今日总专注时长（秒）
    private final MutableLiveData<Integer> todaySessionCount = new MutableLiveData<>(0);       // 今日番茄钟数
    private final MutableLiveData<Integer> todayCompletedTaskCount = new MutableLiveData<>(0); // 今日完成任务数
    private final MutableLiveData<Integer> consecutiveDays = new MutableLiveData<>(0);         // 连续打卡天数
    private final MutableLiveData<List<DailyStats>> dailyStats = new MutableLiveData<>(new ArrayList<>());              // 图表数据
    private final MutableLiveData<List<TaskDistribution>> taskDistribution = new MutableLiveData<>(new ArrayList<>());  // 任务分布数据
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);           // 加载状态
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();              // 错误信息

    private TimeRange currentTimeRange = TimeRange.WEEK;  // 默认显示周视图

    public StatsViewModel(FocusRepository focusRepository, TaskRepository taskRepository,
                          Application application) {
        this.focusRepository = focusRepository;
        this.taskRepository = taskRepository;
        this.application = application;
        loadAllStats();  // 构造时立即加载所有统计数据
    }

    // ═══════════════════════════════════════════════
    // 数据加载
    // ═══════════════════════════════════════════════

    /**
     * 加载所有 6 项统计数据（构造时和下拉刷新时调用）。
     * 6 个查询并行执行，各自在完成后调用 checkLoadingDone() 递减计数器。
     */
    private void loadAllStats() {
        isLoading.setValue(true);
        loadTodayOverview();    // 3 个查询：总时长、番茄钟数、完成任务数
        loadDailyStats(currentTimeRange); // 1 个查询：图表数据
        loadTaskDistribution(); // 1 个查询：任务分布
        loadConsecutiveDays();  // 1 个查询：连续天数
    }

    /**
     * 加载今日概览数据（3 个并行查询）。
     * 使用 TimeFormatter.getDayStart/End 计算今日 00:00 ~ 23:59 的时间戳范围。
     */
    private void loadTodayOverview() {
        long now = System.currentTimeMillis();
        long dayStart = TimeFormatter.getDayStart(now);
        long dayEnd = TimeFormatter.getDayEnd(now);

        // 查询 1：今日总专注时长
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

        // 查询 2：今日番茄钟数（有专注记录的去重任务数）
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

        // 查询 3：今日完成任务数
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

    /**
     * 加载日统计图表数据（无参版本，用于初始加载）。
     */
    private void loadDailyStats(TimeRange range) {
        loadDailyStats(range, false);
    }

    /**
     * 加载日/周/月统计图表数据。
     *
     * @param range      时间维度（DAY / WEEK / MONTH）
     * @param isTabSwitch true = 用户切换 Tab 触发，直接控制 isLoading；
     *                    false = 初始加载，使用 checkLoadingDone() 计数器
     *
     * DAY 视图：按任务维度统计今日各任务的专注时长（柱状图每根柱子代表一个任务）
     * WEEK 视图：按天统计本周每日专注时长，fillWeekDays() 补齐缺失的天
     * MONTH 视图：按天统计本月每日专注时长，fillMonthDays() 补齐缺失的天
     */
    private void loadDailyStats(TimeRange range, boolean isTabSwitch) {
        long now = System.currentTimeMillis();
        currentTimeRange = range;

        switch (range) {
            case DAY:
                long dayStart = TimeFormatter.getDayStart(now);
                long dayEnd = TimeFormatter.getDayEnd(now);
                disposables.add(
                    focusRepository.getDaySessions(dayStart, dayEnd)
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
                                // 将查询结果补齐为完整的周一～周日（缺的天补 0）
                                dailyStats.setValue(fillWeekDays(stats, weekStart));
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
                                // 将查询结果补齐为完整的 1 日～月末（缺的天补 0）
                                dailyStats.setValue(fillMonthDays(stats, monthStart));
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
     * 计算连续打卡天数。
     *
     * 算法：从今天开始，每天往前推一天，检查该日期是否有专注记录。
     * 一旦发现某一天没有记录，立即停止计数。
     *
     * 例如：用户有 7/1、7/2、7/3、7/5、7/6 的记录
     * 今天是 7/6，则连续打卡 = 2（7/6 和 7/5 有记录，7/4 没有）
     *
     * @param dates 有专注记录的日期列表（降序排列，如 ["2026-07-06", "2026-07-05", ...]）
     */
    private int calculateConsecutiveDays(List<String> dates) {
        if (dates == null || dates.isEmpty()) return 0;

        int consecutive = 0;
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());

        // 从今天开始往前遍历，日期必须连续
        for (String date : dates) {
            String expectedDate = TimeFormatter.formatDate(cal.getTimeInMillis());
            if (date.equals(expectedDate)) {
                consecutive++;
                cal.add(java.util.Calendar.DAY_OF_MONTH, -1); // 往前推一天
            } else {
                break; // 不连续了，停止计数
            }
        }

        return consecutive;
    }

    /**
     * 月视图日期补齐。
     *
     * 数据库查询结果只包含有专注记录的日期（可能有缺失的天），
     * 图表需要显示完整的 1 日～月末，没有数据的日期显示为 0。
     *
     * 例如：7/3、7/5 有记录 → 补齐后 = [(7/1, 0), (7/2, 0), (7/3, 120), ...]
     */
    private List<DailyStats> fillMonthDays(List<DailyStats> actual, long monthStart) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(monthStart);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // 将查询结果转为 Map<日期字符串, 时长> 方便查找
        Map<String, Long> dateMap = new HashMap<>();
        for (DailyStats s : actual) {
            dateMap.put(s.getDate(), s.getTotal());
        }

        // 遍历当月每一天，从 map 中取值，取不到则填 0
        List<DailyStats> filled = new ArrayList<>();
        for (int day = 1; day <= daysInMonth; day++) {
            cal.set(Calendar.DAY_OF_MONTH, day);
            String dateStr = TimeFormatter.formatDate(cal.getTimeInMillis());
            DailyStats stat = new DailyStats();
            stat.setDate(dateStr);
            stat.setTotal(dateMap.getOrDefault(dateStr, 0L));
            filled.add(stat);
        }
        return filled;
    }

    /**
     * 周视图日期补齐。
     *
     * 与 fillMonthDays 类似，但固定补齐为周一～周日 7 天。
     */
    private List<DailyStats> fillWeekDays(List<DailyStats> actual, long weekStart) {
        Map<String, Long> dateMap = new HashMap<>();
        for (DailyStats s : actual) {
            dateMap.put(s.getDate(), s.getTotal());
        }

        List<DailyStats> filled = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(weekStart);

        // 遍历周一～周日（共 7 天）
        for (int day = 0; day < 7; day++) {
            String dateStr = TimeFormatter.formatDate(cal.getTimeInMillis());
            DailyStats stat = new DailyStats();
            stat.setDate(dateStr);
            stat.setTotal(dateMap.getOrDefault(dateStr, 0L));
            filled.add(stat);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return filled;
    }

    // ── 加载状态管理 ──

    private int loadingCount = 0;           // 当前已完成的数据加载数
    private static final int TOTAL_LOADS = 6; // 总共需要加载的数据项数

    /**
     * 数据加载完成检查。
     * 每次任意一项数据加载完成后调用，内部计数器 +1。
     * 当到达 TOTAL_LOADS(6) 时关闭 loading 状态。
     *
     * 6 项数据包括：
     * 1. 今日总时长
     * 2. 今日番茄钟数
     * 3. 今日完成任务数
     * 4. 图表数据（日/周/月）
     * 5. 任务分布
     * 6. 连续天数
     */
    private void checkLoadingDone() {
        loadingCount++;
        if (loadingCount >= TOTAL_LOADS) {
            isLoading.setValue(false);
            loadingCount = 0;
        }
    }

    // ═══════════════════════════════════════════════
    // 公共方法（供 UI 层调用）
    // ═══════════════════════════════════════════════

    /**
     * 手动刷新所有统计数据（下拉刷新时调用）。
     *
     * 与 loadAllStats() 的区别：
     * - refresh() 不清除旧数据，不设置 isLoading=true
     * - 直接重新发起 6 个查询，数据更新后 UI 会自动重组
     * - 这样下拉刷新时页面不会闪烁
     */
    public void refresh() {
        disposables.clear();
        loadingCount = 0;
        isLoading.setValue(false); // 重置残留的加载状态，避免下拉刷新时显示 loading 指示器
        loadTodayOverview();
        loadDailyStats(currentTimeRange);
        loadTaskDistribution();
        loadConsecutiveDays();
    }

    /**
     * 切换时间维度 Tab（日/周/月）。
     * 只重新加载图表数据，今日概览、任务分布、连续天数保持不变。
     */
    public void switchTimeRange(TimeRange range) {
        isLoading.setValue(true);
        loadDailyStats(range, true); // isTabSwitch=true，不使用计数器
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