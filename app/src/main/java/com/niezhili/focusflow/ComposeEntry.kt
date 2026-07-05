package com.example.myapplication

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.data.preferences.UserPreferences
import com.example.myapplication.ui.MainScreen
import com.example.myapplication.ui.theme.FocusFlowTheme
import com.example.myapplication.ui.theme.ThemeMode
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers

fun ComponentActivity.setFocusFlowContent() {
    setContent {
        val context = LocalContext.current
        val userPreferences = remember { UserPreferences(context.applicationContext) }

        var themeMode by remember { mutableStateOf(ThemeMode.FOLLOW_SYSTEM) }
        var themeColorIndex by remember { mutableIntStateOf(0) }

        DisposableEffect(userPreferences) {
            val themeModeDisposable = userPreferences.dataStore.data()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { prefs ->
                    val mode = prefs[UserPreferences.KEY_THEME_MODE] ?: 0
                    themeMode = ThemeMode.fromValue(mode)
                }

            val themeColorDisposable = userPreferences.dataStore.data()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { prefs ->
                    themeColorIndex = prefs[UserPreferences.KEY_THEME_COLOR] ?: 0
                }

            onDispose {
                themeModeDisposable.dispose()
                themeColorDisposable.dispose()
            }
        }

        FocusFlowTheme(
            themeMode = themeMode,
            themeColorIndex = themeColorIndex
        ) {
            MainScreen()
        }
    }
}