package com.niezhili.focusflow.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Task - 任务实体
 *
 * 字段：标题、描述、截止日期、完成状态、累计专注秒数、时间戳
 */
@Entity(tableName = "tasks")
public class Task {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String title;

    @NonNull
    private String description = "";

    @Nullable
    @ColumnInfo(name = "due_date")
    private Long dueDate;          // timestamp, nullable

    @ColumnInfo(name = "is_completed")
    private boolean isCompleted = false;

    @ColumnInfo(name = "total_focus_seconds")
    private long totalFocusSeconds = 0;  // 累计专注秒数，冗余字段方便排序

    @ColumnInfo(name = "created_at")
    private long createdAt = System.currentTimeMillis();

    @ColumnInfo(name = "updated_at")
    private long updatedAt = System.currentTimeMillis();

    // ── Getters & Setters ──

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    public void setDescription(@NonNull String description) {
        this.description = description;
    }

    @Nullable
    public Long getDueDate() {
        return dueDate;
    }

    public void setDueDate(@Nullable Long dueDate) {
        this.dueDate = dueDate;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public long getTotalFocusSeconds() {
        return totalFocusSeconds;
    }

    public void setTotalFocusSeconds(long totalFocusSeconds) {
        this.totalFocusSeconds = totalFocusSeconds;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}