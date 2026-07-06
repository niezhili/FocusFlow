package com.niezhili.focusflow.ui.timer;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.niezhili.focusflow.data.local.AppDatabase;
import com.niezhili.focusflow.data.repository.FocusRepository;
import com.niezhili.focusflow.data.repository.TaskRepository;

/**
 * TimerViewModelFactory - ViewModel 工厂
 *
 * 为 TimerViewModel 注入 FocusRepository、TaskRepository 和 Application 依赖
 */
public class TimerViewModelFactory implements ViewModelProvider.Factory {

    private final FocusRepository focusRepository;
    private final TaskRepository taskRepository;
    private final Application application;

    public TimerViewModelFactory(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        this.taskRepository = new TaskRepository(database.taskDao());
        this.focusRepository = new FocusRepository(database.focusSessionDao());
        this.application = application;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(TimerViewModel.class)) {
            return (T) new TimerViewModel(focusRepository, taskRepository, application);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}