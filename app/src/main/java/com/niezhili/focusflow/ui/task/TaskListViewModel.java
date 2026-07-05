package com.example.myapplication.ui.task;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.data.entity.Task;
import com.example.myapplication.data.repository.TaskRepository;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * TaskListViewModel - 任务列表页的状态管理
 *
 * 职责：
 * - 加载任务列表（活跃 + 已完成）
 * - 创建/删除/更新任务
 * - 切换任务完成状态
 */
public class TaskListViewModel extends ViewModel {

    private final TaskRepository taskRepository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    // ── LiveData ──

    private final MutableLiveData<List<Task>> activeTasks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Task>> completedTasks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> snackbarMessage = new MutableLiveData<>();

    public TaskListViewModel(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
        loadTasks();
    }

    // ── 数据加载 ──

    private void loadTasks() {
        isLoading.setValue(true);

        // 加载活跃任务
        disposables.add(
            taskRepository.getActiveTasks()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    tasks -> {
                        activeTasks.setValue(tasks);
                        isLoading.setValue(false);
                    },
                    throwable -> {
                        errorMessage.setValue("加载失败: " + throwable.getMessage());
                        isLoading.setValue(false);
                    }
                )
        );

        // 加载已完成任务
        disposables.add(
            taskRepository.getCompletedTasks()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    tasks -> completedTasks.setValue(tasks),
                    throwable -> { /* ignore */ }
                )
        );
    }

    // ── 任务操作 ──

    /**
     * 创建新任务
     */
    public void createTask(String title) {
        if (title == null || title.trim().isEmpty()) return;

        Task task = new Task();
        task.setTitle(title.trim());

        disposables.add(
            taskRepository.insertTask(task)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> snackbarMessage.setValue("任务已添加"),
                    throwable -> errorMessage.setValue("创建失败")
                )
        );
    }

    /**
     * 删除任务
     */
    public void deleteTask(long taskId) {
        disposables.add(
            taskRepository.deleteTask(taskId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> snackbarMessage.setValue("任务已删除"),
                    throwable -> errorMessage.setValue("删除失败")
                )
        );
    }

    /**
     * 切换任务完成状态
     */
    public void toggleTaskComplete(Task task) {
        disposables.add(
            taskRepository.setTaskCompleted(task.getId(), !task.isCompleted())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> {},
                    throwable -> errorMessage.setValue("操作失败")
                )
        );
    }

    /**
     * 清除一次性消息（Snackbar 消费后调用）
     */
    public void clearSnackbarMessage() {
        snackbarMessage.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }

    // ── Getters ──

    public LiveData<List<Task>> getActiveTasks() {
        return activeTasks;
    }

    public LiveData<List<Task>> getCompletedTasks() {
        return completedTasks;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getSnackbarMessage() {
        return snackbarMessage;
    }
}