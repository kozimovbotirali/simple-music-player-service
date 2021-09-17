package com.sablab.android_simple_music_player.foreground_service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sablab.android_simple_music_player.MainActivity
import com.sablab.android_simple_music_player.R
import com.sablab.android_simple_music_player.extensions.Constants
import com.sablab.android_simple_music_player.extensions.Constants.Companion.channelID
import com.sablab.android_simple_music_player.extensions.Constants.Companion.foregroundServiceNotificationTitle
import com.sablab.android_simple_music_player.model.Music

class MusicService : Service() {
    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val input = intent?.getParcelableExtra<Music>(Constants.inputExtra)

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val notification = NotificationCompat.Builder(this, channelID)
            .setContentTitle(foregroundServiceNotificationTitle)
            .setContentText(input?.displayName)
            .setSmallIcon(R.drawable.ic_music_logo)
            .setDefaults(Notification.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? = null
}