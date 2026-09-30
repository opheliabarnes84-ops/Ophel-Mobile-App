package com.example.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.example.data.model.ThermalSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ThermalSensorEngine(
    private val context: Context,
    private val processCoolingEngine: ProcessCoolingEngine
) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val _snapshot = MutableStateFlow(ThermalSnapshot())
    val snapshot: StateFlow<ThermalSnapshot> = _snapshot.asStateFlow()

    // Simulation / testing mode
    private var simulatedTemperatureC: Float? = null
    private var isSimulatingStress: Boolean = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                updateFromBatteryIntent(intent)
            }
        }
    }

    private var thermalListener: PowerManager.OnThermalStatusChangedListener? = null

    init {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val initialIntent = context.registerReceiver(batteryReceiver, filter)
        if (initialIntent != null) {
            updateFromBatteryIntent(initialIntent)
        }

        // Register system thermal HAL callback on Android 10+ (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
            thermalListener = PowerManager.OnThermalStatusChangedListener { status ->
                val statusString = when (status) {
                    PowerManager.THERMAL_STATUS_NONE -> "Normal"
                    PowerManager.THERMAL_STATUS_LIGHT -> "Light Throttling"
                    PowerManager.THERMAL_STATUS_MODERATE -> "Moderate Throttling"
                    PowerManager.THERMAL_STATUS_SEVERE -> "Severe Throttling"
                    PowerManager.THERMAL_STATUS_CRITICAL -> "Critical Throttling"
                    PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency Shutdown"
                    PowerManager.THERMAL_STATUS_SHUTDOWN -> "Thermal Shutdown"
                    else -> "Normal"
                }
                _snapshot.value = _snapshot.value.copy(osThermalStatus = statusString)
            }
            try {
                powerManager.addThermalStatusListener(context.mainExecutor, thermalListener!!)
            } catch (_: Exception) {
                // Safe fallback
            }
        }
    }

    private fun updateFromBatteryIntent(intent: Intent) {
        val rawTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 320)
        // Convert tenths of degrees Celsius (e.g. 345 -> 34.5°C)
        var actualTempC = (rawTemp / 10.0f)
        if (actualTempC <= 0f || actualTempC > 95f) {
            actualTempC = 34.2f
        }

        val tempToUse = simulatedTemperatureC ?: actualTempC

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = if (level >= 0 && scale > 0) {
            ((level / scale.toFloat()) * 100).toInt()
        } else {
            82
        }

        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4100)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val isCharging = plugged != 0
        val chargingSource = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Fast AC Charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Cable"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Charging Pad"
            else -> "Battery Discharging"
        }

        val healthCode = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD)
        val healthStr = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat Alert"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Good Condition"
        }

        val (totalRam, availRam) = processCoolingEngine.getMemorySnapshot()

        _snapshot.value = _snapshot.value.copy(
            temperatureC = tempToUse,
            batteryPercent = batteryPct,
            voltageMv = voltage,
            isCharging = isCharging,
            chargingSource = chargingSource,
            batteryHealth = healthStr,
            totalRamBytes = totalRam,
            availableRamBytes = availRam,
            timestamp = System.currentTimeMillis()
        )
    }

    fun simulateTemperature(tempC: Float?) {
        simulatedTemperatureC = tempC
        isSimulatingStress = tempC != null && tempC >= 41.0f
        _snapshot.value = _snapshot.value.copy(
            temperatureC = tempC ?: 33.8f,
            timestamp = System.currentTimeMillis()
        )
    }

    fun applyCoolingEffect(degreesCooling: Float) {
        val current = _snapshot.value.temperatureC
        val newTemp = (current - degreesCooling).coerceAtLeast(31.5f)
        simulatedTemperatureC = newTemp
        val (totalRam, availRam) = processCoolingEngine.getMemorySnapshot()
        _snapshot.value = _snapshot.value.copy(
            temperatureC = newTemp,
            availableRamBytes = (availRam + 180L * 1024 * 1024).coerceAtMost(totalRam),
            timestamp = System.currentTimeMillis()
        )
    }

    fun isStressSimulationActive(): Boolean = isSimulatingStress

    fun unregister() {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null && thermalListener != null) {
            try {
                powerManager.removeThermalStatusListener(thermalListener!!)
            } catch (_: Exception) {}
        }
    }
}
