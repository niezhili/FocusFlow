package com.niezhili.focusflow.ui.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.niezhili.focusflow.data.entity.Task
import com.niezhili.focusflow.ui.task.components.DeleteConfirmDialog
import com.niezhili.focusflow.ui.task.components.SwipeDismissBackground
import com.niezhili.focusflow.ui.task.components.TaskCard
import com.niezhili.focusflow.ui.task.components.TaskInputBar

/**
 * TaskListScreen - 任务列表页
 *
 * 包含：输入栏、任务卡片列表、已完成折叠区
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onNavigateToTimer: (taskId: Long, taskTitle: String) -> Unit = { _, _ -> },
    viewModel: TaskListViewModel = viewModel(
        factory = TaskListViewModelFactory(
            androidx.compose.ui.platform.LocalContext.current.applicationContext
                    as android.app.Application
        )
    )
) {
    val activeTasks: List<Task> by viewModel.activeTasks.observeAsState(emptyList())
    val completedTasks: List<Task> by viewModel.completedTasks.observeAsState(emptyList())
    val isLoading: Boolean by viewModel.isLoading.observeAsState(false)
    val snackbarMessage: String? by viewModel.snackbarMessage.observeAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }

    // Snackbar 消息
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(message = msg)
            viewModel.clearSnackbarMessage()
        }
    }

    // 删除确认弹窗
    taskToDelete?.let { task ->
        DeleteConfirmDialog(
            onConfirm = {
                viewModel.deleteTask(task.getId())
                taskToDelete = null
            },
            onDismiss = { taskToDelete = null }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── 输入栏 ──
            TaskInputBar(
                onTaskCreated = { title -> viewModel.createTask(title) }
            )

            // ── 加载状态 ──
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // ── 任务列表 ──
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // 空状态
                if (activeTasks.isEmpty() && completedTasks.isEmpty() && !isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "还没有任务，输入第一个任务开始吧",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // 活跃任务
                items(
                    items = activeTasks,
                    key = { task: Task -> task.getId() }
                ) { task: Task ->
                    SwipeToDismissTaskCard(
                        task = task,
                        onCompleteToggle = { viewModel.toggleTaskComplete(task) },
                        onStartWork = {
                            onNavigateToTimer(task.getId(), task.getTitle())
                        },
                        onDelete = { taskToDelete = task }
                    )
                }

                // 已完成任务
                if (completedTasks.isNotEmpty()) {
                    items(
                        items = completedTasks,
                        key = { task: Task -> task.getId() }
                    ) { task: Task ->
                        SwipeToDismissTaskCard(
                            task = task,
                            onCompleteToggle = { viewModel.toggleTaskComplete(task) },
                            onStartWork = {},
                            onDelete = { taskToDelete = task }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissTaskCard(
    task: Task,
    onCompleteToggle: () -> Unit,
    onStartWork: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                false // 不自动滑出，手动处理弹窗
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeDismissBackground() },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        TaskCard(
            task = task,
            onCompleteToggle = onCompleteToggle,
            onStartWork = onStartWork
        )
    }
}