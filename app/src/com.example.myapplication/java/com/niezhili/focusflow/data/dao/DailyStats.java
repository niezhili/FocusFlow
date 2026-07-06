package com.niezhili.focusflow.data.dao;

import androidx.room.Ignore;

/**
 * DailyStats - 每日统计聚合结果 POJO
 *
 * 用于 FocusSessionDao 的 GROUP BY 日期查询结果映射
 */
public class DailyStats {

    public String date;
    public long total;

    public DailyStats() {
    }

    @Ignore
    public DailyStats(String date, long total) {
        this.date = date;
        this.total = total;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}