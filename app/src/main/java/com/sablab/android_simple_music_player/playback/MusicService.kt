package com.sablab.android_simple_music_player.playback

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.sablab.android_simple_music_player.R
import com.sablab.android_simple_music_player.data.models.Music
import com.sablab.android_simple_music_player.data.models.enums.ServiceCommand
import com.sablab.android_simple_music_player.util.Constants
import com.sablab.android_simple_music_player.util.Constants.Companion.channelID
import com.sablab.android_simple_music_player.util.Constants.Companion.foregroundServiceNotificationTitle
import com.sablab.android_simple_music_player.util.timberLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import timber.log.Timber

class MusicService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var _mediaPlayer: MediaPlayer? = null
    private val mediaPlayer: MediaPlayer get() = _mediaPlayer!!

    override fun onCreate() {
        super.onCreate()
        timberLog("onCreate")
    }

    private fun startForeground(data: Music?) {
        val notification = NotificationCompat.Builder(this, channelID)
            .setContentTitle(foregroundServiceNotificationTitle)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_music_logo))
            .setSmallIcon(R.drawable.ic_music_logo)
            .setDefaults(Notification.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCustomContentView(createView(data))
            .setAutoCancel(false)
            .setSound(null)
            .build()

        startForeground(1, notification)
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createPendingIntent(serviceCommand: ServiceCommand): PendingIntent {
        val intent = Intent(this, MusicService::class.java)
        intent.putExtra(Constants.COMMAND_DATA, serviceCommand)
        return PendingIntent.getService(
            this, serviceCommand.ordinal, intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createView(data: Music?): RemoteViews {
        val remote = RemoteViews(packageName, R.layout.notification_view)
        remote.setTextViewText(R.id.text_name, data?.title)
        remote.setTextViewText(R.id.text_author_name, data?.artist)
        data?.imageUri?.let { remote.setImageViewUri(R.id.image, it) }
        remote.setOnClickPendingIntent(R.id.btn_prev, createPendingIntent(ServiceCommand.PLAY))
        remote.setOnClickPendingIntent(
            R.id.btn_play_pause,
            createPendingIntent(ServiceCommand.PAUSE)
        )
        remote.setOnClickPendingIntent(R.id.btn_next, createPendingIntent(ServiceCommand.STOP))
        return remote
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        timberLog("intent=$intent")
        val data = intent?.getParcelableExtra<Music>(Constants.MUSIC_DATA)
        val command =
            intent?.extras?.getSerializable(Constants.COMMAND_DATA) as? ServiceCommand

        doCommand(command, data)
        return START_NOT_STICKY
    }

    private fun doCommand(serviceCommand: ServiceCommand?, data: Music?) {
        Timber.d("serviceCommand=$serviceCommand")
        when (serviceCommand) {
            ServiceCommand.PAUSE -> {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                }
            }
            ServiceCommand.PLAY -> {
                startForeground(data)
                if (data != null) {
                    prepareMediaPlayer(data)
                }
                mediaPlayer.start()
            }
            ServiceCommand.STOP -> {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                    mediaPlayer.prepare()
                }
            }
        }
    }

    private fun prepareMediaPlayer(data: Music) {
        _mediaPlayer?.stop()
        _mediaPlayer?.prepare()
        _mediaPlayer = MediaPlayer.create(this, Uri.parse(data.data))
    }

    override fun onDestroy() {
        super.onDestroy()
        timberLog("onDestroy")
        serviceScope.cancel()
//        unregisterReceiver(clickReceiver)
    }

    override fun onBind(p0: Intent?): IBinder? = null
}