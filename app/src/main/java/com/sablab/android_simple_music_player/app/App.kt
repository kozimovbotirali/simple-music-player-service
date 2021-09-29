package com.sablab.android_simple_music_player.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.sablab.android_simple_music_player.BuildConfig
import com.sablab.android_simple_music_player.data.sources.local.LocalStorage
import com.sablab.android_simple_music_player.util.Constants.Companion.channelID
import com.sablab.android_simple_music_player.util.Constants.Companion.notificationChannelName
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        instance = this

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        LocalStorage.init(this)

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelID,
                notificationChannelName,
                NotificationManager.IMPORTANCE_LOW
            )

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        lateinit var instance: App
    }
}