package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CoolingSessionEntity
import com.example.data.local.ThermalLogEntity
import com.example.ui.components.MetricStatTile
import com.example.ui.theme.FrostDarkSurfaceVariant
import com.example.ui.theme.FrostPrimary
import com.example.ui.theme.FrostSecondary
import com.example.ui.theme.FrostTertiary
import com.example.ui.theme.ThermalCritical
import com.example.ui.theme.ThermalOptimal
import com.example.ui.viewmodel.ThermalUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsScreen(
    uiState: ThermalUiState,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.settings
    val logs = uiState.recentLogs.reversed() // oldest to newest for graph
    val sessions = uiState.coolingSessions
    val useF = settings.useFahrenheit
    val unit = if (useF) "°F" else "°C"

    val maxTempC = uiState.maxTemperature ?: uiState.snapshot.temperatureC
    val avgTempC = uiState.avgTemperature ?: uiState.snapshot.temperatureC
    val totalRamFreedMb = uiState.totalRamFreedMb ?: 0L

    val maxDisplay = if (useF) (maxTempC * 9/5 + 32) else maxTempC
    val avgDisplay = if (useF) (avgTempC * 9/5 + 32) else avgTempC

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .testTag("analytics_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Thermal Analytics",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (logs.isNotEmpty() || sessions.isNotEmpty()) {
                    IconButton(onClick = onClearHistory) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear history",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // Summary Metric Tiles
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatTile(
                    title = "Peak Temperature",
                    value = String.format("%.1f%s", maxDisplay, unit),
                    subtitle = if (maxTempC > 41f) "Thermal spike detected" else "Within normal boundary",
                    accentColor = if (maxTempC > 41f) ThermalCritical else FrostSecondary,
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = ThermalCritical,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                MetricStatTile(
                    title = "Avg Temperature",
                    value = String.format("%.1f%s", avgDisplay, unit),
                    subtitle = "Historical running average",
                    accentColor = FrostTertiary,
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Thermostat,
                            contentDescription = null,
                            tint = FrostTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatTile(
                    title = "RAM Freed",
                    value = "${totalRamFreedMb} MB",
                    subtitle = "Reclaimed by FrostGuard",
                    accentColor = FrostPrimary,
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AcUnit,
                            contentDescription = null,
                            tint = FrostPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                MetricStatTile(
                    title = "Cooling Sessions",
                    value = "${sessions.size}",
                    subtitle = "Optimization cycles",
                    accentColor = Color.White,
                    modifier = Modifier.weight(1f),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = FrostSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }

        // Live Temperature Trend Graph
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = FrostDarkSurfaceVariant),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Thermal Trend Timeline",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .border(1.dp, ThermalCritical)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Limit: ${String.format("%.0f%s", if (useF) (settings.alertThresholdC * 9/5 + 32) else settings.alertThresholdC, unit)}",
                                fontSize = 11.sp,
                                color = ThermalCritical
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    TemperatureLineChart(
                        logs = logs,
                        alertThresholdC = settings.alertThresholdC,
                        useFahrenheit = useF,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                }
            }
        }

        // Past Cooling Sessions History List
        item {
            Text(
                text = "Cooling History (${sessions.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (sessions.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = FrostDarkSurfaceVariant.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AcUnit,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No cooling sessions recorded yet",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Tap 'Cool Down Phone' on the dashboard to run a cycle.",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            items(sessions, key = { it.id }) { session ->
                CoolingSessionCard(session = session, useFahrenheit = useF)
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun TemperatureLineChart(
    logs: List<ThermalLogEntity>,
    alertThresholdC: Float,
    useFahrenheit: Boolean,
    modifier: Modifier = Modifier
) {
    val samplePoints = if (logs.isNotEmpty()) {
        logs.map { it.temperatureC }
    } else {
        // Fallback default sample curve
        listOf(33.2f, 34.0f, 35.5f, 38.0f, 40.2f, 37.1f, 35.0f, 34.2f)
    }

    val minT = (samplePoints.minOrNull() ?: 30f).coerceAtMost(30f)
    val maxT = ((samplePoints.maxOrNull() ?: 45f).coerceAtLeast(alertThresholdC + 3f))
    val range = (maxT - minT).coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Draw dotted threshold line
        val thresholdY = h - ((alertThresholdC - minT) / range) * h
        drawLine(
            color = ThermalCritical.copy(alpha = 0.8f),
            start = Offset(0f, thresholdY),
            end = Offset(w, thresholdY),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
        )

        // Draw Temperature Line & Gradient fill
        if (samplePoints.size >= 2) {
            val stepX = w / (samplePoints.size - 1)
            val path = Path()
            val fillPath = Path()

            samplePoints.forEachIndexed { i, temp ->
                val x = i * stepX
                val y = h - ((temp - minT) / range) * h

                if (i == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, h)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }

                // Point circle
                drawCircle(
                    color = if (temp >= alertThresholdC) ThermalCritical else FrostPrimary,
                    radius = 3.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            fillPath.lineTo(w, h)
            fillPath.close()

            // Fill gradient below curve
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    listOf(FrostPrimary.copy(alpha = 0.25f), Color.Transparent)
                )
            )

            // Stroke line
            drawPath(
                path = path,
                color = FrostPrimary,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun CoolingSessionCard(
    session: CoolingSessionEntity,
    useFahrenheit: Boolean
) {
    val tempBefore = if (useFahrenheit) (session.tempBeforeC * 9/5 + 32) else session.tempBeforeC
    val tempAfter = if (useFahrenheit) (session.tempAfterC * 9/5 + 32) else session.tempAfterC
    val delta = tempBefore - tempAfter
    val unit = if (useFahrenheit) "°F" else "°C"
    val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(session.timestamp))

    Card(
        colors = CardDefaults.cardColors(containerColor = FrostDarkSurfaceVariant),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x2238BDF8), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session.mode,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = ThermalOptimal.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = String.format("-%.1f%s", delta, unit),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ThermalOptimal,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$dateStr • ${session.processesTerminatedCount} Tasks Closed • +${session.ramFreedMb} MB RAM",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Text(
                text = String.format("%.1f%s → %.1f%s", tempBefore, unit, tempAfter, unit),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = FrostSecondary
            )
        }
    }
}
