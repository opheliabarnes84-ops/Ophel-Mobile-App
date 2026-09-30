package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.FrostGuardApp
import com.example.MainActivity
import com.example.engine.ThermalAlertEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ThermalMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var monitorJob: Job? = null
    private var lastAlertTimeMs: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createMonitorNotification("34.5°C", "Optimal")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                ThermalAlertEngine.NOTIFICATION_MONITOR_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(ThermalAlertEngine.NOTIFICATION_MONITOR_ID, notification)
        }

        startMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        monitorJob?.cancel()
        val app = applicationContext as? FrostGuardApp ?: return

        monitorJob = serviceScope.launch {
            app.thermalSensorEngine.snapshot.collectLatest { snapshot ->
                val settings = app.repository.settings.value
                val tempStr = String.format("%.1f°C", snapshot.temperatureC)
                val statusStr = snapshot.thermalLevel.label

                // Update sticky notification
                val updatedNotif = createMonitorNotification(tempStr, statusStr)
                val notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notifManager.notify(ThermalAlertEngine.NOTIFICATION_MONITOR_ID, updatedNotif)

                // Check overheat threshold
                val now = System.currentTimeMillis()
                if (snapshot.temperatureC >= settings.alertThresholdC && (now - lastAlertTimeMs > 60_000)) {
                    lastAlertTimeMs = now
                    app.alertEngine.triggerOverheatAlert(
                        temperatureC = snapshot.temperatureC,
                        thresholdC = settings.alertThresholdC,
                        isCharging = snapshot.isCharging,
                        playVibration = settings.vibrationAlertsEnabled
                    )
                }

                // Periodic history log (every 30 seconds if changed)
                app.repository.recordThermalSnapshot(snapshot)
            }
        }
    }

    private fun createMonitorNotification(tempStr: String, statusStr: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, ThermalAlertEngine.CHANNEL_MONITOR_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("FrostGuard Thermal Shield")
            .setContentText("Current: $tempStr | Status: $statusStr")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        monitorJob?.cancel()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, ThermalMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ThermalMonitorService::class.java)
            context.stopService(intent)
        }
    }
}
