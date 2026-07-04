# FocusFlow — 开发文档

## 1. 环境配置

### 1.1 开发环境
| 工具 | 版本 |
|------|------|
| Android Studio | Hedgehog (2023.1.1) 或更高 |
| JDK | 17 |
| Gradle | 8.2+ |
| AGP | 8.2+ |
| compileSdk | 34 |
| minSdk | 26 (Android 8.0) |
| targetSdk | 34 (Android 14) |

### 1.2 依赖配置 (build.gradle.kts Module)

```kotlin
dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Activity & Lifecycle
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-rxjava3:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // RxJava3
    implementation("io.reactivex.rxjava3:rxjava:3.1.8")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.datastore:datastore-preferences-rxjava3:1.0.0")

    // Vico (图表)
    implementation("com.patrykandpatrick.vico:compose-m3:2.0.0-alpha.19")

    // Core KTX (Java 兼容)
    implementation("androidx.core:core:1.12.0")
}
```

---

## 2. 数据库设计

### 2.1 数据库版本与迁移

```java
@Database(
    entities = {Task.class, FocusSession.class},
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    // ...
}
```

| 版本 | 变更内容 | 迁移策略 |
|------|---------|---------|
| 1 | 初始版本：tasks + focus_sessions 表 | 无 |

### 2.2 Task 表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PRIMARY KEY, AUTO_INCREMENT | 任务 ID |
| title | TEXT | NOT NULL | 任务标题 |
| description | TEXT | NOT NULL, DEFAULT '' | 任务描述 |
| due_date | INTEGER | NULLABLE | 截止日期时间戳 |
| is_completed | INTEGER | NOT NULL, DEFAULT 0 | 是否完成 (0/1) |
| total_focus_seconds | INTEGER | NOT NULL, DEFAULT 0 | 累计专注秒数 |
| created_at | INTEGER | NOT NULL | 创建时间戳 |
| updated_at | INTEGER | NOT NULL | 更新时间戳 |

**索引**：
```sql
CREATE INDEX idx_tasks_created_at ON tasks(created_at DESC);
CREATE INDEX idx_tasks_is_completed ON tasks(is_completed);
```

### 2.3 FocusSession 表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INTEGER | PRIMARY KEY, AUTO_INCREMENT | 记录 ID |
| task_id | INTEGER | NOT NULL, FOREIGN KEY → tasks(id) ON DELETE CASCADE | 关联任务 |
| planned_duration | INTEGER | NOT NULL, DEFAULT 0 | 计划时长（秒） |
| actual_duration | INTEGER | NOT NULL | 实际专注时长（秒） |
| start_time | INTEGER | NOT NULL | 开始时间戳 |
| end_time | INTEGER | NULLABLE | 结束时间戳 |

| is_completed | INTEGER | NOT NULL, DEFAULT 0 | 是否完成 (0/1) |

**索引**：
```sql
CREATE INDEX idx_focus_task_id ON focus_sessions(task_id);
CREATE INDEX idx_focus_start_time ON focus_sessions(start_time DESC);

```

---

## 3. DAO 接口定义

### 3.1 TaskDao

```java
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

    // ── 累计时长 ──

    @Query("UPDATE tasks SET total_focus_seconds = total_focus_seconds + :seconds, updated_at = :now WHERE id = :taskId")
    Completable addFocusSeconds(long taskId, long seconds, long now);
}
```

### 3.2 FocusSessionDao

```java
@Dao
public interface FocusSessionDao {

    // ── 写入 ──

    @Insert
    Completable insertSession(FocusSession session);

    @Update
    Completable updateSession(FocusSession session);

    // ── 查询 ──

    @Query("SELECT * FROM focus_sessions WHERE task_id = :taskId ORDER BY start_time DESC")
    Flowable<List<FocusSession>> getSessionsByTaskId(long taskId);

    @Query("SELECT * FROM focus_sessions WHERE end_time IS NULL AND is_completed = 0")
    Single<FocusSession> getUnfinishedSession();

    // ── 统计聚合 ──

    // 今日总专注时长
    @Query("SELECT COALESCE(SUM(actual_duration), 0) FROM focus_sessions " +
           "WHERE start_time >= :dayStart AND start_time < :dayEnd AND is_completed = 1")
    Single<Long> getTodayTotalFocusSeconds(long dayStart, long dayEnd);

    // 今日完成番茄钟数
    @Query("SELECT COUNT(*) FROM focus_sessions " +
           "WHERE start_time >= :dayStart AND start_time < :dayEnd AND is_completed = 1")
    Single<Integer> getTodaySessionCount(long dayStart, long dayEnd);

    // 今日完成任务数 (去重 taskId)
    @Query("SELECT COUNT(DISTINCT task_id) FROM focus_sessions " +
           "WHERE start_time >= :dayStart AND start_time < :dayEnd AND is_completed = 1")
    Single<Integer> getTodayCompletedTaskCount(long dayStart, long dayEnd);

    // 本周每日专注时长 (GROUP BY 日期)
    @Query("SELECT strftime('%Y-%m-%d', start_time / 1000, 'unixepoch') AS date, " +
           "COALESCE(SUM(actual_duration), 0) AS total " +
           "FROM focus_sessions " +
           "WHERE start_time >= :weekStart AND start_time < :weekEnd AND is_completed = 1 " +
           "GROUP BY date ORDER BY date")
    Flowable<List<DailyStats>> getWeeklyStats(long weekStart, long weekEnd);

    // 本月每日专注时长
    @Query("SELECT strftime('%Y-%m-%d', start_time / 1000, 'unixepoch') AS date, " +
           "COALESCE(SUM(actual_duration), 0) AS total " +
           "FROM focus_sessions " +
           "WHERE start_time >= :monthStart AND start_time < :monthEnd AND is_completed = 1 " +
           "GROUP BY date ORDER BY date")
    Flowable<List<DailyStats>> getMonthlyStats(long monthStart, long monthEnd);

    // 各任务累计专注时长 (任务分布)
    @Query("SELECT t.id AS taskId, t.title AS taskTitle, " +
           "COALESCE(SUM(f.actual_duration), 0) AS totalSeconds " +
           "FROM tasks t LEFT JOIN focus_sessions f ON t.id = f.task_id AND f.is_completed = 1 " +
           "GROUP BY t.id, t.title ORDER BY totalSeconds DESC")
    Flowable<List<TaskDistribution>> getTaskDistribution();

    // 连续打卡天数
    @Query("SELECT DISTINCT strftime('%Y-%m-%d', start_time / 1000, 'unixepoch') AS date " +
           "FROM focus_sessions WHERE is_completed = 1 ORDER BY date DESC")
    Single<List<String>> getFocusDates();
}
```

### 3.3 聚合结果 POJO

```java
public class DailyStats {
    public String date;
    public long total;
}

public class TaskDistribution {
    public long taskId;
    public String taskTitle;
    public long totalSeconds;
}
```

---

## 4. Repository 层

### 4.1 TaskRepository

```java
public class TaskRepository {

    private final TaskDao taskDao;

    public TaskRepository(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    public Flowable<List<Task>> getActiveTasks() {
        return taskDao.getActiveTasks().subscribeOn(Schedulers.io());
    }

    public Flowable<List<Task>> getCompletedTasks() {
        return taskDao.getCompletedTasks().subscribeOn(Schedulers.io());
    }

    public Flowable<List<Task>> searchTasks(String keyword) {
        return taskDao.searchTasks(keyword).subscribeOn(Schedulers.io());
    }

    public Single<Task> getTaskById(long taskId) {
        return taskDao.getTaskById(taskId).subscribeOn(Schedulers.io());
    }

    public Completable insertTask(Task task) {
        task.setCreatedAt(System.currentTimeMillis());
        task.setUpdatedAt(System.currentTimeMillis());
        return taskDao.insertTask(task).subscribeOn(Schedulers.io());
    }

    public Completable updateTask(Task task) {
        task.setUpdatedAt(System.currentTimeMillis());
        return taskDao.updateTask(task).subscribeOn(Schedulers.io());
    }

    public Completable deleteTask(long taskId) {
        return taskDao.deleteTaskById(taskId).subscribeOn(Schedulers.io());
    }

    public Completable setTaskCompleted(long taskId, boolean completed) {
        return taskDao.setTaskCompleted(taskId, completed, System.currentTimeMillis())
                .subscribeOn(Schedulers.io());
    }

    public Completable addFocusSeconds(long taskId, long seconds) {
        return taskDao.addFocusSeconds(taskId, seconds, System.currentTimeMillis())
                .subscribeOn(Schedulers.io());
    }
}
```

### 4.2 FocusRepository

```java
public class FocusRepository {

    private final FocusSessionDao focusSessionDao;
    private final TaskRepository taskRepository;

    public FocusRepository(FocusSessionDao focusSessionDao, TaskRepository taskRepository) {
        this.focusSessionDao = focusSessionDao;
        this.taskRepository = taskRepository;
    }

    public Completable startSession(FocusSession session) {
        return focusSessionDao.insertSession(session).subscribeOn(Schedulers.io());
    }

    public Completable completeSession(FocusSession session) {
        return Completable.mergeArray(
            focusSessionDao.updateSession(session).subscribeOn(Schedulers.io()),
            taskRepository.addFocusSeconds(session.getTaskId(), session.getActualDuration())
        );
    }

    public Single<FocusSession> getUnfinishedSession() {
        return focusSessionDao.getUnfinishedSession().subscribeOn(Schedulers.io());
    }

    // 统计方法...
    public Single<Long> getTodayTotalFocusSeconds(long dayStart, long dayEnd) {
        return focusSessionDao.getTodayTotalFocusSeconds(dayStart, dayEnd).subscribeOn(Schedulers.io());
    }

    public Single<Integer> getTodaySessionCount(long dayStart, long dayEnd) {
        return focusSessionDao.getTodaySessionCount(dayStart, dayEnd).subscribeOn(Schedulers.io());
    }

    public Flowable<List<DailyStats>> getWeeklyStats(long weekStart, long weekEnd) {
        return focusSessionDao.getWeeklyStats(weekStart, weekEnd).subscribeOn(Schedulers.io());
    }

    public Flowable<List<TaskDistribution>> getTaskDistribution() {
        return focusSessionDao.getTaskDistribution().subscribeOn(Schedulers.io());
    }
}
```

---

## 5. DataStore 配置

### 5.1 UserPreferences

```java
public class UserPreferences {

    private static final String PREF_NAME = "focus_flow_settings";

    private final DataStore<Preferences> dataStore;

    // Keys
    private static final Preferences.Key<Integer> KEY_THEME_MODE =
            PreferencesKeys.intKey("theme_mode");       // 0=亮色 1=暗色 2=跟随系统
    private static final Preferences.Key<Integer> KEY_THEME_COLOR =
            PreferencesKeys.intKey("theme_color");      // 0-5
    private static final Preferences.Key<Boolean> KEY_NOTIFICATION_ENABLED =
            PreferencesKeys.booleanKey("notification_enabled");
    private static final Preferences.Key<String> KEY_SOUND =
            PreferencesKeys.stringKey("sound");         // "ding", "bell", "chime"
    private static final Preferences.Key<Boolean> KEY_VIBRATION_ENABLED =
            PreferencesKeys.booleanKey("vibration_enabled");

    public UserPreferences(Context context) {
        this.dataStore = new DataStoreFactory() {
            @Override
            public DataStore<Preferences> create(File file) {
                return new RxPreferenceDataStoreBuilder(context, PREF_NAME).build();
            }
        }.create(new File(context.getFilesDir(), "datastore/" + PREF_NAME));
    }

    // Theme Mode
    public Single<Integer> getThemeMode() { /* ... */ }
    public Completable setThemeMode(int mode) { /* ... */ }

    // Theme Color
    public Single<Integer> getThemeColor() { /* ... */ }
    public Completable setThemeColor(int color) { /* ... */ }

    // Notification
    public Single<Boolean> isNotificationEnabled() { /* ... */ }
    public Completable setNotificationEnabled(boolean enabled) { /* ... */ }

    // Sound
    public Single<String> getSound() { /* ... */ }
    public Completable setSound(String sound) { /* ... */ }

    // Vibration
    public Single<Boolean> isVibrationEnabled() { /* ... */ }
    public Completable setVibrationEnabled(boolean enabled) { /* ... */ }
}
```

---

## 6. ViewModel 层

### 6.1 TaskListViewModel

```java
public class TaskListViewModel extends ViewModel {

    private final TaskRepository taskRepository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    // LiveData
    private final MutableLiveData<List<Task>> taskList = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> snackbarMessage = new MutableLiveData<>();

    private String searchQuery = "";

    public TaskListViewModel(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
        loadTasks();
    }

    private void loadTasks() {
        isLoading.setValue(true);
        disposables.add(
            taskRepository.getActiveTasks()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    tasks -> {
                        taskList.setValue(tasks);
                        isLoading.setValue(false);
                    },
                    throwable -> {
                        errorMessage.setValue("加载失败: " + throwable.getMessage());
                        isLoading.setValue(false);
                    }
                )
        );
    }

    public void createTask(String title, @Nullable Long dueDate) {
        if (title.trim().isEmpty()) return;

        Task task = new Task();
        task.setTitle(title.trim());
        task.setDueDate(dueDate);

        disposables.add(
            taskRepository.insertTask(task)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> snackbarMessage.setValue("任务已添加"),
                    throwable -> errorMessage.setValue("创建失败")
                )
        );
    }

    public void deleteTask(long taskId) {
        disposables.add(
            taskRepository.deleteTask(taskId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> snackbarMessage.setValue("任务已删除"),
                    throwable -> errorMessage.setValue("删除失败")
                )
        );
    }

    public void toggleTaskComplete(Task task) {
        disposables.add(
            taskRepository.setTaskCompleted(task.getId(), !task.isCompleted())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> {},
                    throwable -> errorMessage.setValue("操作失败")
                )
        );
    }

    public void searchTasks(String query) {
        this.searchQuery = query;
        // 触发 loadTasks 或使用专门搜索方法
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }

    // Getters for LiveData...
    public LiveData<List<Task>> getTaskList() { return taskList; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getSnackbarMessage() { return snackbarMessage; }
}
```

### 6.2 TimerViewModel

```java
public class TimerViewModel extends ViewModel {

    private final FocusRepository focusRepository;
    private final TaskRepository taskRepository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private enum TimerState { IDLE, RUNNING, STOPPED }

    // LiveData
    private final MutableLiveData<TimerState> timerState = new MutableLiveData<>(TimerState.IDLE);
    private final MutableLiveData<Long> elapsedSeconds = new MutableLiveData<>(0L);
    private final MutableLiveData<String> taskTitle = new MutableLiveData<>("");
    private final MutableLiveData<Integer> ringColor = new MutableLiveData<>(0xFFFFFFFF); // 白色

    private long taskId;
    private long sessionStartTime;
    private long offsetSeconds = 0; // 本次计时开始前的累计秒数，用于计算 actualDuration
    private Disposable timerDisposable;

    public TimerViewModel(FocusRepository focusRepository, TaskRepository taskRepository) {
        this.focusRepository = focusRepository;
        this.taskRepository = taskRepository;
    }

    public void init(long taskId) {
        this.taskId = taskId;
        // 加载任务标题 + 累计专注时间（重新进入时恢复上次的时间）
        disposables.add(
            taskRepository.getTaskById(taskId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    task -> {
                        taskTitle.setValue(task.getTitle());
                        if (task.getTotalFocusSeconds() > 0) {
                            elapsedSeconds.setValue(task.getTotalFocusSeconds());
                            offsetSeconds = task.getTotalFocusSeconds();
                            timerState.setValue(TimerState.STOPPED);
                        }
                    },
                    throwable -> {}
                )
        );
        // 检查是否有未完成的 session 需要恢复（优先级高于累计时间）
        disposables.add(
            focusRepository.getUnfinishedSession()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    session -> {
                        if (session.getTaskId() == taskId) {
                            resumeSession(session);
                        }
                    },
                    throwable -> {} // 没有未完成 session，正常
                )
        );
    }

    public void toggleTimer() {
        TimerState current = timerState.getValue();
        if (current == TimerState.IDLE || current == TimerState.STOPPED) {
            startTimer();
        } else if (current == TimerState.RUNNING) {
            stopTimer();
        }
    }

    private void startTimer() {
        timerState.setValue(TimerState.RUNNING);
        ringColor.setValue(0xFF4CAF50); // 绿色
        sessionStartTime = System.currentTimeMillis();

        timerDisposable = Observable.interval(1, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                tick -> elapsedSeconds.setValue(tick + 1),
                throwable -> {}
            );
        disposables.add(timerDisposable);

        // 创建 FocusSession 记录
        FocusSession session = new FocusSession();
        session.setTaskId(taskId);
        session.setStartTime(sessionStartTime);
        session.setType("FOCUS");

        disposables.add(
            focusRepository.startSession(session)
                .subscribeOn(Schedulers.io())
                .subscribe(() -> {}, throwable -> {})
        );

        // 启动 Foreground Service
        // context.startService(new Intent(context, TimerService.class));
    }

    private void stopTimer() {
        if (timerDisposable != null && !timerDisposable.isDisposed()) {
            timerDisposable.dispose();
        }
        timerState.setValue(TimerState.STOPPED);
        ringColor.setValue(0xFFFFFFFF); // 白色

        long elapsed = elapsedSeconds.getValue() != null ? elapsedSeconds.getValue() : 0;
        // actualDuration = 总计时 - 起始偏移，只记录本次新增的时长
        long actualDuration = elapsed - offsetSeconds;

        FocusSession session = new FocusSession();
        session.setTaskId(taskId);
        session.setActualDuration((int) actualDuration);
        session.setEndTime(System.currentTimeMillis());
        session.setCompleted(true);

        disposables.add(
            focusRepository.completeSession(session)
                .subscribeOn(Schedulers.io())
                .subscribe(() -> {}, throwable -> {})
        );

        // 停止 Foreground Service
        // context.stopService(new Intent(context, TimerService.class));
    }

    private void resumeSession(FocusSession session) {
        long elapsed = (System.currentTimeMillis() - session.getStartTime()) / 1000;
        elapsedSeconds.setValue(elapsed);
        timerState.setValue(TimerState.RUNNING);
        ringColor.setValue(0xFF4CAF50);

        timerDisposable = Observable.interval(1, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(tick -> elapsedSeconds.setValue(elapsed + tick + 1));
        disposables.add(timerDisposable);
    }

    public void stopAndSave() {
        if (timerState.getValue() == TimerState.RUNNING) {
            stopTimer();
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }

    // Getters...
    public LiveData<TimerState> getTimerState() { return timerState; }
    public LiveData<Long> getElapsedSeconds() { return elapsedSeconds; }
    public LiveData<String> getTaskTitle() { return taskTitle; }
    public LiveData<Integer> getRingColor() { return ringColor; }
}
```

---

## 7. Foreground Service

### 7.1 TimerService

```java
public class TimerService extends Service {

    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "timer_channel";

    private PowerManager.WakeLock wakeLock;
    private Disposable timerDisposable;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private long taskId;
    private String taskTitle;
    private long startTime;
    private long elapsedSeconds;
    private boolean isRunning = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;

        String action = intent.getAction();
        if (action == null) return START_STICKY;

        switch (action) {
            case "START":
                taskId = intent.getLongExtra("task_id", 0);
                taskTitle = intent.getStringExtra("task_title");
                startTime = intent.getLongExtra("start_time", System.currentTimeMillis());
                startTimer();
                break;
            case "STOP":
                stopTimer();
                break;
            case "PAUSE":
                pauseTimer();
                break;
            case "RESUME":
                resumeTimer();
                break;
        }

        return START_STICKY;
    }

    private void startTimer() {
        isRunning = true;
        acquireWakeLock();
        createNotificationChannel();

        Notification notification = buildNotification("00:00", false);
        startForeground(NOTIFICATION_ID, notification);

        timerDisposable = Observable.interval(1, TimeUnit.SECONDS)
            .subscribe(tick -> {
                elapsedSeconds = tick + 1;
                String timeStr = TimeFormatter.formatSeconds(elapsedSeconds);
                updateNotification(timeStr);
            });
        disposables.add(timerDisposable);
    }

    private void pauseTimer() {
        isRunning = false;
        if (timerDisposable != null && !timerDisposable.isDisposed()) {
            timerDisposable.dispose();
        }
        String timeStr = TimeFormatter.formatSeconds(elapsedSeconds);
        updateNotification(timeStr + " (已暂停)");
    }

    private void resumeTimer() {
        isRunning = true;
        timerDisposable = Observable.interval(1, TimeUnit.SECONDS)
            .subscribe(tick -> {
                elapsedSeconds++;
                String timeStr = TimeFormatter.formatSeconds(elapsedSeconds);
                updateNotification(timeStr);
            });
        disposables.add(timerDisposable);
    }

    private void stopTimer() {
        isRunning = false;
        if (timerDisposable != null && !timerDisposable.isDisposed()) {
            timerDisposable.dispose();
        }
        releaseWakeLock();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FocusFlow:TimerWakeLock");
        wakeLock.acquire(24 * 60 * 60 * 1000); // 最长 24 小时
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }

    // Notification 相关方法...
    private void createNotificationChannel() { /* ... */ }
    private Notification buildNotification(String timeStr, boolean isPaused) { /* ... */ }
    private void updateNotification(String timeStr) { /* ... */ }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposables.clear();
        releaseWakeLock();
    }
}
```

### 7.2 AndroidManifest 声明

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.WAKE_LOCK" />

<application>
    <service
        android:name=".service.TimerService"
        android:foregroundServiceType="specialUse"
        android:exported="false" />
</application>
```

---

## 8. 工具类

### 8.1 TimeFormatter

```java
public class TimeFormatter {

    /**
     * 秒 → "HH:MM:SS" 格式
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
     * 获取当天的开始时间戳 (00:00:00)
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
     * 获取本周开始时间戳 (周一 00:00:00)
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
     * 获取本月开始时间戳 (1日 00:00:00)
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
}
```

### 8.2 NotificationHelper

```java
public class NotificationHelper {

    private static final String CHANNEL_ID = "timer_channel";
    private static final String CHANNEL_NAME = "计时通知";
    private static final int NOTIFICATION_ID = 1001;

    private final Context context;
    private final NotificationManager notificationManager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("用于显示专注计时状态");
            notificationManager.createNotificationChannel(channel);
        }
    }

    public Notification buildTimerNotification(String taskTitle, String timeStr, boolean isPaused) {
        // 暂停/继续按钮
        PendingIntent toggleIntent = buildActionIntent(isPaused ? "RESUME" : "PAUSE");
        PendingIntent stopIntent = buildActionIntent("STOP");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(taskTitle)
            .setContentText(isPaused ? timeStr + " (已暂停)" : timeStr)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, isPaused ? "继续" : "暂停", toggleIntent)
            .addAction(0, "停止", stopIntent);

        return builder.build();
    }

    private PendingIntent buildActionIntent(String action) {
        Intent intent = new Intent(context, TimerService.class);
        intent.setAction(action);
        return PendingIntent.getService(
            context, action.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    public void updateNotification(String taskTitle, String timeStr) {
        Notification notification = buildTimerNotification(taskTitle, timeStr, false);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    public void cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID);
    }
}
```

---

## 9. 导航图

### 9.1 路由定义

```java
public class AppNavigation {

    // 路由常量
    public static final String ROUTE_TASK_LIST = "task_list";
    public static final String ROUTE_TIMER = "timer/{taskId}/{taskTitle}";
    public static final String ROUTE_STATS = "stats";
    public static final String ROUTE_SETTINGS = "settings";

    // 底部导航项
    public enum BottomNavItem {
        TASKS(ROUTE_TASK_LIST, R.drawable.ic_task, "任务"),
        STATS(ROUTE_STATS, R.drawable.ic_stats, "统计"),
        SETTINGS(ROUTE_SETTINGS, R.drawable.ic_settings, "设置");
        // ...
    }
}
```

### 9.2 导航流

```
BottomNavBar
├── 任务列表 (ROUTE_TASK_LIST)      ← 默认首页
│   └── 计时页 (ROUTE_TIMER)        ← 点击「开始工作」导航，含 taskId 参数，隐藏底部导航栏
├── 统计 (ROUTE_STATS)               ← 底部导航切换到统计页
└── 设置 (ROUTE_SETTINGS)            ← 底部导航切换到设置页
```

---

## 10. 主题配置

### 10.1 主题色定义

```java
public class Color {

    // 6 种主题色
    public static final long[] THEME_COLORS = {
        0xFF2196F3,  // 蓝色
        0xFFFF9800,  // 橙色
        0xFF9C27B0,  // 紫色
        0xFF4CAF50,  // 绿色
        0xFFF44336,  // 红色
        0xFF607D8B,  // 灰色
    };

    // 计时器颜色
    public static final long TIMER_IDLE = 0xFFFFFFFF;      // 白色（未开始）
    public static final long TIMER_RUNNING = 0xFF4CAF50;   // 绿色（计时中）

    // 亮色模式
    public static final long LIGHT_BACKGROUND = 0xFFFAFAFA;
    public static final long LIGHT_SURFACE = 0xFFFFFFFF;
    public static final long LIGHT_ON_SURFACE = 0xFF1C1B1F;

    // 暗色模式
    public static final long DARK_BACKGROUND = 0xFF121212;
    public static final long DARK_SURFACE = 0xFF1E1E1E;
    public static final long DARK_ON_SURFACE = 0xFFE6E1E5;
}
```

### 10.2 主题模式

```java
public enum ThemeMode {
    LIGHT(0),      // 亮色
    DARK(1),       // 暗色
    FOLLOW_SYSTEM(2); // 跟随系统

    private final int value;
    ThemeMode(int value) { this.value = value; }
    public int getValue() { return value; }

    public static ThemeMode fromValue(int value) {
        for (ThemeMode mode : values()) {
            if (mode.value == value) return mode;
        }
        return FOLLOW_SYSTEM;
    }
}
```

---

## 11. 错误处理

### 11.1 错误码

| 错误码 | 含义 | 处理方式 |
|--------|------|---------|
| DB_INSERT_FAILED | 数据库写入失败 | Snackbar 提示 + 重试 |
| DB_QUERY_FAILED | 数据库查询失败 | 显示空状态 + 重试按钮 |
| PERMISSION_DENIED | 通知权限被拒绝 | 功能降级，不显示通知，不崩溃 |
| TIMER_OVERFLOW | 计时超过 24 小时 | 自动停止并保存 |

### 11.2 RxJava 错误处理模板

```java
disposables.add(
    repository.someOperation()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            result -> { /* 成功处理 */ },
            throwable -> {
                Log.e("FocusFlow", "Operation failed", throwable);
                errorMessage.setValue("操作失败，请重试");
            }
        )
);
```

---

## 12. 编码规范

### 12.1 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 类名 | PascalCase | `TaskListViewModel` |
| 方法名 | camelCase | `loadTasks()` |
| 常量 | UPPER_SNAKE | `TIMER_IDLE` |
| 成员变量 | camelCase | `taskRepository` |
| 资源文件 | snake_case | `ic_start_work.xml` |
| 布局 ID | snake_case | `task_card_title` |

### 12.2 包结构

```
com.example.focusflow/
├── data/             # 数据层 (Entity, DAO, Repository, Preferences)
├── ui/               # 界面层 (Screen, ViewModel, Components, Theme, Navigation)
├── service/          # 后台服务
└── util/             # 工具类
```

### 12.3 文件头注释

```java
/**
 * TaskListViewModel - 任务列表页的状态管理
 * 
 * 职责：
 * - 加载任务列表
 * - 创建/删除/更新任务
 * - 搜索任务
 * - 切换任务完成状态
 */
```

---

## 13. 测试策略

### 13.1 单元测试

| 测试对象 | 测试内容 | 工具 |
|---------|---------|------|
| TaskDao | CRUD 操作、搜索查询 | Room In-Memory Database + JUnit |
| FocusSessionDao | 聚合统计查询 | Room In-Memory Database + JUnit |
| TaskRepository | 业务逻辑封装 | Mockito |
| TimerViewModel | 状态转换逻辑 | Mockito + JUnit |
| TimeFormatter | 格式化输出 | JUnit |

### 13.2 测试用例示例

```java
@RunWith(AndroidJUnit4.class)
public class TaskDaoTest {

    private AppDatabase database;
    private TaskDao taskDao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        taskDao = database.taskDao();
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void testInsertAndQuery() {
        Task task = new Task();
        task.setTitle("测试任务");
        taskDao.insertTask(task).blockingAwait();

        List<Task> tasks = taskDao.getAllTasks().blockingFirst();
        assertEquals(1, tasks.size());
        assertEquals("测试任务", tasks.get(0).getTitle());
    }
}
```

---

## 14. 发布检查清单

- [ ] 移除所有 Log 调试输出
- [ ] 关闭 Room `exportSchema` 或配置 schema 导出路径
- [ ] ProGuard/R8 混淆规则配置
- [ ] 通知权限申请流程（Android 13+）
- [ ] 前台服务类型声明（Android 14+）
- [ ] 应用图标（自适应图标 + 通知图标）
- [ ] 签名配置
- [ ] versionCode / versionName 更新