package com.example.myapplication.ui.task.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.entity.Task
import com.example.myapplication.util.TimeFormatter

/**
 * TaskCard - 任务卡片组件
 *
 * 展示任务标题、截止日期、专注时长，支持完成切换和开始工作
 */
@Composable
fun TaskCard(
    task: Task,
    onCompleteToggle: () -> Unit,
    onStartWork: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOverdue = task.getDueDate() != null && task.getDueDate()!! < System.currentTimeMillis()
    val isCompleted = task.isCompleted()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 完成复选框
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { onCompleteToggle() }
            )

            // 中间内容
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                // 任务标题
                Text(
                    text = task.getTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCompleted)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                // 副标题行：截止日期 + 专注时长
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 截止日期
                    if (task.getDueDate() != null) {
                        Text(
                            text = "截止: ${TimeFormatter.formatDate(task.getDueDate()!!)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isOverdue && !isCompleted)
                                Color(0xFFF44336) // 红色
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    // 累计专注时长
                    if (task.getTotalFocusSeconds() > 0) {
                        Text(
                            text = "已专注 ${TimeFormatter.formatDuration(task.getTotalFocusSeconds())}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // 开始工作按钮
            if (!isCompleted) {
                Button(
                    onClick = onStartWork,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("开始工作")
                }
            }
        }
    }
}

/**
 * SwipeDismissBackground - 滑动删除背景
 */
@Composable
fun SwipeDismissBackground(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF44336))
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(
            text = "删除",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
    }
}