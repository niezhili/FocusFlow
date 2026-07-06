package com.niezhili.focusflow.ui.task;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.niezhili.focusflow.data.local.AppDatabase;
import com.niezhili.focusflow.data.repository.TaskRepository;

/**
 * TaskListViewModelFactory - ViewModel 工厂
 *
 * 为 TaskListViewModel 注入 TaskRepository 依赖
 */
public class TaskListViewModelFactory implements ViewModelProvider.Factory {

    private final TaskRepository taskRepository;

    public TaskListViewModelFactory(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        this.taskRepository = new TaskRepository(database.taskDao());
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(TaskListViewModel.class)) {
            return (T) new TaskListViewModel(taskRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}