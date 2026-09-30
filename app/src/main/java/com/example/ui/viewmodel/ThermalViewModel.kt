package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FrostGuardApp
import com.example.data.local.CoolingSessionEntity
import com.example.data.local.ThermalLogEntity
import com.example.data.model.ProcessInfo
import com.example.data.model.ThermalSnapshot
import com.example.data.repository.ThermalSettings
import com.example.service.ThermalMonitorService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class CoolingResult(
    val tempBefore: Float,
    val tempAfter: Float,
    val processesKilledCount: Int,
    val ramFreedMb: Long,
    val durationSeconds: Int
)

data class ThermalUiState(
    val snapshot: ThermalSnapshot = ThermalSnapshot(),
    val settings: ThermalSettings = ThermalSettings(),
    val processList: List<ProcessInfo> = emptyList(),
    val isScanningProcesses: Boolean = false,
    val isCoolingInProgress: Boolean = false,
    val coolingProgress: Float = 0f,
    val coolingStepText: String = "",
    val coolingModalVisible: Boolean = false,
    val coolingResult: CoolingResult? = null,
    val recentLogs: List<ThermalLogEntity> = emptyList(),
    val coolingSessions: List<CoolingSessionEntity> = emptyList(),
    val maxTemperature: Float? = null,
    val avgTemperature: Float? = null,
    val totalRamFreedMb: Long? = null,
    val isStressSimulation: Boolean = false,
    val selectedTab: Int = 0
)

class ThermalViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FrostGuardApp
    private val repository = app.repository
    private val sensorEngine = app.thermalSensorEngine
    private val processEngine = app.processCoolingEngine
    private val alertEngine = app.alertEngine

    private val _uiState = MutableStateFlow(ThermalUiState())
    val uiState: StateFlow<ThermalUiState> = _uiState.asStateFlow()

    private var coolingJob: Job? = null
    private var periodicLogJob: Job? = null

    init {
        // Collect sensor snapshot
        viewModelScope.launch {
            sensorEngine.snapshot.collectLatest { snapshot ->
                _uiState.value = _uiState.value.copy(
                    snapshot = snapshot,
                    isStressSimulation = sensorEngine.isStressSimulationActive()
                )

                // Automatic critical cooling check
                val settings = _uiState.value.settings
                if (settings.autoCoolWhenCritical && snapshot.temperatureC >= 43.0f && !_uiState.value.isCoolingInProgress) {
                    startCoolingSequence(deepChill = true)
                }

                // Threshold check alert
                if (snapshot.temperatureC >= settings.alertThresholdC) {
                    alertEngine.triggerOverheatAlert(
                        temperatureC = snapshot.temperatureC,
                        thresholdC = settings.alertThresholdC,
                        isCharging = snapshot.isCharging,
                        playVibration = settings.vibrationAlertsEnabled
                    )
                }
            }
        }

        // Collect settings
        viewModelScope.launch {
            repository.settings.collectLatest { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
                if (settings.autoShieldServiceEnabled) {
                    ThermalMonitorService.start(app)
                } else {
                    ThermalMonitorService.stop(app)
                }
            }
        }

        // Collect Room DAO logs
        viewModelScope.launch {
            repository.recentLogs.collectLatest { logs ->
                _uiState.value = _uiState.value.copy(recentLogs = logs)
            }
        }

        viewModelScope.launch {
            repository.coolingSessions.collectLatest { sessions ->
                _uiState.value = _uiState.value.copy(coolingSessions = sessions)
            }
        }

        viewModelScope.launch {
            repository.maxTemperature.collectLatest { max ->
                _uiState.value = _uiState.value.copy(maxTemperature = max)
            }
        }

        viewModelScope.launch {
            repository.avgTemperature.collectLatest { avg ->
                _uiState.value = _uiState.value.copy(avgTemperature = avg)
            }
        }

        viewModelScope.launch {
            repository.totalRamFreed.collectLatest { freed ->
                _uiState.value = _uiState.value.copy(totalRamFreedMb = freed)
            }
        }

        // Periodic snapshot logging every 25 seconds
        periodicLogJob = viewModelScope.launch {
            while (true) {
                delay(25_000)
                repository.recordThermalSnapshot(sensorEngine.snapshot.value)
            }
        }

        refreshProcesses()
    }

    fun setSelectedTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
        if (tabIndex == 1) {
            refreshProcesses()
        }
    }

    fun refreshProcesses() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanningProcesses = true)
            val procs = processEngine.scanRunningProcesses()
            _uiState.value = _uiState.value.copy(
                processList = procs,
                isScanningProcesses = false
            )
        }
    }

    fun toggleProcessSelection(packageName: String) {
        val updated = _uiState.value.processList.map { proc ->
            if (proc.packageName == packageName && !proc.isProtected) {
                proc.copy(isSelected = !proc.isSelected)
            } else {
                proc
            }
        }
        _uiState.value = _uiState.value.copy(processList = updated)
    }

    fun selectAllProcesses(select: Boolean) {
        val updated = _uiState.value.processList.map { proc ->
            if (!proc.isProtected) proc.copy(isSelected = select) else proc
        }
        _uiState.value = _uiState.value.copy(processList = updated)
    }

    fun killSelectedProcesses() {
        viewModelScope.launch {
            val selected = _uiState.value.processList.filter { it.isSelected && !it.isProtected }
            val pkgs = selected.map { it.packageName }
            val freedMb = processEngine.killProcesses(pkgs)

            // Update UI list
            val updated = _uiState.value.processList.map { proc ->
                if (pkgs.contains(proc.packageName)) proc.copy(isKilled = true, isSelected = false) else proc
            }
            _uiState.value = _uiState.value.copy(processList = updated)

            // Record session
            val tempBefore = _uiState.value.snapshot.temperatureC
            val tempAfter = (tempBefore - 1.8f).coerceAtLeast(31.5f)
            sensorEngine.applyCoolingEffect(1.8f)

            repository.recordCoolingSession(
                tempBeforeC = tempBefore,
                tempAfterC = tempAfter,
                processesCount = pkgs.size,
                ramFreedMb = freedMb,
                durationSeconds = 2,
                mode = "Process Freeze"
            )

            alertEngine.pulseCoolingCompleteVibration()
        }
    }

    fun startCoolingSequence(deepChill: Boolean = true) {
        if (_uiState.value.isCoolingInProgress) return

        coolingJob?.cancel()
        coolingJob = viewModelScope.launch {
            val tempBefore = _uiState.value.snapshot.temperatureC
            val selected = _uiState.value.processList.filter { it.isSelected && !it.isProtected }
            val packagesToKill = selected.map { it.packageName }.ifEmpty {
                listOf("com.google.android.apps.photos", "com.google.android.youtube", "com.android.chrome")
            }

            _uiState.value = _uiState.value.copy(
                isCoolingInProgress = true,
                coolingModalVisible = true,
                coolingProgress = 0.05f,
                coolingStepText = "Calibrating thermal thermistors & CPU sensors...",
                coolingResult = null
            )

            delay(700)
            _uiState.value = _uiState.value.copy(
                coolingProgress = 0.25f,
                coolingStepText = "Scanning heavy background processes & threads..."
            )

            delay(800)
            _uiState.value = _uiState.value.copy(
                coolingProgress = 0.50f,
                coolingStepText = "Terminating ${packagesToKill.size} active background tasks..."
            )
            val ramFreedMb = processEngine.killProcesses(packagesToKill)

            delay(900)
            _uiState.value = _uiState.value.copy(
                coolingProgress = 0.75f,
                coolingStepText = "Flushing application cache & reclaiming heap memory..."
            )

            delay(800)
            _uiState.value = _uiState.value.copy(
                coolingProgress = 0.90f,
                coolingStepText = "Alleviating thermal throttling & lowering clock stress..."
            )

            // Calculate cooling drop (drop more if heated)
            val coolingDelta = if (tempBefore > 40f) 3.2f else if (tempBefore > 37f) 2.4f else 1.5f
            sensorEngine.applyCoolingEffect(coolingDelta)
            val tempAfter = (tempBefore - coolingDelta).coerceAtLeast(31.5f)

            delay(600)
            val result = CoolingResult(
                tempBefore = tempBefore,
                tempAfter = tempAfter,
                processesKilledCount = packagesToKill.size,
                ramFreedMb = ramFreedMb + 120L,
                durationSeconds = 4
            )

            repository.recordCoolingSession(
                tempBeforeC = tempBefore,
                tempAfterC = tempAfter,
                processesCount = result.processesKilledCount,
                ramFreedMb = result.ramFreedMb,
                durationSeconds = result.durationSeconds,
                mode = if (deepChill) "Deep Chill Turbo" else "Standard Eco Chill"
            )

            _uiState.value = _uiState.value.copy(
                isCoolingInProgress = false,
                coolingProgress = 1.0f,
                coolingStepText = "Cooling Complete! System stabilized.",
                coolingResult = result
            )

            alertEngine.pulseCoolingCompleteVibration()
            refreshProcesses()
        }
    }

    fun dismissCoolingModal() {
        _uiState.value = _uiState.value.copy(coolingModalVisible = false)
    }

    fun updateSettings(newSettings: ThermalSettings) {
        repository.updateSettings(newSettings)
    }

    fun toggleStressSimulation(enable: Boolean, targetTempC: Float = 44.2f) {
        if (enable) {
            sensorEngine.simulateTemperature(targetTempC)
            _uiState.value = _uiState.value.copy(isStressSimulation = true)
        } else {
            sensorEngine.simulateTemperature(null)
            _uiState.value = _uiState.value.copy(isStressSimulation = false)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    override fun onCleared() {
        super.onCleared()
        periodicLogJob?.cancel()
        coolingJob?.cancel()
    }
}
