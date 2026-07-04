package com.example.myapplication.data.repository;

import com.example.myapplication.data.dao.DailyStats;
import com.example.myapplication.data.dao.FocusSessionDao;
import com.example.myapplication.data.dao.TaskDistribution;
import com.example.myapplication.data.entity.FocusSession;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * FocusRepository - 专注记录仓库
 *
 * 封装 FocusSessionDao，处理计时开始/完成等业务逻辑。
 * 完成操作使用 andThen 顺序执行：先更新 session，再累加 task 专注时长。
 */
public class FocusRepository {

    private final FocusSessionDao focusSessionDao;

    public FocusRepository(FocusSessionDao focusSessionDao) {
        this.focusSessionDao = focusSessionDao;
    }

    public Single<Long> startSession(FocusSession session) {
        return focusSessionDao.insertSession(session).subscribeOn(Schedulers.io());
    }

    public Completable updateSession(FocusSession session) {
        return focusSessionDao.updateSession(session).subscribeOn(Schedulers.io());
    }

    /**
     * 完成计时：先更新 session 记录，再累加任务的专注秒数。
     * 使用 andThen 顺序执行，保证 session 更新成功后才累加 task 时长。
     */
    public Completable completeSession(FocusSession session) {
        return focusSessionDao.updateSession(session)
            .andThen(focusSessionDao.addTaskFocusSeconds(
                session.getTaskId(), session.getActualDuration(), System.currentTimeMillis()))
            .subscribeOn(Schedulers.io());
    }

    /**
     * 安全网完成计时：通过 taskId + startTime 匹配 session（不依赖 session ID），
     * 然后累加任务的专注秒数。使用 andThen 顺序执行。
     */
    public Completable completeSessionByTaskAndStartTime(long taskId, long startTime,
                                                         int actualDuration, long endTime) {
        return focusSessionDao.completeSessionByTaskAndStartTime(
                taskId, startTime, actualDuration, endTime)
            .andThen(focusSessionDao.addTaskFocusSeconds(
                taskId, actualDuration, System.currentTimeMillis()))
            .subscribeOn(Schedulers.io());
    }

    public Single<FocusSession> getUnfinishedSession() {
        return focusSessionDao.getUnfinishedSession().subscribeOn(Schedulers.io());
    }

    public Flowable<List<FocusSession>> getSessionsByTaskId(long taskId) {
        return focusSessionDao.getSessionsByTaskId(taskId).subscribeOn(Schedulers.io());
    }

    // ── 统计方法 ──

    public Single<Long> getTodayTotalFocusSeconds(long dayStart, long dayEnd) {
        return focusSessionDao.getTodayTotalFocusSeconds(dayStart, dayEnd).subscribeOn(Schedulers.io());
    }

    public Single<Integer> getTodaySessionCount(long dayStart, long dayEnd) {
        return focusSessionDao.getTodaySessionCount(dayStart, dayEnd).subscribeOn(Schedulers.io());
    }

    public Single<Integer> getTodayCompletedTaskCount(long dayStart, long dayEnd) {
        return focusSessionDao.getTodayCompletedTaskCount(dayStart, dayEnd).subscribeOn(Schedulers.io());
    }

    public Flowable<List<DailyStats>> getWeeklyStats(long weekStart, long weekEnd) {
        return focusSessionDao.getWeeklyStats(weekStart, weekEnd).subscribeOn(Schedulers.io());
    }

    public Flowable<List<DailyStats>> getMonthlyStats(long monthStart, long monthEnd) {
        return focusSessionDao.getMonthlyStats(monthStart, monthEnd).subscribeOn(Schedulers.io());
    }

    public Flowable<List<TaskDistribution>> getTaskDistribution() {
        return focusSessionDao.getTaskDistribution().subscribeOn(Schedulers.io());
    }

    public Single<List<String>> getFocusDates() {
        return focusSessionDao.getFocusDates().subscribeOn(Schedulers.io());
    }
}