package com.niezhili.focusflow.ui.timer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.niezhili.focusflow.ui.timer.components.CircularTimer

/**
 * TimerScreen - 计时页
 *
 * 顶部：返回箭头 + 任务标题
 * 居中：环形进度条（点击切换开始/停止）
 *
 * 返回或退出时自动停止计时并保存数据
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    taskId: Long,
    taskTitle: String,
    onBack: () -> Unit,
    viewModel: TimerViewModel = viewModel(
        factory = TimerViewModelFactory(
            androidx.compose.ui.platform.LocalContext.current.applicationContext
                    as android.app.Application
        )
    )
) {
    val timerState: TimerViewModel.TimerState? by viewModel.timerState.observeAsState()
    val elapsedSeconds: Long by viewModel.elapsedSeconds.observeAsState(0L)
    val title: String by viewModel.taskTitle.observeAsState(taskTitle)
    val ringColor: Long by viewModel.ringColor.observeAsState(TimerViewModel.COLOR_IDLE)

    // 初始化 ViewModel
    LaunchedEffect(taskId) {
        viewModel.init(taskId)
    }

    // 返回键处理：自动停止计时并保存
    BackHandler {
        viewModel.stopAndSave()
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stopAndSave()
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF1A1A2E),
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 环形计时器（整块区域可点击切换）
            CircularTimer(
                elapsedSeconds = elapsedSeconds,
                ringColor = ringColor,
                timerState = timerState ?: TimerViewModel.TimerState.IDLE,
                onToggle = { viewModel.toggleTimer() }
            )

            // 底部提示文字
            val hintText = when (timerState) {
                TimerViewModel.TimerState.IDLE -> "点击环形区域开始计时"
                TimerViewModel.TimerState.RUNNING -> "点击环形区域停止计时"
                TimerViewModel.TimerState.STOPPED -> "计时已停止，点击重新开始"
                null -> ""
            }

            Text(
                text = hintText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 32.dp)
            )
        }
    }
}