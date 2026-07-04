package com.example.myapplication.ui.stats;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.repository.FocusRepository;
import com.example.myapplication.data.repository.TaskRepository;

/**
 * StatsViewModelFactory - ViewModel 工厂
 *
 * 为 StatsViewModel 注入 FocusRepository 和 Application 依赖
 */
public class StatsViewModelFactory implements ViewModelProvider.Factory {

    private final FocusRepository focusRepository;
    private final TaskRepository taskRepository;
    private final Application application;

    public StatsViewModelFactory(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        TaskRepository taskRepository = new TaskRepository(database.taskDao());
        this.focusRepository = new FocusRepository(database.focusSessionDao());
        this.taskRepository = taskRepository;
        this.application = application;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(StatsViewModel.class)) {
            return (T) new StatsViewModel(focusRepository, taskRepository, application);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}