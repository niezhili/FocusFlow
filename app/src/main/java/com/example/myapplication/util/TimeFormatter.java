package com.example.myapplication.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * TimeFormatter - 时间格式化工具类
 *
 * 提供秒→显示格式、时间戳→日期、时间范围计算等静态方法
 */
public class TimeFormatter {

    /**
     * 秒 → "HH:MM:SS" 或 "MM:SS" 格式
     */
    public static String formatSeconds(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

    /**
     * 秒 → "Xh Ym" / "Xm" / "Xs" 格式（用于任务卡片展示）
     * 不足 1 分钟以秒为单位，超过 1 分钟以分钟为单位
     */
    public static String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0 && minutes > 0) {
            return String.format(Locale.getDefault(), "%dh %dm", hours, minutes);
        } else if (hours > 0) {
            return String.format(Locale.getDefault(), "%dh", hours);
        } else if (minutes > 0) {
            return String.format(Locale.getDefault(), "%dm", minutes);
        } else if (seconds > 0) {
            return String.format(Locale.getDefault(), "%ds", seconds);
        } else {
            return "0s";
        }
    }

    /**
     * 时间戳 → "yyyy-MM-dd" 日期字符串
     */
    public static String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    /**
     * 获取当天的开始时间戳 (00:00:00.000)
     */
    public static long getDayStart(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * 获取当天的结束时间戳 (23:59:59.999)
     */
    public static long getDayEnd(long timestamp) {
        return getDayStart(timestamp) + 24 * 60 * 60 * 1000 - 1;
    }

    /**
     * 获取本周开始时间戳 (周一 00:00:00.000)
     */
    public static long getWeekStart(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * 获取本周结束时间戳 (周日 23:59:59.999)
     */
    public static long getWeekEnd(long timestamp) {
        return getWeekStart(timestamp) + 7 * 24 * 60 * 60 * 1000 - 1;
    }

    /**
     * 获取本月开始时间戳 (1日 00:00:00.000)
     */
    public static long getMonthStart(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * 获取本月结束时间戳 (最后一天 23:59:59.999)
     */
    public static long getMonthEnd(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }
}