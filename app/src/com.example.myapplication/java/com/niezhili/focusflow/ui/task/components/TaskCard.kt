package com.niezhili.focusflow.ui.task.components

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
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.niezhili.focusflow.data.entity.Task
import com.niezhili.focusflow.util.TimeFormatter

/**
 * 环形图调色板 — 与 TaskDistributionChart / DailyChart 保持一致
 */
private val ChartColors = listOf(
    Color(0xFF2196F3), // Blue
    Color(0xFFFF9800), // Orange
    Color(0xFF9C27B0), // Purple
    Color(0xFF4CAF50), // Green
    Color(0xFFF44336), // Red
    Color(0xFF607D8B), // Blue Grey
    Color(0xFF00BCD4), // Cyan
    Color(0xFFFF5722)  // Deep Orange
)

/**
 * TaskCard - 任务卡片组件
 *
 * 展示任务标题、截止日期、专注时长，支持完成切换和开始工作。
 * 卡片背景色取自环形图调色板，每个任务根据 ID 固定分配一种颜色。
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
    val cardColor = ChartColors[(task.getId() % ChartColors.size).toInt()]

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
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
                onCheckedChange = { onCompleteToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color.White,
                    uncheckedColor = Color.White.copy(alpha = 0.7f),
                    checkmarkColor = cardColor
                )
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
                        Color.White.copy(alpha = 0.5f)
                    else
                        Color.White,
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
                                Color(0xFFFFEB3B) // 黄色警告，在彩色背景上更醒目
                            else
                                Color.White.copy(alpha = 0.7f)
                        )
                    }

                    // 累计专注时长
                    if (task.getTotalFocusSeconds() > 0) {
                        Text(
                            text = "已专注 ${TimeFormatter.formatDuration(task.getTotalFocusSeconds())}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // 开始工作按钮
            if (!isCompleted) {
                Button(
                    onClick = onStartWork,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = cardColor
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
            .background(Color.Transparent)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Text(
            text = "删除",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.titleMedium
        )
    }
}