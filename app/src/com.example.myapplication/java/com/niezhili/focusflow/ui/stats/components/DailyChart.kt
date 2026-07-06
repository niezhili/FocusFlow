package com.niezhili.focusflow.ui.stats.components

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
import com.niezhili.focusflow.data.dao.DailyStats
import com.niezhili.focusflow.data.dao.TaskDistribution
import com.niezhili.focusflow.util.TimeFormatter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * DailyChart - 专注时长图表
 *
 * 日视图：按任务名称匹配环形图颜色的柱状图
 * 周视图：统一主题色的柱状图
 * 月视图：主题色折线图
 */
@Composable
fun DailyChart(
    stats: List<DailyStats>,
    viewType: ChartViewType = ChartViewType.WEEK,
    taskDistribution: List<TaskDistribution> = emptyList(),
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
                val barColor = MaterialTheme.colorScheme.primary
                val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                val onSurfaceDimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

                // 与环形图一致的颜色调色板
                val chartColors = listOf(
                    Color(0xFF2196F3), // Blue
                    Color(0xFFFF9800), // Orange
                    Color(0xFF9C27B0), // Purple
                    Color(0xFF4CAF50), // Green
                    Color(0xFFF44336), // Red
                    Color(0xFF607D8B), // Blue Grey
                    Color(0xFF00BCD4), // Cyan
                    Color(0xFFFF5722)  // Deep Orange
                )

                // 日视图：按任务标题匹配环形图颜色索引
                val taskColorIndexMap = if (viewType == ChartViewType.DAY) {
                    taskDistribution.withIndex().associate { (index, item) ->
                        item.taskTitle to index
                    }
                } else {
                    emptyMap()
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(top = 8.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    if (viewType == ChartViewType.MONTH) {
                        // ── 月视图：折线图 ──
                        val hPadding = 40f
                        val chartWidth = canvasWidth - hPadding * 2
                        val topPadding = 30f
                        val bottomPadding = 40f
                        val chartHeight = canvasHeight - bottomPadding - topPadding

                        val points = stats.mapIndexed { index, stat ->
                            val x = if (stats.size > 1) {
                                hPadding + index * (chartWidth / (stats.size - 1))
                            } else {
                                canvasWidth / 2
                            }
                            val y = if (maxTotal > 0) {
                                canvasHeight - bottomPadding - (stat.total.toFloat() / maxTotal) * chartHeight
                            } else {
                                canvasHeight - bottomPadding
                            }
                            Offset(x, y)
                        }

                        // 连接线
                        for (i in 0 until points.size - 1) {
                            drawLine(
                                color = barColor,
                                start = points[i],
                                end = points[i + 1],
                                strokeWidth = 3f
                            )
                        }

                        stats.forEachIndexed { index, stat ->
                            val point = points[index]

                            // 数据点外圈
                            drawCircle(
                                color = barColor,
                                radius = 6f,
                                center = point
                            )
                            // 数据点内圈
                            drawCircle(
                                color = Color.White,
                                radius = 3f,
                                center = point
                            )

                            // 数值
                            if (stat.total > 0) {
                                val paint = android.graphics.Paint().apply {
                                    this.color = onSurfaceColor.hashCode()
                                    textSize = 22f
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isAntiAlias = true
                                }
                                drawContext.canvas.nativeCanvas.drawText(
                                    TimeFormatter.formatDuration(stat.total),
                                    point.x,
                                    point.y - 14f,
                                    paint
                                )
                            }

                            // 日期标签（主题色）
                            val datePaint = android.graphics.Paint().apply {
                                this.color = barColor.hashCode()
                                textSize = 20f
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                            val dateLabel = stat.date.substring(stat.date.lastIndexOf('-') + 1)
                            drawContext.canvas.nativeCanvas.drawText(
                                dateLabel,
                                point.x,
                                canvasHeight - 4f,
                                datePaint
                            )
                        }
                    } else {
                        // ── 日/周视图：柱状图 ──
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

                            // 日视图按任务颜色，周视图统一主题色
                            val color = if (viewType == ChartViewType.DAY) {
                                val colorIndex = taskColorIndexMap[stat.date] ?: -1
                                if (colorIndex >= 0) chartColors[colorIndex % chartColors.size]
                                else barColor
                            } else {
                                barColor
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
                            val shortDate = when (viewType) {
                                ChartViewType.DAY -> {
                                    if (stat.date.length > 4) stat.date.take(4) + ".."
                                    else stat.date
                                }
                                ChartViewType.WEEK -> dateToWeekday(stat.date)
                                ChartViewType.MONTH -> stat.date.substring(stat.date.lastIndexOf('-') + 1)
                            }
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
}

/**
 * 图表视图类型，影响横轴标签格式
 */
enum class ChartViewType {
    /** 日视图 — 标签为任务名称 */
    DAY,
    /** 周视图 — 标签为周几，如 "周一" */
    WEEK,
    /** 月视图 — 标签为日期数字，如 "5" */
    MONTH
}

private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

private val weekdayLabels = mapOf(
    DayOfWeek.MONDAY to "周一",
    DayOfWeek.TUESDAY to "周二",
    DayOfWeek.WEDNESDAY to "周三",
    DayOfWeek.THURSDAY to "周四",
    DayOfWeek.FRIDAY to "周五",
    DayOfWeek.SATURDAY to "周六",
    DayOfWeek.SUNDAY to "周日"
)

private fun dateToWeekday(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr, dateFormatter)
        weekdayLabels[date.dayOfWeek] ?: dateStr
    } catch (e: Exception) {
        dateStr
    }
}