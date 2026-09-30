package com.example

import android.app.Application
import com.example.data.local.ThermalDatabase
import com.example.data.repository.ThermalRepository
import com.example.engine.ProcessCoolingEngine
import com.example.engine.ThermalAlertEngine
import com.example.engine.ThermalSensorEngine

class FrostGuardApp : Application() {

    lateinit var database: ThermalDatabase
        private set

    lateinit var repository: ThermalRepository
        private set

    lateinit var alertEngine: ThermalAlertEngine
        private set

    lateinit var processCoolingEngine: ProcessCoolingEngine
        private set

    lateinit var thermalSensorEngine: ThermalSensorEngine
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = ThermalDatabase.getDatabase(this)
        repository = ThermalRepository(database.thermalDao(), this)
        alertEngine = ThermalAlertEngine(this)
        processCoolingEngine = ProcessCoolingEngine(this)
        thermalSensorEngine = ThermalSensorEngine(this, processCoolingEngine)
    }

    companion object {
        lateinit var instance: FrostGuardApp
            private set
    }
}
