package com.vinote.ui.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vinote.domain.finance.DailyTrendPoint
import com.vinote.ui.components.FormatUtils
import com.vinote.ui.theme.ViNoteMintSuccess
import com.vinote.ui.theme.ViNotePrimary
import com.vinote.ui.theme.ViNoteSoftPink
import com.vinote.ui.theme.ViNoteSurfaceContainerLow
import com.vinote.ui.theme.ViNoteTextPrimary
import com.vinote.ui.theme.ViNoteTextSecondary
import com.vinote.ui.theme.ViNoteWarmYellow
import kotlin.math.cos
import kotlin.math.sin

private val ChartColors = listOf(
    ViNotePrimary,
    ViNoteMintSuccess,
    ViNoteWarmYellow,
    ViNoteSoftPink,
    Color(0xFF9C27B0),
    Color(0xFF00BCD4),
    Color(0xFFFF9800),
    Color(0xFF607D8B)
)

/**
 * Native Canvas-based 30-Day Line Chart with bezier curve and fill gradient.
 */
@Composable
fun LineChart30Days(
    points: List<DailyTrendPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(1000))
    }

    val maxVal = (points.maxOfOrNull { it.amount } ?: 1L).coerceAtLeast(10_000L).toFloat()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ViNoteSurfaceContainerLow)
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val spacing = width / (points.size - 1).coerceAtLeast(1)

            val strokePath = Path()
            val fillPath = Path()

            val coordinates = points.mapIndexed { index, point ->
                val x = index * spacing
                val normalizedY = (point.amount.toFloat() / maxVal).coerceIn(0f, 1f) * animatedProgress.value
                val y = height - (normalizedY * (height - 20.dp.toPx())) - 10.dp.toPx()
                Offset(x, y)
            }

            if (coordinates.isNotEmpty()) {
                strokePath.moveTo(coordinates[0].x, coordinates[0].y)
                fillPath.moveTo(coordinates[0].x, height)
                fillPath.lineTo(coordinates[0].x, coordinates[0].y)

                for (i in 0 until coordinates.size - 1) {
                    val p0 = coordinates[i]
                    val p1 = coordinates[i + 1]
                    val controlPoint1 = Offset((p0.x + p1.x) / 2f, p0.y)
                    val controlPoint2 = Offset((p0.x + p1.x) / 2f, p1.y)

                    strokePath.cubicTo(
                        controlPoint1.x, controlPoint1.y,
                        controlPoint2.x, controlPoint2.y,
                        p1.x, p1.y
                    )
                    fillPath.cubicTo(
                        controlPoint1.x, controlPoint1.y,
                        controlPoint2.x, controlPoint2.y,
                        p1.x, p1.y
                    )
                }

                fillPath.lineTo(coordinates.last().x, height)
                fillPath.close()

                // Draw gradient under curve
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            ViNotePrimary.copy(alpha = 0.35f),
                            ViNotePrimary.copy(alpha = 0.02f)
                        )
                    )
                )

                // Draw line stroke
                drawPath(
                    path = strokePath,
                    color = ViNotePrimary,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw accent dot on peak point
                val maxPointIndex = points.indexOfMaxByOrNull { it.amount } ?: 0
                if (maxPointIndex in coordinates.indices) {
                    val peakOffset = coordinates[maxPointIndex]
                    drawCircle(
                        color = ViNotePrimary,
                        radius = 6.dp.toPx(),
                        center = peakOffset
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = peakOffset
                    )
                }
            }
        }
    }
}

/**
 * Native Canvas-based Category Donut Chart with legend.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryDonutChart(
    breakdown: Map<String, Long>,
    modifier: Modifier = Modifier
) {
    if (breakdown.isEmpty()) return

    val total = breakdown.values.sum().coerceAtLeast(1L).toFloat()
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(breakdown) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(900))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(140.dp)) {
                val strokeWidth = 24.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)

                var startAngle = -90f
                var colorIdx = 0

                for ((_, amount) in breakdown) {
                    val sweepAngle = (amount.toFloat() / total) * 360f * animatedProgress.value
                    val color = ChartColors[colorIdx % ChartColors.size]

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )

                    startAngle += sweepAngle
                    colorIdx++
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total",
                    fontSize = 11.sp,
                    color = ViNoteTextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = FormatUtils.formatRupiah(total.toLong()),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ViNoteTextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend Flow
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            var idx = 0
            for ((category, amount) in breakdown) {
                val color = ChartColors[idx % ChartColors.size]
                val percent = ((amount.toFloat() / total) * 100).toInt()

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$category $percent%",
                        fontSize = 12.sp,
                        color = ViNoteTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
                idx++
            }
        }
    }
}

/**
 * Financial Health Score Speedometer Gauge (PRD Section 3.2).
 * Zones:
 * - 0-40: Merah (Perlu Perhatian)
 * - 41-75: Kuning (Cukup Sehat)
 * - 76-100: Hijau (Sangat Sehat)
 */
@Composable
fun FinancialHealthGauge(
    score: Int,
    modifier: Modifier = Modifier
) {
    val clampedScore = score.coerceIn(0, 100)
    val animatedScore = remember { Animatable(0f) }

    LaunchedEffect(clampedScore) {
        animatedScore.animateTo(clampedScore.toFloat(), animationSpec = tween(1200))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(200.dp, 120.dp)) {
            val strokeWidth = 16.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, (size.height * 2) - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            // Red Zone: 180° to 252° (40%)
            drawArc(
                color = Color(0xFFEF5350),
                startAngle = 180f,
                sweepAngle = 72f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Yellow Zone: 252° to 315° (35%)
            drawArc(
                color = ViNoteWarmYellow,
                startAngle = 252f,
                sweepAngle = 63f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )

            // Green Zone: 315° to 360° (25%)
            drawArc(
                color = ViNoteMintSuccess,
                startAngle = 315f,
                sweepAngle = 45f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Needle pointer calculation
            val angleRad = Math.toRadians((180f + (animatedScore.value / 100f * 180f)).toDouble())
            val needleRadius = (size.width / 2f) - strokeWidth - 6.dp.toPx()
            val centerX = size.width / 2f
            val centerY = size.height

            val needleEnd = Offset(
                x = (centerX + needleRadius * cos(angleRad)).toFloat(),
                y = (centerY + needleRadius * sin(angleRad)).toFloat()
            )

            drawLine(
                color = Color(0xFF171827),
                start = Offset(centerX, centerY),
                end = needleEnd,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Color(0xFF171827),
                radius = 6.dp.toPx(),
                center = Offset(centerX, centerY)
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${clampedScore}/100",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ViNoteTextPrimary
            )
            val label = when {
                clampedScore >= 75 -> "Sangat Sehat 🌟"
                clampedScore >= 45 -> "Cukup Baik 👍"
                else -> "Perlu Perhatian ⚠️"
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    clampedScore >= 75 -> ViNoteMintSuccess
                    clampedScore >= 45 -> ViNoteWarmYellow
                    else -> Color(0xFFEF5350)
                }
            )
        }
    }
}

private inline fun <T> List<T>.indexOfMaxByOrNull(selector: (T) -> Long): Int? {
    if (isEmpty()) return null
    var maxIndex = 0
    var maxValue = selector(this[0])
    for (i in 1 until size) {
        val value = selector(this[i])
        if (value > maxValue) {
            maxValue = value
            maxIndex = i
        }
    }
    return maxIndex
}
