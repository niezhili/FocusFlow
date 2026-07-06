package com.niezhili.focusflow.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * FocusFlow 颜色定义
 *
 * 包含 6 种主题色、计时器颜色、亮色/暗色模式表面色
 */

// 6 种主题色
val Blue = Color(0xFF2196F3)
val Orange = Color(0xFFFF9800)
val Purple = Color(0xFF9C27B0)
val Green = Color(0xFF4CAF50)
val Red = Color(0xFFF44336)
val Grey = Color(0xFF607D8B)

val ThemeColors = listOf(Blue, Orange, Purple, Green, Red, Grey)

// 计时器颜色
val TimerIdle = Color(0xFFFFFFFF)      // 白色（未开始）
val TimerRunning = Color(0xFF4CAF50)   // 绿色（计时中）

// 亮色模式
val LightBackground = Color(0xFFFAFAFA)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF1C1B1F)

// 暗色模式
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkOnSurface = Color(0xFFE6E1E5)