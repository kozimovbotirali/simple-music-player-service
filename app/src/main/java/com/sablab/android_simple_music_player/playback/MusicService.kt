package com.sablab.android_simple_music_player.playback

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.sablab.android_simple_music_player.R
import com.sablab.android_simple_music_player.data.models.Music
import com.sablab.android_simple_music_player.data.models.enums.ServiceCommand
import com.sablab.android_simple_music_player.data.sources.local.LocalStorage
import com.sablab.android_simple_music_player.util.Constants
import com.sablab.android_simple_music_player.util.Constants.Companion.channelID
import com.sablab.android_simple_music_player.util.Constants.Companion.foregroundServiceNotificationTitle
import com.sablab.android_simple_music_player.util.extensions.getAudioInfo
import com.sablab.android_simple_music_player.util.timberErrorLog
import com.sablab.android_simple_music_player.util.timberLog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var _mediaPlayer: MediaPlayer? = null
    private var notificationBuilder: NotificationCompat.Builder? = null
    private var currentMusic: Music? = null

    @Inject
    lateinit var storage: LocalStorage

    private val clickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val command = intent.extras?.getSerializable(Constants.COMMAND_DATA) as? ServiceCommand
            timberErrorLog(command?.name.toString())
            doNotificationCommand(command)
        }
    }

    private fun doNotificationCommand(command: ServiceCommand?) {
        when (command) {
            ServiceCommand.PREV -> {
                try {
                    val pos = Constants.allMusics.indexOf(currentMusic?.data)

                    var t = pos - 1
                    if (t <= 0)
                        t = 0

                    if (t < Constants.allMusics.size) {
                        Constants.allMusics.toList()[t].let { it1 ->
                            currentMusic = getAudioInfo(it1)
                        }
                    }
                    currentMusic?.let {
                        storage.lastPlayedData = it.data ?: ""
                    }
                } catch (e: Exception) {
                    timberErrorLog(e.message.toString())
                }
            }
            ServiceCommand.NEXT -> {
                try {
                    val pos = Constants.allMusics.indexOf(currentMusic?.data)

                    val t = pos + 1
                    if (t < Constants.allMusics.size) {
                        Constants.allMusics.toList()[t].let { it1 ->
                            currentMusic = getAudioInfo(it1)
                        }
                    }
                    currentMusic?.let {
                        storage.lastPlayedData = it.data ?: ""
                    }
                } catch (e: Exception) {
                    timberErrorLog(e.message.toString())
                }
            }
            else -> {
            }
        }
        //play
        if (storage.isPlaying || _mediaPlayer == null) {
            prepareMediaPlayer(currentMusic)
        }
        _mediaPlayer?.start()

        storage.isPlaying = true
        startForeground(currentMusic)
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(clickReceiver, IntentFilter(Constants.NOTIFICATION_ACTION_PLAYER))
        timberLog("onCreate")
    }

    private fun startForeground(data: Music?) {
        notificationBuilder = NotificationCompat.Builder(this, channelID)
            .setContentTitle(foregroundServiceNotificationTitle)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_music_logo))
            .setSmallIcon(R.drawable.ic_music_logo)
            .setDefaults(Notification.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCustomContentView(createView(data))
            .setAutoCancel(false)

        startForeground(1, notificationBuilder?.build())
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createPendingIntent(serviceCommand: ServiceCommand): PendingIntent {
        val intent = Intent(this, MusicService::class.java)
        intent.putExtra(Constants.COMMAND_DATA, serviceCommand)
        intent.putExtra(Constants.MUSIC_DATA, currentMusic)
        return PendingIntent.getService(
            this, serviceCommand.ordinal, intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createView(data: Music?): RemoteViews {
        val remote = RemoteViews(packageName, R.layout.notification_view)
        remote.setTextViewText(R.id.text_name, data?.title)
        remote.setTextViewText(R.id.text_author_name, data?.artist)
        if (data?.imageUri == null) {
            remote.setImageViewResource(R.id.image, R.drawable.ic_music)
        } else {
            data.imageUri.let { remote.setImageViewUri(R.id.image, it) }
        }
        when {
            storage.isPlaying -> {
                remote.setImageViewResource(R.id.btn_play_pause, R.drawable.ic_pause)
            }
            !storage.isPlaying -> {
                remote.setImageViewResource(R.id.btn_play_pause, R.drawable.ic_play)
            }
        }

        remote.setOnClickPendingIntent(R.id.btn_prev, createPendingIntent(ServiceCommand.PREV))
        remote.setOnClickPendingIntent(
            R.id.btn_play_pause,
            createPendingIntent(ServiceCommand.PLAY_PAUSE_NOTIFICATION)
        )
        remote.setOnClickPendingIntent(R.id.btn_next, createPendingIntent(ServiceCommand.NEXT))
        return remote
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val data = intent?.getParcelableExtra<Music>(Constants.MUSIC_DATA)
        currentMusic = data

        val command =
            intent?.extras?.getSerializable(Constants.COMMAND_DATA) as? ServiceCommand

        doCommand(command, data)
        return START_NOT_STICKY
    }

    private fun doCommand(serviceCommand: ServiceCommand?, data: Music?) {
        timberLog("serviceCommand=$serviceCommand")
        timberLog("data=$data")
        when (serviceCommand) {
            ServiceCommand.PAUSE -> {
                if (storage.isPlaying) {
                    _mediaPlayer?.pause()

                    storage.isPlaying = false
                    stopForeground(true)
                }
            }
            ServiceCommand.PLAY -> {
                if (storage.isPlaying || _mediaPlayer == null) {
                    prepareMediaPlayer(data)
                }
                _mediaPlayer?.start()

                storage.isPlaying = true
                startForeground(data)
            }
            ServiceCommand.PLAY_NEW -> {
                prepareMediaPlayer(data)
                _mediaPlayer?.start()

                storage.isPlaying = true
                startForeground(data)
            }
            ServiceCommand.PLAY_PAUSE_NOTIFICATION -> {
                if (storage.isPlaying) {
                    _mediaPlayer?.pause()
                    storage.isPlaying = false

                    val intent = Intent(Constants.ACTION_PLAYER)
                    intent.putExtra(Constants.COMMAND_DATA, ServiceCommand.PAUSE)
                    sendBroadcast(intent)
                } else {
                    _mediaPlayer?.start()
                    storage.isPlaying = true

                    val intent = Intent(Constants.ACTION_PLAYER)
                    intent.putExtra(Constants.COMMAND_DATA, ServiceCommand.PLAY)
                    sendBroadcast(intent)
                }
                startForeground(data)
            }
            ServiceCommand.STOP -> {
                if (storage.isPlaying) {
                    _mediaPlayer?.stop()
                    _mediaPlayer?.prepare()

                    storage.isPlaying = false
                    stopForeground(true)
                }
            }
            else -> {
                val intent = Intent(Constants.NOTIFICATION_ACTION_PLAYER)
                intent.putExtra(Constants.COMMAND_DATA, serviceCommand)
                sendBroadcast(intent)
            }
        }
    }

    private fun prepareMediaPlayer(data: Music?) {
        data?.data?.let {
            timberErrorLog(it)
            _mediaPlayer?.stop()
            _mediaPlayer?.prepare()
            _mediaPlayer = MediaPlayer.create(this, Uri.fromFile(File(it)))
            _mediaPlayer?.setOnCompletionListener {
                val intent = Intent(Constants.ACTION_PLAYER)
                intent.putExtra(Constants.COMMAND_DATA, ServiceCommand.NEXT)
                sendBroadcast(intent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timberLog("onDestroy")
        serviceScope.cancel()
        storage.isPlaying = false
        unregisterReceiver(clickReceiver)
//        unregisterReceiver(clickReceiver)
    }

    override fun onBind(p0: Intent?): IBinder? = null
}