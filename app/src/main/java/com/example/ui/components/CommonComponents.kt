package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ThermalLevel
import com.example.ui.theme.FrostDarkSurface
import com.example.ui.theme.FrostDarkSurfaceVariant
import com.example.ui.theme.FrostPrimary
import com.example.ui.theme.FrostSecondary
import com.example.ui.theme.FrostTertiary
import com.example.ui.theme.ThermalCritical
import com.example.ui.theme.ThermalHeated
import com.example.ui.theme.ThermalOptimal
import com.example.ui.theme.ThermalWarm

@Composable
fun ThermalDial(
    temperatureC: Float,
    useFahrenheit: Boolean,
    isCooling: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 230.dp
) {
    val displayTemp = if (useFahrenheit) (temperatureC * 9f / 5f) + 32f else temperatureC
    val tempUnit = if (useFahrenheit) "°F" else "°C"

    val animatedTemp by animateFloatAsState(
        targetValue = displayTemp,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "temp_anim"
    )

    // Gauge fraction: 25°C to 50°C mapping
    val fraction = ((temperatureC - 25f) / 25f).coerceIn(0f, 1f)
    val animatedSweep by animateFloatAsState(
        targetValue = fraction * 260f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "sweep_anim"
    )

    val dialColor = when {
        temperatureC >= 43.0f -> ThermalCritical
        temperatureC >= 40.0f -> ThermalHeated
        temperatureC >= 37.0f -> ThermalWarm
        else -> ThermalOptimal
    }

    val animatedDialColor by animateColorAsState(
        targetValue = dialColor,
        animationSpec = tween(500),
        label = "dial_color"
    )

    // Pulsing ring if cooling or hot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isCooling) 500 else 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .size(size)
            .testTag("thermal_dial_gauge"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            val strokeWidth = 14.dp.toPx()
            val trackStroke = 10.dp.toPx()
            val canvasSize = this.size
            val arcSize = Size(canvasSize.width - strokeWidth, canvasSize.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

            // Background Track Arc (260 degrees arc starting at 140 deg)
            drawArc(
                color = Color(0x33334155),
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = trackStroke, cap = StrokeCap.Round)
            )

            // Foreground Active Arc
            val gradientBrush = Brush.sweepGradient(
                0.0f to ThermalOptimal,
                0.4f to ThermalWarm,
                0.7f to ThermalHeated,
                1.0f to ThermalCritical
            )

            if (animatedSweep > 0f) {
                drawArc(
                    brush = gradientBrush,
                    startAngle = 140f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Inner glowing thermal display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Thermostat,
                contentDescription = "Temperature icon",
                tint = animatedDialColor,
                modifier = Modifier.size(28.dp)
            )

            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = String.format("%.1f", animatedTemp),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = Color.White
                )
                Text(
                    text = tempUnit,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = animatedDialColor,
                    modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                )
            }

            Text(
                text = if (isCooling) "COOLING ACTIVE" else when {
                    temperatureC >= 43.0f -> "CRITICAL OVERHEAT"
                    temperatureC >= 40.0f -> "HEATED / THROTTLING"
                    temperatureC >= 37.0f -> "WARM"
                    else -> "SAFE & OPTIMAL"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = animatedDialColor,
                letterSpacing = 1.2.sp
            )
        }
    }
}

@Composable
fun ThermalStatusBadge(
    level: ThermalLevel,
    temperatureC: Float,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon) = when (level) {
        ThermalLevel.OPTIMAL -> Triple(Color(0xFF064E3B), ThermalOptimal, Icons.Default.Info)
        ThermalLevel.WARM -> Triple(Color(0xFF78350F), ThermalWarm, Icons.Default.Info)
        ThermalLevel.HEATED -> Triple(Color(0xFF7C2D12), ThermalHeated, Icons.Default.Warning)
        ThermalLevel.CRITICAL -> Triple(Color(0xFF7F1D1D), ThermalCritical, Icons.Default.Warning)
    }

    Surface(
        color = bgColor.copy(alpha = 0.85f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, textColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Status badge icon",
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = level.label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = textColor
                )
                Text(
                    text = level.description,
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun MetricStatTile(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = FrostDarkSurfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .border(1.dp, Color(0x2238BDF8), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8)
                )
                icon()
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
