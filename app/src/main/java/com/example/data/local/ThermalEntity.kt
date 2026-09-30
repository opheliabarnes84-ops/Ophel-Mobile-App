package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "thermal_logs")
data class ThermalLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val temperatureC: Float,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val thermalStatus: String,
    val ramUsagePercent: Int
)

@Entity(tableName = "cooling_sessions")
data class CoolingSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val tempBeforeC: Float,
    val tempAfterC: Float,
    val processesTerminatedCount: Int,
    val ramFreedMb: Long,
    val durationSeconds: Int,
    val mode: String = "Deep Chill"
)
