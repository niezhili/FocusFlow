package com.niezhili.focusflow.ui.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.niezhili.focusflow.data.dao.TaskDistribution
import com.niezhili.focusflow.util.TimeFormatter

/**
 * TaskDistributionChart - 任务分布环形图
 *
 * 展示各任务耗时占比
 */
@Composable
fun TaskDistributionChart(
    distribution: List<TaskDistribution>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "任务分布",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (distribution.isEmpty() || distribution.all { it.totalSeconds == 0L }) {
                Text(
                    text = "暂无数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                val totalSeconds = distribution.sumOf { it.totalSeconds }
                val chartColors = listOf(
                    Color(0xFF2196F3),
                    Color(0xFFFF9800),
                    Color(0xFF9C27B0),
                    Color(0xFF4CAF50),
                    Color(0xFFF44336),
                    Color(0xFF607D8B),
                    Color(0xFF00BCD4),
                    Color(0xFFFF5722)
                )

                val surfaceColor = MaterialTheme.colorScheme.onSurface

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(160.dp)
                            .padding(8.dp)
                    ) {
                        val canvasSize = size.minDimension
                        val strokeWidth = canvasSize * 0.25f
                        val radius = (canvasSize - strokeWidth) / 2
                        val center = Offset(canvasSize / 2, canvasSize / 2)

                        var startAngle = -90f

                        distribution.forEachIndexed { index, item ->
                            val sweepAngle = if (totalSeconds > 0) {
                                (item.totalSeconds.toFloat() / totalSeconds) * 360f
                            } else {
                                0f
                            }

                            drawArc(
                                color = chartColors[index % chartColors.size],
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = Offset(
                                    center.x - radius,
                                    center.y - radius
                                ),
                                size = Size(radius * 2, radius * 2),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )

                            startAngle += sweepAngle
                        }

                        val paint = android.graphics.Paint().apply {
                            color = surfaceColor.hashCode()
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            TimeFormatter.formatDuration(totalSeconds),
                            center.x,
                            center.y + 10f,
                            paint
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        distribution.take(6).forEachIndexed { index, item ->
                            val percentage = if (totalSeconds > 0) {
                                (item.totalSeconds.toFloat() / totalSeconds * 100).toInt()
                            } else {
                                0
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Canvas(modifier = Modifier.size(12.dp)) {
                                    drawRect(
                                        color = chartColors[index % chartColors.size],
                                        size = size
                                    )
                                }
                                Text(
                                    text = "${item.taskTitle} $percentage%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(start = 6.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}