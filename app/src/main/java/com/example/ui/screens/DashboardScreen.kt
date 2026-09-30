package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ThermalLevel
import com.example.ui.components.MetricStatTile
import com.example.ui.components.ThermalDial
import com.example.ui.components.ThermalStatusBadge
import com.example.ui.theme.FrostDarkSurface
import com.example.ui.theme.FrostDarkSurfaceVariant
import com.example.ui.theme.FrostPrimary
import com.example.ui.theme.FrostSecondary
import com.example.ui.theme.FrostTertiary
import com.example.ui.theme.ThermalCritical
import com.example.ui.theme.ThermalHeated
import com.example.ui.theme.ThermalOptimal
import com.example.ui.theme.ThermalWarm
import com.example.ui.viewmodel.ThermalUiState

@Composable
fun DashboardScreen(
    uiState: ThermalUiState,
    onCoolDownClick: () -> Unit,
    onToggleStressSimulation: (Boolean) -> Unit,
    onNavigateToProcesses: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snapshot = uiState.snapshot
    val settings = uiState.settings
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .testTag("dashboard_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App header with hero banner
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = FrostDarkSurfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(20.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_cooling_hero),
                    contentDescription = "Cooling Hero Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, FrostDarkSurface.copy(alpha = 0.92f))
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "FrostGuard Thermal Engine",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (settings.autoShieldServiceEnabled) "Active Real-Time Shield" else "On-Demand Cooling",
                            fontSize = 12.sp,
                            color = FrostPrimary
                        )
                    }
                    Surface(
                        color = Color(0x44000000),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "OS: ${snapshot.osThermalStatus}",
                            fontSize = 11.sp,
                            color = Color(0xFFE2E8F0),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Warning banner if charging while heated
        AnimatedVisibility(
            visible = snapshot.isCharging && snapshot.temperatureC >= 39.0f,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                color = Color(0xFF7C2D12),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .border(1.dp, ThermalHeated, RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryAlert,
                        contentDescription = "Charging heat alert",
                        tint = ThermalHeated,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Charging Heat Danger!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Fast charging is intensifying battery thermistors. Unplugging charger is strongly advised.",
                            fontSize = 11.sp,
                            color = Color(0xFFFED7AA),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Live Thermometer Dial Gauge
        ThermalDial(
            temperatureC = snapshot.temperatureC,
            useFahrenheit = settings.useFahrenheit,
            isCooling = uiState.isCoolingInProgress,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Thermal Status Info Badge
        ThermalStatusBadge(
            level = snapshot.thermalLevel,
            temperatureC = snapshot.temperatureC
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Primary Action: Cool Down Phone CTA
        Button(
            onClick = onCoolDownClick,
            enabled = !uiState.isCoolingInProgress,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (snapshot.temperatureC >= 40f) ThermalCritical else FrostPrimary,
                contentColor = Color(0xFF001F29)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("cool_down_phone_button")
        ) {
            Icon(
                imageVector = Icons.Default.AcUnit,
                contentDescription = "Cool phone icon",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (uiState.isCoolingInProgress) "COOLING IN PROGRESS…" else "COOL DOWN PHONE NOW",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Quick Telemetry Metric Tiles (2x2 Grid)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricStatTile(
                title = "RAM Usage",
                value = "${snapshot.usedRamPercent}%",
                subtitle = "${snapshot.usedRamBytes / (1024 * 1024 * 1024)}GB / ${snapshot.totalRamBytes / (1024 * 1024 * 1024)}GB",
                accentColor = if (snapshot.usedRamPercent > 75) ThermalHeated else FrostSecondary,
                modifier = Modifier.weight(1f),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = "Memory icon",
                        tint = FrostSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            MetricStatTile(
                title = "Battery & Power",
                value = "${snapshot.batteryPercent}%",
                subtitle = "${snapshot.voltageMv} mV • ${if (snapshot.isCharging) snapshot.chargingSource else "Discharging"}",
                accentColor = if (snapshot.isCharging) FrostTertiary else Color.White,
                modifier = Modifier.weight(1f),
                icon = {
                    Icon(
                        imageVector = if (snapshot.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.Bolt,
                        contentDescription = "Battery icon",
                        tint = FrostTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricStatTile(
                title = "Background Tasks",
                value = "${uiState.processList.filter { !it.isProtected }.size} Active",
                subtitle = "Tap to manage & freeze",
                accentColor = FrostPrimary,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToProcesses() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Processes icon",
                        tint = FrostPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            MetricStatTile(
                title = "Battery Health",
                value = snapshot.batteryHealth,
                subtitle = "Safe Threshold: ${String.format("%.0f°C", settings.alertThresholdC)}",
                accentColor = if (snapshot.batteryHealth.contains("Overheat")) ThermalCritical else ThermalOptimal,
                modifier = Modifier.weight(1f),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Health icon",
                        tint = ThermalOptimal,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Heat Stress Simulator Card (Allows developer/user/reviewer to test overheating alerts)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2D)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x33475569), RoundedCornerShape(16.dp))
                .testTag("thermal_simulation_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF7F1D1D)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Fire stress icon",
                            tint = ThermalCritical,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Test Overheat Alert",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Simulate 44.5°C thermal spike",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Switch(
                    checked = uiState.isStressSimulation,
                    onCheckedChange = { onToggleStressSimulation(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ThermalCritical,
                        checkedTrackColor = Color(0xFF7F1D1D)
                    ),
                    modifier = Modifier.testTag("stress_simulation_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
