package com.niezhili.focusflow.ui.timer.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.niezhili.focusflow.ui.timer.TimerViewModel
import com.niezhili.focusflow.util.TimeFormatter

/**
 * CircularTimer - 环形进度条计时器组件
 *
 * Canvas 绘制环形进度条，弧线从顶部顺时针增长。
 * 整块区域可点击，切换开始/停止计时。
 *
 * 60 分钟 = 360° 完整一圈，超出一圈后弧线保持满圈。
 */
@Composable
fun CircularTimer(
    elapsedSeconds: Long,
    ringColor: Long,
    timerState: TimerViewModel.TimerState,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Dp = 220.dp
) {
    val color = Color(ringColor)

    // 弧线角度：60 分钟 = 360°，超出后保持满圈
    val targetSweepAngle = if (timerState == TimerViewModel.TimerState.IDLE) {
        0f
    } else {
        ((elapsedSeconds % 3600) / 3600f * 360f).coerceAtMost(360f)
    }

    // 平滑动画过渡
    val animatedSweepAngle by animateFloatAsState(
        targetValue = targetSweepAngle,
        animationSpec = tween(durationMillis = 300),
        label = "arcSweep"
    )

    val isRunning = timerState == TimerViewModel.TimerState.RUNNING
    val timeText = TimeFormatter.formatSeconds(elapsedSeconds)
    val statusLabel = when (timerState) {
        TimerViewModel.TimerState.IDLE -> "点击开始"
        TimerViewModel.TimerState.RUNNING -> "专注中"
        TimerViewModel.TimerState.STOPPED -> "已停止"
    }

    Box(
        modifier = modifier
            .size(diameter)
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val strokeWidth = 12.dp.toPx()
            val arcDiameter = size.width - strokeWidth
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

            // 背景圆环
            drawArc(
                color = Color.White.copy(alpha = 0.2f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcDiameter, arcDiameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 前景弧线（计时进度）
            if (animatedSweepAngle > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = animatedSweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcDiameter, arcDiameter),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // 中心文字：时间 + 状态标签
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = if (isRunning) color else Color.White
            )
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}