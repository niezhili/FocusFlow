package com.niezhili.focusflow.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.niezhili.focusflow.data.entity.FocusSession;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/**
 * FocusSessionDao - 专注记录表数据访问对象
 *
 * 提供专注记录的写入、查询以及日/周/月/任务分布等聚合统计
 */
@Dao
public interface FocusSessionDao {

    // ── 异步写入（RxJava，供外部调用） ──

    @Insert
    Single<Long> insertSession(FocusSession session);

    @Update
    Completable updateSession(FocusSession session);

    // ── 查询 ──

    @Query("SELECT * FROM focus_sessions WHERE task_id = :taskId ORDER BY start_time DESC")
    Flowable<List<FocusSession>> getSessionsByTaskId(long taskId);

    @Query("SELECT * FROM focus_sessions WHERE end_time IS NULL AND is_completed = 0")
    Single<FocusSession> getUnfinishedSession();

    // ── 安全网：Service 结束时保存 ──

    @Query("UPDATE focus_sessions SET actual_duration = :duration, end_time = :endTime, is_completed = 1 " +
           "WHERE task_id = :taskId AND start_time = :startTime AND is_completed = 0")
    Completable completeSessionByTaskAndStartTime(long taskId, long startTime, int duration, long endTime);

    // ── 跨表更新：累加任务专注秒数 ──

    @Query("UPDATE tasks SET total_focus_seconds = total_focus_seconds + :seconds, updated_at = :now WHERE id = :taskId")
    Completable addTaskFocusSeconds(long taskId, long seconds, long now);

    // ── 统计聚合 ──
    // 注意：所有统计查询使用 end_time 过滤，确保按"完成时间"而非"开始时间"统计，
    // 这样跨天的计时会话会被计入完成当天，与任务卡片上的累计时长保持一致。

    // 今日总专注时长
    @Query("SELECT COALESCE(SUM(actual_duration), 0) FROM focus_sessions " +
           "WHERE end_time >= :dayStart AND end_time < :dayEnd AND is_completed = 1")
    Single<Long> getTodayTotalFocusSeconds(long dayStart, long dayEnd);

    // 今日番茄钟数（有专注记录的任务数，去重）
    @Query("SELECT COUNT(DISTINCT task_id) FROM focus_sessions " +
           "WHERE end_time >= :dayStart AND end_time < :dayEnd AND is_completed = 1")
    Single<Integer> getTodaySessionCount(long dayStart, long dayEnd);

    // 今日完成任务数 (去重 taskId)
    @Query("SELECT COUNT(DISTINCT task_id) FROM focus_sessions " +
           "WHERE end_time >= :dayStart AND end_time < :dayEnd AND is_completed = 1")
    Single<Integer> getTodayCompletedTaskCount(long dayStart, long dayEnd);

    // 本周每日专注时长 (GROUP BY 日期)
    @Query("SELECT strftime('%Y-%m-%d', end_time / 1000, 'unixepoch') AS date, " +
           "COALESCE(SUM(actual_duration), 0) AS total " +
           "FROM focus_sessions " +
           "WHERE end_time >= :weekStart AND end_time < :weekEnd AND is_completed = 1 " +
           "GROUP BY date ORDER BY date")
    Flowable<List<DailyStats>> getWeeklyStats(long weekStart, long weekEnd);

    // 本月每日专注时长
    @Query("SELECT strftime('%Y-%m-%d', end_time / 1000, 'unixepoch') AS date, " +
           "COALESCE(SUM(actual_duration), 0) AS total " +
           "FROM focus_sessions " +
           "WHERE end_time >= :monthStart AND end_time < :monthEnd AND is_completed = 1 " +
           "GROUP BY date ORDER BY date")
    Flowable<List<DailyStats>> getMonthlyStats(long monthStart, long monthEnd);

    // 日视图：按任务维度，每个任务一根柱子，时长求和
    // 复用 DailyStats POJO：date = 任务标题，total = 时长(秒)
    @Query("SELECT t.title AS date, COALESCE(SUM(f.actual_duration), 0) AS total " +
           "FROM focus_sessions f " +
           "INNER JOIN tasks t ON f.task_id = t.id " +
           "WHERE f.end_time >= :dayStart AND f.end_time < :dayEnd AND f.is_completed = 1 " +
           "GROUP BY t.id, t.title ORDER BY MAX(f.start_time)")
    Flowable<List<DailyStats>> getDaySessions(long dayStart, long dayEnd);

    // 各任务累计专注时长 (任务分布)
    @Query("SELECT t.id AS taskId, t.title AS taskTitle, " +
           "COALESCE(SUM(f.actual_duration), 0) AS totalSeconds " +
           "FROM tasks t LEFT JOIN focus_sessions f ON t.id = f.task_id AND f.is_completed = 1 " +
           "GROUP BY t.id, t.title ORDER BY totalSeconds DESC")
    Flowable<List<TaskDistribution>> getTaskDistribution();

    // 连续打卡天数（按完成日期计算）
    @Query("SELECT DISTINCT strftime('%Y-%m-%d', end_time / 1000, 'unixepoch') AS date " +
           "FROM focus_sessions WHERE is_completed = 1 ORDER BY date DESC")
    Single<List<String>> getFocusDates();
}