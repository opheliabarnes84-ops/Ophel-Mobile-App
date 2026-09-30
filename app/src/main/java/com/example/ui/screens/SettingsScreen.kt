package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.ThermalSettings
import com.example.ui.theme.FrostDarkSurfaceVariant
import com.example.ui.theme.FrostPrimary
import com.example.ui.theme.FrostSecondary
import com.example.ui.theme.FrostTertiary
import com.example.ui.theme.ThermalCritical
import com.example.ui.theme.ThermalHeated
import com.example.ui.theme.ThermalOptimal

@Composable
fun SettingsScreen(
    settings: ThermalSettings,
    onUpdateSettings: (ThermalSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val useF = settings.useFahrenheit
    val unit = if (useF) "°F" else "°C"
    val thresholdDisplay = if (useF) (settings.alertThresholdC * 9/5 + 32) else settings.alertThresholdC

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp)
            .testTag("settings_screen")
    ) {
        Text(
            text = "Thermal Guard Settings",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(top = 10.dp, bottom = 14.dp)
        )

        // Threshold Slider Card
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DeviceThermostat,
                            contentDescription = null,
                            tint = ThermalHeated,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Overheat Alert Limit",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = String.format("%.1f%s", thresholdDisplay, unit),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThermalHeated
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "FrostGuard alerts you with high-priority audio & haptics when temperature crosses this threshold.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Slider(
                    value = settings.alertThresholdC,
                    onValueChange = { newC ->
                        onUpdateSettings(settings.copy(alertThresholdC = newC))
                    },
                    valueRange = 36.0f..48.0f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = ThermalHeated,
                        activeTrackColor = ThermalHeated,
                        inactiveTrackColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.testTag("thermal_threshold_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("36°C (Cautious)", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text("40°C (Recommended)", fontSize = 11.sp, color = FrostPrimary)
                    Text("48°C (Extreme)", fontSize = 11.sp, color = Color(0xFF64748B))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Shield & Alert switches
        Card(
            colors = CardDefaults.cardColors(containerColor = FrostDarkSurfaceVariant),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x2238BDF8), RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Protection & Alerts",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = FrostSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                SettingToggleRow(
                    icon = Icons.Default.Security,
                    iconTint = FrostPrimary,
                    title = "Background Thermal Shield",
                    description = "Runs background service with live temperature in status bar",
                    checked = settings.autoShieldServiceEnabled,
                    onCheckedChange = { onUpdateSettings(settings.copy(autoShieldServiceEnabled = it)) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                SettingToggleRow(
                    icon = Icons.Default.DeviceThermostat,
                    iconTint = FrostSecondary,
                    title = "Fahrenheit Unit (°F)",
                    description = "Display values in Fahrenheit instead of Celsius",
                    checked = settings.useFahrenheit,
                    onCheckedChange = { onUpdateSettings(settings.copy(useFahrenheit = it)) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                SettingToggleRow(
                    icon = Icons.Default.VolumeUp,
                    iconTint = FrostTertiary,
                    title = "Audio Alarm on Overheat",
                    description = "Plays alert ringtone when temperature exceeds limit",
                    checked = settings.soundAlertsEnabled,
                    onCheckedChange = { onUpdateSettings(settings.copy(soundAlertsEnabled = it)) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                SettingToggleRow(
                    icon = Icons.Default.Vibration,
                    iconTint = ThermalHeated,
                    title = "Vibration Alert Pattern",
                    description = "Pulses device haptics during high temperature",
                    checked = settings.vibrationAlertsEnabled,
                    onCheckedChange = { onUpdateSettings(settings.copy(vibrationAlertsEnabled = it)) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                SettingToggleRow(
                    icon = Icons.Default.BatteryChargingFull,
                    iconTint = ThermalCritical,
                    title = "Charging Heat Guard",
                    description = "Warns immediately if phone is fast charging while warm",
                    checked = settings.chargingHeatGuardEnabled,
                    onCheckedChange = { onUpdateSettings(settings.copy(chargingHeatGuardEnabled = it)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Hardware Cooling Best Practices Guide
        Card(
            colors = CardDefaults.cardColors(containerColor = FrostDarkSurfaceVariant),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x2238BDF8), RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = FrostPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hardware Cooling Best Practices",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                CoolingTipItem(
                    number = "1",
                    title = "Remove Thick Phone Case",
                    description = "Rubber & leather cases trap dissipating heat against the battery."
                )

                CoolingTipItem(
                    number = "2",
                    title = "Avoid Direct Sunlight & Cars",
                    description = "Solar radiation on car mounts can heat batteries beyond 50°C rapidly."
                )

                CoolingTipItem(
                    number = "3",
                    title = "Pause Fast Charging",
                    description = "High amperage power supplies deliver extreme thermal spikes. Let phone cool first."
                )

                CoolingTipItem(
                    number = "4",
                    title = "Close Camera & 3D Games",
                    description = "Image processing pipelines and GPU rendering generate massive internal wattage."
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun SettingToggleRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = FrostPrimary,
                checkedTrackColor = Color(0xFF0C4A6E),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF1E293B)
            )
        )
    }
}

@Composable
fun CoolingTipItem(
    number: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$number.",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = FrostPrimary,
            modifier = Modifier.width(20.dp)
        )
        Column {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                lineHeight = 15.sp
            )
        }
    }
}
