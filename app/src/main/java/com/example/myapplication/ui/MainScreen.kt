package com.example.myapplication.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.ui.navigation.AppNavigation
import com.example.myapplication.ui.navigation.bottomNavItems
import com.example.myapplication.ui.settings.SettingsScreen
import com.example.myapplication.ui.stats.StatsScreen
import com.example.myapplication.ui.task.TaskListScreen
import com.example.myapplication.ui.timer.TimerScreen

/**
 * MainScreen - 主屏幕
 *
 * 包含底部导航栏（任务/统计/设置）和 NavHost
 */
@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                // 在计时页隐藏底部导航栏
                val showBottomBar = currentDestination?.route != AppNavigation.ROUTE_TIMER

                if (showBottomBar) {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppNavigation.ROUTE_TASK_LIST,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 任务列表
            composable(AppNavigation.ROUTE_TASK_LIST) {
                TaskListScreen(
                    onNavigateToTimer = { taskId, taskTitle ->
                        navController.navigate(
                            AppNavigation.timerRoute(taskId, taskTitle)
                        )
                    }
                )
            }

            // 计时页
            composable(
                route = AppNavigation.ROUTE_TIMER,
                arguments = listOf(
                    navArgument("taskId") { type = NavType.LongType },
                    navArgument("taskTitle") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
                val taskTitle = backStackEntry.arguments?.getString("taskTitle") ?: ""
                TimerScreen(
                    taskId = taskId,
                    taskTitle = taskTitle,
                    onBack = { navController.popBackStack() }
                )
            }

            // 统计
            composable(AppNavigation.ROUTE_STATS) {
                StatsScreen()
            }

            // 设置
            composable(AppNavigation.ROUTE_SETTINGS) {
                SettingsScreen()
            }
        }
    }
}