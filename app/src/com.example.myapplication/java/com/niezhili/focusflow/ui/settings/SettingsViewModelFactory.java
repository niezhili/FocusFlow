package com.niezhili.focusflow.ui.settings;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

/**
 * SettingsViewModelFactory - ViewModel 工厂
 *
 * 为 SettingsViewModel 注入 Application 依赖
 */
public class SettingsViewModelFactory implements ViewModelProvider.Factory {

    private final Application application;

    public SettingsViewModelFactory(Application application) {
        this.application = application;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(SettingsViewModel.class)) {
            return (T) new SettingsViewModel(application);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}