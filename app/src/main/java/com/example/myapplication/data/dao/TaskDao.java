package com.example.myapplication.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.myapplication.data.entity.Task;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/**
 * TaskDao - 任务表数据访问对象
 *
 * 提供任务的 CRUD、搜索、完成状态切换、累计时长更新等操作
 */
@Dao
public interface TaskDao {

    // ── 查询 ──

    @Query("SELECT * FROM tasks ORDER BY created_at DESC")
    Flowable<List<Task>> getAllTasks();

    @Query("SELECT * FROM tasks WHERE is_completed = 0 ORDER BY created_at DESC")
    Flowable<List<Task>> getActiveTasks();

    @Query("SELECT * FROM tasks WHERE is_completed = 1 ORDER BY created_at DESC")
    Flowable<List<Task>> getCompletedTasks();

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :keyword || '%' ORDER BY created_at DESC")
    Flowable<List<Task>> searchTasks(String keyword);

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    Single<Task> getTaskById(long taskId);

    // ── 写入 ──

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertTask(Task task);

    @Update
    Completable updateTask(Task task);

    @Delete
    Completable deleteTask(Task task);

    @Query("DELETE FROM tasks WHERE id = :taskId")
    Completable deleteTaskById(long taskId);

    // ── 完成状态 ──

    @Query("UPDATE tasks SET is_completed = :completed, updated_at = :now WHERE id = :taskId")
    Completable setTaskCompleted(long taskId, boolean completed, long now);

    // 今日完成任务数（标记为完成且 updated_at 在今天范围内）
    @Query("SELECT COUNT(*) FROM tasks " +
           "WHERE is_completed = 1 AND updated_at >= :dayStart AND updated_at < :dayEnd")
    Single<Integer> getTodayCompletedTaskCount(long dayStart, long dayEnd);

    // ── 累计时长 ──

    @Query("UPDATE tasks SET total_focus_seconds = total_focus_seconds + :seconds, updated_at = :now WHERE id = :taskId")
    Completable addFocusSeconds(long taskId, long seconds, long now);
}