package com.niezhili.focusflow.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.niezhili.focusflow.data.dao.FocusSessionDao;
import com.niezhili.focusflow.data.dao.TaskDao;
import com.niezhili.focusflow.data.entity.FocusSession;
import com.niezhili.focusflow.data.entity.Task;

/**
 * AppDatabase - Room 数据库单例
 *
 * 版本 1：tasks + focus_sessions 初始表
 */
@Database(
    entities = {Task.class, FocusSession.class},
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract TaskDao taskDao();

    public abstract FocusSessionDao focusSessionDao();

    /**
     * 在数据库事务中执行操作，保证原子性（要么全部成功，要么全部回滚）
     */
    public void runInTransaction(Runnable runnable) {
        super.runInTransaction(runnable);
    }

    /**
     * 获取数据库单例（双重检查锁定）
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "focusflow.db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}