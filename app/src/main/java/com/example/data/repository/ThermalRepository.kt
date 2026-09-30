package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.CoolingSessionEntity
import com.example.data.local.ThermalDao
import com.example.data.local.ThermalLogEntity
import com.example.data.model.ThermalSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ThermalSettings(
    val alertThresholdC: Float = 40.0f,
    val useFahrenheit: Boolean = false,
    val soundAlertsEnabled: Boolean = true,
    val vibrationAlertsEnabled: Boolean = true,
    val autoShieldServiceEnabled: Boolean = false,
    val chargingHeatGuardEnabled: Boolean = true,
    val autoCoolWhenCritical: Boolean = false
)

class ThermalRepository(
    private val thermalDao: ThermalDao,
    context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("frostguard_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<ThermalSettings> = _settings.asStateFlow()

    val recentLogs: Flow<List<ThermalLogEntity>> = thermalDao.getRecentLogs(40)
    val coolingSessions: Flow<List<CoolingSessionEntity>> = thermalDao.getAllCoolingSessions()
    val maxTemperature: Flow<Float?> = thermalDao.getMaxRecordedTemperature()
    val avgTemperature: Flow<Float?> = thermalDao.getAvgRecordedTemperature()
    val totalRamFreed: Flow<Long?> = thermalDao.getTotalRamFreedMb()

    private fun loadSettings(): ThermalSettings {
        return ThermalSettings(
            alertThresholdC = prefs.getFloat("alert_threshold_c", 40.0f),
            useFahrenheit = prefs.getBoolean("use_fahrenheit", false),
            soundAlertsEnabled = prefs.getBoolean("sound_alerts", true),
            vibrationAlertsEnabled = prefs.getBoolean("vibration_alerts", true),
            autoShieldServiceEnabled = prefs.getBoolean("auto_shield_service", false),
            chargingHeatGuardEnabled = prefs.getBoolean("charging_heat_guard", true),
            autoCoolWhenCritical = prefs.getBoolean("auto_cool_critical", false)
        )
    }

    fun updateSettings(newSettings: ThermalSettings) {
        prefs.edit()
            .putFloat("alert_threshold_c", newSettings.alertThresholdC)
            .putBoolean("use_fahrenheit", newSettings.useFahrenheit)
            .putBoolean("sound_alerts", newSettings.soundAlertsEnabled)
            .putBoolean("vibration_alerts", newSettings.vibrationAlertsEnabled)
            .putBoolean("auto_shield_service", newSettings.autoShieldServiceEnabled)
            .putBoolean("charging_heat_guard", newSettings.chargingHeatGuardEnabled)
            .putBoolean("auto_cool_critical", newSettings.autoCoolWhenCritical)
            .apply()
        _settings.value = newSettings
    }

    suspend fun recordThermalSnapshot(snapshot: ThermalSnapshot) {
        val entity = ThermalLogEntity(
            timestamp = snapshot.timestamp,
            temperatureC = snapshot.temperatureC,
            batteryLevel = snapshot.batteryPercent,
            isCharging = snapshot.isCharging,
            thermalStatus = snapshot.thermalLevel.name,
            ramUsagePercent = snapshot.usedRamPercent
        )
        thermalDao.insertLog(entity)
    }

    suspend fun recordCoolingSession(
        tempBeforeC: Float,
        tempAfterC: Float,
        processesCount: Int,
        ramFreedMb: Long,
        durationSeconds: Int,
        mode: String = "Deep Chill"
    ) {
        val session = CoolingSessionEntity(
            timestamp = System.currentTimeMillis(),
            tempBeforeC = tempBeforeC,
            tempAfterC = tempAfterC,
            processesTerminatedCount = processesCount,
            ramFreedMb = ramFreedMb,
            durationSeconds = durationSeconds,
            mode = mode
        )
        thermalDao.insertCoolingSession(session)
    }

    suspend fun clearHistory() {
        thermalDao.clearThermalLogs()
        thermalDao.clearCoolingSessions()
    }
}
