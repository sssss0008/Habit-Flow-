package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HabitCyan
import com.example.ui.theme.HabitEmerald
import com.example.ui.theme.HabitGold
import com.example.ui.theme.HabitViolet

data class DailyChartPoint(
    val dayLabel: String,
    val completedCount: Int,
    val totalScheduled: Int,
    val completionRate: Float // 0.0 to 1.0
)

@Composable
fun WeeklyProgressChart(
    points: List<DailyChartPoint>,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000),
        label = "chart_growth_animation"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.TrendingUp,
                            contentDescription = null,
                            tint = HabitCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Weekly Habit Completion Rate",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Last 7 days performance & rate visualization",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Average Rate Pill
                val averageRate = if (points.isNotEmpty()) {
                    (points.map { it.completionRate }.average() * 100).toInt()
                } else 0
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(HabitEmerald.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$averageRate% Avg",
                        color = HabitEmerald,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Interactive Line & Area Waveform Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (points.size >= 2) {
                        val spacing = size.width / (points.size - 1)
                        val chartHeight = size.height * 0.8f
                        val topPadding = size.height * 0.1f

                        val path = Path()
                        val fillPath = Path()

                        val coordinates = points.mapIndexed { index, point ->
                            val x = index * spacing
                            val normalizedY = (1f - (point.completionRate * animatedProgress)) * chartHeight + topPadding
                            Offset(x, normalizedY)
                        }

                        // Grid Reference Lines (25%, 50%, 75%, 100%)
                        listOf(0.25f, 0.5f, 0.75f, 1f).forEach { pct ->
                            val gridY = (1f - pct) * chartHeight + topPadding
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.15f),
                                start = Offset(0f, gridY),
                                end = Offset(size.width, gridY),
                                strokeWidth = 1.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }

                        // Build smooth bezier curves
                        path.moveTo(coordinates[0].x, coordinates[0].y)
                        fillPath.moveTo(coordinates[0].x, size.height)
                        fillPath.lineTo(coordinates[0].x, coordinates[0].y)

                        for (i in 0 until coordinates.size - 1) {
                            val p0 = coordinates[i]
                            val p1 = coordinates[i + 1]
                            val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                            val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)

                            path.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                            fillPath.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                        }

                        fillPath.lineTo(coordinates.last().x, size.height)
                        fillPath.close()

                        // Gradient Area Fill under Curve
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                listOf(
                                    HabitViolet.copy(alpha = 0.35f),
                                    HabitCyan.copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            )
                        )

                        // Smooth Line Stroke
                        drawPath(
                            path = path,
                            brush = Brush.horizontalGradient(listOf(HabitViolet, HabitCyan)),
                            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Draw Anchor Points
                        coordinates.forEachIndexed { idx, coord ->
                            val isSelected = selectedIndex == idx
                            drawCircle(
                                color = if (isSelected) HabitGold else Color.White,
                                radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                                center = coord
                            )
                            drawCircle(
                                color = if (isSelected) HabitGold else HabitViolet,
                                radius = if (isSelected) 3.dp.toPx() else 2.dp.toPx(),
                                center = coord
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Day Labels & Interactive Selectors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                points.forEachIndexed { index, point ->
                    val isSelected = selectedIndex == index
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedIndex = if (isSelected) null else index }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${(point.completionRate * 100).toInt()}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) HabitGold else if (point.completionRate > 0.5f) HabitCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = point.dayLabel,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Selected Day Inspection Tooltip
            selectedIndex?.let { idx ->
                val p = points.getOrNull(idx)
                if (p != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${p.dayLabel}: ${p.completedCount} of ${p.totalScheduled} habits completed",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${(p.completionRate * 100).toInt()}% Complete",
                                color = HabitCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
