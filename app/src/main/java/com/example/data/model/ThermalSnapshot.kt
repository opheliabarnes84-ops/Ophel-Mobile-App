package com.example.data.model

enum class ThermalLevel(val label: String, val description: String) {
    OPTIMAL("Optimal Cooling", "Hardware operates within safe, efficient temperature limits."),
    WARM("Mildly Warm", "Active processes or charging are generating heat. Monitoring."),
    HEATED("High Temperature", "Throttling risk. Background processes should be managed."),
    CRITICAL("Overheating Danger", "Immediate cooling recommended to prevent thermal throttling & battery degradation.")
}

data class ThermalSnapshot(
    val temperatureC: Float = 34.5f,
    val batteryPercent: Int = 85,
    val voltageMv: Int = 4120,
    val isCharging: Boolean = false,
    val chargingSource: String = "Battery",
    val batteryHealth: String = "Good",
    val osThermalStatus: String = "Normal",
    val totalRamBytes: Long = 6L * 1024 * 1024 * 1024,
    val availableRamBytes: Long = 2800L * 1024 * 1024,
    val backgroundProcessesCount: Int = 12,
    val timestamp: Long = System.currentTimeMillis()
) {
    val temperatureF: Float
        get() = (temperatureC * 9f / 5f) + 32f

    val usedRamBytes: Long
        get() = (totalRamBytes - availableRamBytes).coerceAtLeast(0)

    val usedRamPercent: Int
        get() = if (totalRamBytes > 0) {
            (((totalRamBytes - availableRamBytes).toFloat() / totalRamBytes) * 100).toInt().coerceIn(0, 100)
        } else 50

    val thermalLevel: ThermalLevel
        get() = when {
            temperatureC >= 43.0f -> ThermalLevel.CRITICAL
            temperatureC >= 40.0f -> ThermalLevel.HEATED
            temperatureC >= 37.0f -> ThermalLevel.WARM
            else -> ThermalLevel.OPTIMAL
        }
}
