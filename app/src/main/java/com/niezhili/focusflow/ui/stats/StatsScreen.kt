package com.example.myapplication.ui.stats

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.stats.components.DailyChart
import com.example.myapplication.ui.stats.components.ChartViewType
import com.example.myapplication.ui.stats.components.SummaryCard
import com.example.myapplication.ui.stats.components.TaskDistributionChart
import kotlinx.coroutines.delay

/**
 * StatsScreen - 统计页
 *
 * 展示今日概览、日/周/月统计图表、任务分布环形图
 * 支持下拉刷新数据
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = viewModel(
        factory = StatsViewModelFactory(
            LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val totalFocusSeconds: Long by viewModel.todayTotalFocusSeconds.observeAsState(0L)
    val sessionCount: Int by viewModel.todaySessionCount.observeAsState(0)
    val completedTaskCount: Int by viewModel.todayCompletedTaskCount.observeAsState(0)
    val consecutiveDays: Int by viewModel.consecutiveDays.observeAsState(0)
    val dailyStats: List<com.example.myapplication.data.dao.DailyStats> by viewModel.dailyStats.observeAsState(emptyList())
    val taskDistribution: List<com.example.myapplication.data.dao.TaskDistribution> by viewModel.taskDistribution.observeAsState(emptyList())
    val isLoading: Boolean by viewModel.isLoading.observeAsState(false)

    var selectedTabIndex by remember { mutableIntStateOf(1) }

    val tabs = listOf("日", "周", "月")

    // ── 下拉刷新 ──
    val pullToRefreshState = rememberPullToRefreshState()

    // 监听下拉手势，触发刷新
    LaunchedEffect(Unit) {
        snapshotFlow { pullToRefreshState.progress }
            .collect { progress ->
                if (progress >= 1f && !pullToRefreshState.isRefreshing) {
                    pullToRefreshState.startRefresh()
                    viewModel.refresh()
                    delay(300)
                    pullToRefreshState.endRefresh()
                }
            }
    }

    // 进入页面时自动刷新数据
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pullToRefreshState.nestedScrollConnection)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (isLoading && !pullToRefreshState.isRefreshing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            SummaryCard(
                totalFocusSeconds = totalFocusSeconds,
                sessionCount = sessionCount,
                completedTaskCount = completedTaskCount,
                consecutiveDays = consecutiveDays
            )

            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                edgePadding = 0.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                            val range = when (index) {
                                0 -> StatsViewModel.TimeRange.DAY
                                1 -> StatsViewModel.TimeRange.WEEK
                                2 -> StatsViewModel.TimeRange.MONTH
                                else -> StatsViewModel.TimeRange.WEEK
                            }
                            viewModel.switchTimeRange(range)
                        },
                        text = { Text(title) }
                    )
                }
            }

            DailyChart(
                stats = dailyStats,
                viewType = when (selectedTabIndex) {
                    0 -> ChartViewType.DAY
                    1 -> ChartViewType.WEEK
                    2 -> ChartViewType.MONTH
                    else -> ChartViewType.WEEK
                },
                taskDistribution = taskDistribution
            )

            TaskDistributionChart(distribution = taskDistribution)
        }

        // 下拉刷新指示器
        if (pullToRefreshState.progress > 0f || pullToRefreshState.isRefreshing) {
            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}