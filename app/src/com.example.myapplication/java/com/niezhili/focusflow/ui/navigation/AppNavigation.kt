package com.niezhili.focusflow.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * AppNavigation - 导航路由定义
 *
 * 定义底部导航项和路由常量
 */
object AppNavigation {

    // 路由常量
    const val ROUTE_TASK_LIST = "task_list"
    const val ROUTE_TIMER = "timer/{taskId}/{taskTitle}"
    const val ROUTE_STATS = "stats"
    const val ROUTE_SETTINGS = "settings"

    /**
     * 构建计时页路由
     */
    fun timerRoute(taskId: Long, taskTitle: String): String {
        return "timer/$taskId/$taskTitle"
    }
}

/**
 * 底部导航项
 */
sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    data object Tasks : BottomNavItem(
        route = AppNavigation.ROUTE_TASK_LIST,
        icon = Icons.Filled.CheckCircle,
        label = "任务"
    )

    data object Stats : BottomNavItem(
        route = AppNavigation.ROUTE_STATS,
        icon = Icons.Filled.Star,
        label = "统计"
    )

    data object Settings : BottomNavItem(
        route = AppNavigation.ROUTE_SETTINGS,
        icon = Icons.Filled.Settings,
        label = "设置"
    )
}

val bottomNavItems = listOf(
    BottomNavItem.Tasks,
    BottomNavItem.Stats,
    BottomNavItem.Settings
)