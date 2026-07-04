package com.example.myapplication.ui.settings;

import android.app.Application;
import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.myapplication.data.preferences.UserPreferences;
import com.example.myapplication.ui.theme.ThemeMode;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * SettingsViewModel - 设置页的状态管理
 *
 * 职责：
 * - 读取/写入主题模式、主题色、通知开关、提示音、震动开关
 * - 实时同步 DataStore 中的设置值
 */
public class SettingsViewModel extends ViewModel {

    private final UserPreferences userPreferences;
    private final CompositeDisposable disposables = new CompositeDisposable();

    // ─ LiveData ──

    private final MutableLiveData<ThemeMode> themeMode = new MutableLiveData<>(ThemeMode.FOLLOW_SYSTEM);
    private final MutableLiveData<Integer> themeColor = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> notificationEnabled = new MutableLiveData<>(true);
    private final MutableLiveData<String> sound = new MutableLiveData<>("ding");
    private final MutableLiveData<Boolean> vibrationEnabled = new MutableLiveData<>(true);
    private final MutableLiveData<String> snackbarMessage = new MutableLiveData<>();

    public SettingsViewModel(Application application) {
        Context context = application.getApplicationContext();
        this.userPreferences = new UserPreferences(context);
        loadAllSettings();
    }

    // ── 数据加载 ──

    private void loadAllSettings() {
        // 主题模式
        disposables.add(
            userPreferences.getThemeMode()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    mode -> themeMode.setValue(ThemeMode.fromValue(mode.intValue())),
                    throwable -> {}
                )
        );

        // 主题色
        disposables.add(
            userPreferences.getThemeColor()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    color -> themeColor.setValue(color),
                    throwable -> {}
                )
        );

        // 通知开关
        disposables.add(
            userPreferences.isNotificationEnabled()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    enabled -> notificationEnabled.setValue(enabled),
                    throwable -> {}
                )
        );

        // 提示音
        disposables.add(
            userPreferences.getSound()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    s -> sound.setValue(s),
                    throwable -> {}
                )
        );

        // 震动开关
        disposables.add(
            userPreferences.isVibrationEnabled()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    enabled -> vibrationEnabled.setValue(enabled),
                    throwable -> {}
                )
        );
    }

    // ── 设置写入 ──

    public void setThemeMode(ThemeMode mode) {
        themeMode.setValue(mode);
        disposables.add(
            userPreferences.setThemeMode(mode.getValue())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> snackbarMessage.setValue("主题模式已更新"),
                    throwable -> snackbarMessage.setValue("更新失败")
                )
        );
    }

    public void setThemeColor(int colorIndex) {
        themeColor.setValue(colorIndex);
        disposables.add(
            userPreferences.setThemeColor(colorIndex)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> snackbarMessage.setValue("主题色已更新"),
                    throwable -> snackbarMessage.setValue("更新失败")
                )
        );
    }

    public void setNotificationEnabled(boolean enabled) {
        notificationEnabled.setValue(enabled);
        disposables.add(
            userPreferences.setNotificationEnabled(enabled)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> {},
                    throwable -> snackbarMessage.setValue("更新失败")
                )
        );
    }

    public void setSound(String soundName) {
        sound.setValue(soundName);
        disposables.add(
            userPreferences.setSound(soundName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> snackbarMessage.setValue("提示音已更新"),
                    throwable -> snackbarMessage.setValue("更新失败")
                )
        );
    }

    public void setVibrationEnabled(boolean enabled) {
        vibrationEnabled.setValue(enabled);
        disposables.add(
            userPreferences.setVibrationEnabled(enabled)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> {},
                    throwable -> snackbarMessage.setValue("更新失败")
                )
        );
    }

    public void clearSnackbarMessage() {
        snackbarMessage.setValue(null);
    }

    // ── Getters ──

    public LiveData<ThemeMode> getThemeMode() {
        return themeMode;
    }

    public LiveData<Integer> getThemeColor() {
        return themeColor;
    }

    public LiveData<Boolean> getNotificationEnabled() {
        return notificationEnabled;
    }

    public LiveData<String> getSound() {
        return sound;
    }

    public LiveData<Boolean> getVibrationEnabled() {
        return vibrationEnabled;
    }

    public LiveData<String> getSnackbarMessage() {
        return snackbarMessage;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}