package com.example.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class ThermalAlertEngine(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ALERT_ID = "frostguard_thermal_critical_channel"
        const val CHANNEL_MONITOR_ID = "frostguard_monitor_channel"
        const val NOTIFICATION_ALERT_ID = 2001
        const val NOTIFICATION_MONITOR_ID = 2002
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alertSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val alertChannel = NotificationChannel(
                CHANNEL_ALERT_ID,
                "Overheating Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alerts when device temperature crosses safe cooling thresholds"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 500)
                setSound(alertSoundUri, audioAttributes)
            }

            val monitorChannel = NotificationChannel(
                CHANNEL_MONITOR_ID,
                "Thermal Shield Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing device temperature and thermal monitor"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(alertChannel)
            notificationManager.createNotificationChannel(monitorChannel)
        }
    }

    fun triggerOverheatAlert(
        temperatureC: Float,
        thresholdC: Float,
        isCharging: Boolean,
        playVibration: Boolean = true
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_COOLING_ACTION", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val chargingWarning = if (isCharging) " (⚠️ Fast Charging Active - Unplug Charger!)" else ""
        val contentText = String.format(
            "Phone reached %.1f°C (limit %.1f°C).%s Background tasks are stressing CPU.",
            temperatureC, thresholdC, chargingWarning
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERT_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("🔥 High Temperature Warning!")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            notificationManager.notify(NOTIFICATION_ALERT_ID, builder.build())
        } catch (_: SecurityException) {
            // Permission might be pending or denied
        }

        if (playVibration) {
            pulseVibrateAlert()
        }
    }

    fun pulseVibrateAlert() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val effect = VibrationEffect.createWaveform(
                    longArrayOf(0, 300, 150, 400),
                    intArrayOf(0, 255, 0, 255),
                    -1
                )
                vibratorManager?.vibrate(CombinedVibration.createParallel(effect))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(
                        longArrayOf(0, 300, 150, 400),
                        -1
                    )
                    vibrator?.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(500)
                }
            }
        } catch (_: Exception) {
            // Ignored if vibrator unavailable
        }
    }

    fun pulseCoolingCompleteVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        } catch (_: Exception) {
            // Ignored
        }
    }
}
