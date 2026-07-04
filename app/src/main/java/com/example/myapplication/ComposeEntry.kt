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

/**
 * Compose 入口辅助函数
 *
 * 为 Java 代码提供调用 Compose setContent 的 Kotlin 桥接
 * 从 DataStore 读取用户主题偏好并应用到 FocusFlowTheme
 */
fun ComponentActivity.setFocusFlowContent() {
    setContent {
        val context = LocalContext.current
        val userPreferences = remember { UserPreferences(context.applicationContext) }

        var themeMode by remember { mutableStateOf(ThemeMode.FOLLOW_SYSTEM) }
        var themeColorIndex by remember { mutableIntStateOf(0) }

        DisposableEffect(Unit) {
            // 订阅主题模式
            val themeModeDisposable = userPreferences.getThemeMode()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { mode -> themeMode = ThemeMode.fromValue(mode) }

            // 订阅主题色
            val themeColorDisposable = userPreferences.getThemeColor()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { color -> themeColorIndex = color }

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