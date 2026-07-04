package com.example.myapplication.ui.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.dao.DailyStats
import com.example.myapplication.util.TimeFormatter

/**
 * DailyChart - 柱状图组件
 *
 * 展示每日专注时长，标注最高/最低日
 */
@Composable
fun DailyChart(
    stats: List<DailyStats>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "专注时长趋势",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            if (stats.isEmpty()) {
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
                val maxTotal = stats.maxOf { it.total }
                val minTotal = if (stats.size > 1) stats.minOf { it.total } else maxTotal
                val barColor = MaterialTheme.colorScheme.primary
                val barColorMax = Color(0xFF4CAF50)   // 绿色 — 最高日
                val barColorMin = Color(0xFFFF9800)   // 橙色 — 最低日
                val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                val onSurfaceDimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(top = 8.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val barWidth = canvasWidth / stats.size * 0.6f
                    val barSpacing = canvasWidth / stats.size

                    stats.forEachIndexed { index, stat ->
                        val barHeight = if (maxTotal > 0) {
                            (stat.total.toFloat() / maxTotal) * (canvasHeight - 60)
                        } else {
                            0f
                        }

                        val x = index * barSpacing + (barSpacing - barWidth) / 2
                        val y = canvasHeight - barHeight - 40

                        // 根据是否为最大/最小值选择颜色
                        val color = when {
                            stats.size > 1 && stat.total == maxTotal -> barColorMax
                            stats.size > 1 && stat.total == minTotal && maxTotal != minTotal -> barColorMin
                            else -> barColor
                        }

                        drawRect(
                            color = color,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight)
                        )

                        if (stat.total > 0) {
                            val paint = android.graphics.Paint().apply {
                                this.color = onSurfaceColor.hashCode()
                                textSize = 22f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                            drawContext.canvas.nativeCanvas.drawText(
                                TimeFormatter.formatDuration(stat.total),
                                x + barWidth / 2,
                                y - 8f,
                                paint
                            )
                        }

                        val datePaint = android.graphics.Paint().apply {
                            this.color = onSurfaceDimColor.hashCode()
                            textSize = 20f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }
                        val shortDate = stat.date.substring(5)
                        drawContext.canvas.nativeCanvas.drawText(
                            shortDate,
                            x + barWidth / 2,
                            canvasHeight - 4f,
                            datePaint
                        )
                    }
                }
            }
        }
    }
}