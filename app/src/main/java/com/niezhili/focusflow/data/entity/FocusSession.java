package com.example.myapplication.data.entity;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * FocusSession - 专注记录实体
 *
 * 关联 Task，记录每次专注的开始/结束时间、实际时长等信息
 */
@Entity(
    tableName = "focus_sessions",
    foreignKeys = @ForeignKey(
        entity = Task.class,
        parentColumns = "id",
        childColumns = "task_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {
        @Index("task_id"),
        @Index("start_time")
    }
)
public class FocusSession {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "task_id")
    private long taskId;

    @ColumnInfo(name = "planned_duration")
    private int plannedDuration;

    @ColumnInfo(name = "actual_duration")
    private int actualDuration;

    @ColumnInfo(name = "start_time")
    private long startTime;

    @Nullable
    @ColumnInfo(name = "end_time")
    private Long endTime;

    @ColumnInfo(name = "is_completed")
    private boolean isCompleted = false;

    // ── Getters & Setters ──

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public int getPlannedDuration() {
        return plannedDuration;
    }

    public void setPlannedDuration(int plannedDuration) {
        this.plannedDuration = plannedDuration;
    }

    public int getActualDuration() {
        return actualDuration;
    }

    public void setActualDuration(int actualDuration) {
        this.actualDuration = actualDuration;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    @Nullable
    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(@Nullable Long endTime) {
        this.endTime = endTime;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }
}