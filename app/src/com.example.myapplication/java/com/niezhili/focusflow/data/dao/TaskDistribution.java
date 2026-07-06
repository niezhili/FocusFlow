package com.niezhili.focusflow.data.dao;

import androidx.room.Ignore;

/**
 * TaskDistribution - 任务专注时长分布 POJO
 *
 * 用于 FocusSessionDao 的任务分布查询结果映射
 */
public class TaskDistribution {

    public long taskId;
    public String taskTitle;
    public long totalSeconds;

    public TaskDistribution() {
    }

    @Ignore
    public TaskDistribution(long taskId, String taskTitle, long totalSeconds) {
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.totalSeconds = totalSeconds;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public String getTaskTitle() {
        return taskTitle;
    }

    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }

    public long getTotalSeconds() {
        return totalSeconds;
    }

    public void setTotalSeconds(long totalSeconds) {
        this.totalSeconds = totalSeconds;
    }
}