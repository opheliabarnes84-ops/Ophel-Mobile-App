package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ThermalDao {

    @Query("SELECT * FROM thermal_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 40): Flow<List<ThermalLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ThermalLogEntity)

    @Query("SELECT * FROM cooling_sessions ORDER BY timestamp DESC")
    fun getAllCoolingSessions(): Flow<List<CoolingSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoolingSession(session: CoolingSessionEntity)

    @Query("SELECT MAX(temperatureC) FROM thermal_logs")
    fun getMaxRecordedTemperature(): Flow<Float?>

    @Query("SELECT AVG(temperatureC) FROM thermal_logs")
    fun getAvgRecordedTemperature(): Flow<Float?>

    @Query("SELECT SUM(ramFreedMb) FROM cooling_sessions")
    fun getTotalRamFreedMb(): Flow<Long?>

    @Query("DELETE FROM thermal_logs")
    suspend fun clearThermalLogs()

    @Query("DELETE FROM cooling_sessions")
    suspend fun clearCoolingSessions()
}
