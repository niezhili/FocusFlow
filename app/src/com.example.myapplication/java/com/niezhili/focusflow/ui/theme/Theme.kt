package com.niezhili.focusflow.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import kotlin.jvm.JvmStatic
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 主题模式枚举
 */
enum class ThemeMode(val value: Int) {
    LIGHT(0),       // 亮色
    DARK(1),        // 暗色
    FOLLOW_SYSTEM(2); // 跟随系统

    companion object {
        @JvmStatic
        fun fromValue(value: Int): ThemeMode {
            return entries.find { it.value == value } ?: FOLLOW_SYSTEM
        }
    }
}

/**
 * 根据主题色索引获取 Color 对象
 */
fun getThemeColor(index: Int): Color {
    return when (index) {
        0 -> Blue
        1 -> Orange
        2 -> Purple
        3 -> Green
        4 -> Red
        5 -> Grey
        else -> Blue
    }
}

/**
 * 构建亮色配色方案
 */
private fun getLightColorScheme(primaryColor: Color) = lightColorScheme(
    primary = primaryColor,
    onPrimary = LightOnSurface,
    background = LightBackground,
    surface = LightSurface,
    onSurface = LightOnSurface
)

/**
 * 构建暗色配色方案
 */
private fun getDarkColorScheme(primaryColor: Color) = darkColorScheme(
    primary = primaryColor,
    onPrimary = DarkOnSurface,
    background = DarkBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface
)

/**
 * FocusFlow 主题
 *
 * @param themeMode 主题模式（亮色/暗色/跟随系统）
 * @param themeColorIndex 主题色索引（0-5）
 * @param content 子 composable
 */
@Composable
fun FocusFlowTheme(
    themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    themeColorIndex: Int = 0,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
    }

    val primaryColor = getThemeColor(themeColorIndex)
    val colorScheme = if (darkTheme) getDarkColorScheme(primaryColor) else getLightColorScheme(primaryColor)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}