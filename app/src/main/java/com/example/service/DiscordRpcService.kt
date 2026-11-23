package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.WatchRpcApp
import com.example.model.formatDuration

class DiscordRpcService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val manager = (application as WatchRpcApp).rpcManager
        when (intent?.action) {
            ACTION_TOGGLE_PLAY_PAUSE -> {
                manager.togglePlayPause()
                updateNotification()
            }

            ACTION_STOP -> {
                manager.disconnect()
                stopSelf()
            }

            ACTION_REFRESH -> {
                updateNotification()
            }

            else -> {
                val notification = buildNotification()
                val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    0
                }
                ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
            }
        }
        return START_STICKY
    }

    private fun updateNotification() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val manager = (application as WatchRpcApp).rpcManager
        val config = manager.config.value

        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val toggleIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, DiscordRpcService::class.java).apply {
                action = ACTION_TOGGLE_PLAY_PAUSE
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, DiscordRpcService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = getString(
            R.string.notification_watching_prefix,
            config.mediaTitle.ifBlank { getString(R.string.app_name) },
        )

        val subtitle = buildString {
            val statusWord = if (config.isPlaying) "Playing" else "Paused"
            append(statusWord)
            if (config.episodeTag.isNotBlank()) {
                append(" • ")
                append(config.episodeTag)
            }
            if (config.showsProgressBar) {
                append(" • ")
                append(formatDuration(config.elapsedSeconds, forceHours = config.totalSeconds >= 3600L))
                append(" / ")
                append(formatDuration(config.totalSeconds, forceHours = config.totalSeconds >= 3600L))
            } else {
                append(" • ")
                append(formatDuration(config.elapsedSeconds, forceHours = false))
            }
        }

        val toggleLabel = if (config.isPlaying) {
            getString(R.string.action_pause)
        } else {
            getString(R.string.action_resume)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent)
            .addAction(0, toggleLabel, toggleIntent)
            .addAction(0, getString(R.string.action_stop), stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "watch_rpc_live_channel"
        const val NOTIFICATION_ID = 4107
        const val ACTION_START = "com.example.watchrpc.ACTION_START"
        const val ACTION_REFRESH = "com.example.watchrpc.ACTION_REFRESH"
        const val ACTION_TOGGLE_PLAY_PAUSE = "com.example.watchrpc.ACTION_TOGGLE_PLAY_PAUSE"
        const val ACTION_STOP = "com.example.watchrpc.ACTION_STOP"
    }
}
