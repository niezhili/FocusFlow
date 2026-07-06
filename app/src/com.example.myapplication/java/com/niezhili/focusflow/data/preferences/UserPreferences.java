package com.niezhili.focusflow.data.preferences;

import android.content.Context;

import androidx.datastore.rxjava3.RxDataStore;
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

/**
 * UserPreferences - DataStore 配置存储
 *
 * 管理主题模式、主题色、通知开关、提示音、震动等用户偏好
 */
public class UserPreferences {

    private static final String PREF_NAME = "focus_flow_settings";

    public final RxDataStore<androidx.datastore.preferences.core.Preferences> dataStore;

    // Keys
    public static final androidx.datastore.preferences.core.Preferences.Key<Integer> KEY_THEME_MODE =
            androidx.datastore.preferences.core.PreferencesKeys.intKey("theme_mode");       // 0=亮色 1=暗色 2=跟随系统

    public static final androidx.datastore.preferences.core.Preferences.Key<Integer> KEY_THEME_COLOR =
            androidx.datastore.preferences.core.PreferencesKeys.intKey("theme_color");      // 0-5

    private static final androidx.datastore.preferences.core.Preferences.Key<Boolean> KEY_NOTIFICATION_ENABLED =
            androidx.datastore.preferences.core.PreferencesKeys.booleanKey("notification_enabled");

    private static final androidx.datastore.preferences.core.Preferences.Key<String> KEY_SOUND =
            androidx.datastore.preferences.core.PreferencesKeys.stringKey("sound");         // "ding", "bell", "chime"

    private static final androidx.datastore.preferences.core.Preferences.Key<Boolean> KEY_VIBRATION_ENABLED =
            androidx.datastore.preferences.core.PreferencesKeys.booleanKey("vibration_enabled");

    public UserPreferences(Context context) {
        this.dataStore = new RxPreferenceDataStoreBuilder(context, PREF_NAME).build();
    }

    // ── Theme Mode ──

    public Single<Integer> getThemeMode() {
        return dataStore.data().firstOrError()
                .map(prefs -> {
                    Integer value = prefs.get(KEY_THEME_MODE);
                    return value != null ? value : 0; // 默认亮色
                });
    }

    public Completable setThemeMode(int mode) {
        return dataStore.updateDataAsync(prefs -> {
            androidx.datastore.preferences.core.MutablePreferences mutablePrefs =
                    prefs.toMutablePreferences();
            mutablePrefs.set(KEY_THEME_MODE, mode);
            return Single.just(mutablePrefs);
        }).ignoreElement();
    }

    // ── Theme Color ──

    public Single<Integer> getThemeColor() {
        return dataStore.data().firstOrError()
                .map(prefs -> {
                    Integer value = prefs.get(KEY_THEME_COLOR);
                    return value != null ? value : 0; // 默认蓝色
                });
    }

    public Completable setThemeColor(int color) {
        return dataStore.updateDataAsync(prefs -> {
            androidx.datastore.preferences.core.MutablePreferences mutablePrefs =
                    prefs.toMutablePreferences();
            mutablePrefs.set(KEY_THEME_COLOR, color);
            return Single.just(mutablePrefs);
        }).ignoreElement();
    }

    // ── Notification ──

    public Single<Boolean> isNotificationEnabled() {
        return dataStore.data().firstOrError()
                .map(prefs -> {
                    Boolean value = prefs.get(KEY_NOTIFICATION_ENABLED);
                    return value != null ? value : true; // 默认开启
                });
    }

    public Completable setNotificationEnabled(boolean enabled) {
        return dataStore.updateDataAsync(prefs -> {
            androidx.datastore.preferences.core.MutablePreferences mutablePrefs =
                    prefs.toMutablePreferences();
            mutablePrefs.set(KEY_NOTIFICATION_ENABLED, enabled);
            return Single.just(mutablePrefs);
        }).ignoreElement();
    }

    // ── Sound ──

    public Single<String> getSound() {
        return dataStore.data().firstOrError()
                .map(prefs -> {
                    String value = prefs.get(KEY_SOUND);
                    return value != null ? value : "ding"; // 默认叮咚
                });
    }

    public Completable setSound(String sound) {
        return dataStore.updateDataAsync(prefs -> {
            androidx.datastore.preferences.core.MutablePreferences mutablePrefs =
                    prefs.toMutablePreferences();
            mutablePrefs.set(KEY_SOUND, sound);
            return Single.just(mutablePrefs);
        }).ignoreElement();
    }

    // ── Vibration ──

    public Single<Boolean> isVibrationEnabled() {
        return dataStore.data().firstOrError()
                .map(prefs -> {
                    Boolean value = prefs.get(KEY_VIBRATION_ENABLED);
                    return value != null ? value : true; // 默认开启
                });
    }

    public Completable setVibrationEnabled(boolean enabled) {
        return dataStore.updateDataAsync(prefs -> {
            androidx.datastore.preferences.core.MutablePreferences mutablePrefs =
                    prefs.toMutablePreferences();
            mutablePrefs.set(KEY_VIBRATION_ENABLED, enabled);
            return Single.just(mutablePrefs);
        }).ignoreElement();
    }
}