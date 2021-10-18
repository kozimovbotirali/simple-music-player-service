package com.sablab.android_simple_music_player

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.sablab.android_simple_music_player.data.models.enums.ServiceCommand
import com.sablab.android_simple_music_player.data.sources.local.LocalStorage
import com.sablab.android_simple_music_player.playback.MusicService
import com.sablab.android_simple_music_player.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(R.layout.activity_main) {
    @Inject
    lateinit var storage: LocalStorage

    override fun onPause() {
        if (storage.isPlaying && Settings.canDrawOverlays(this)) {
            startMusicService(ServiceCommand.CREATE_OVERLAY)
        }
        super.onPause()
    }

    override fun onResume() {
        if (Settings.canDrawOverlays(this)) {
            startMusicService(ServiceCommand.REMOVE_OVERLAY)
        }
        super.onResume()
    }

    @Suppress("SameParameterValue")
    private fun startMusicService(serviceCommand: ServiceCommand) {
        val intent = Intent(this, MusicService::class.java)
        intent.putExtra(Constants.COMMAND_DATA, serviceCommand)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}