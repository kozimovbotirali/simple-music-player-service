package com.sablab.android_simple_music_player.playback

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.sablab.android_simple_music_player.R
import com.sablab.android_simple_music_player.data.models.Music
import com.sablab.android_simple_music_player.data.models.enums.MusicState
import com.sablab.android_simple_music_player.data.models.enums.ServiceCommand
import com.sablab.android_simple_music_player.data.sources.local.LocalStorage
import com.sablab.android_simple_music_player.util.Constants
import com.sablab.android_simple_music_player.util.Constants.Companion.channelID
import com.sablab.android_simple_music_player.util.Constants.Companion.foregroundServiceNotificationTitle
import com.sablab.android_simple_music_player.util.extensions.getPlayListCursor
import com.sablab.android_simple_music_player.util.extensions.toMusicData
import com.sablab.android_simple_music_player.util.timberErrorLog
import com.sablab.android_simple_music_player.util.timberLog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var _mediaPlayer: MediaPlayer? = null
    private var currentMusic: Music? = null

    private var cursor: Cursor? = null

    private val notificationBuilder by lazy {
        NotificationCompat.Builder(this, channelID)
            .setContentTitle(foregroundServiceNotificationTitle)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_music_logo))
            .setSmallIcon(R.drawable.ic_music_logo)
            .setDefaults(Notification.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(false)
    }

    private fun getNotification(builder: NotificationCompat.Builder? = null): Notification =
        (builder ?: notificationBuilder)
            .setCustomContentView(createView())
            .build()


    @Inject
    lateinit var storage: LocalStorage

    override fun onCreate() {
        super.onCreate()
        startForeground(Constants.notificationId, getNotification())
        timberLog("onCreate")
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun createPendingIntent(serviceCommand: ServiceCommand): PendingIntent {
        val intent = Intent(this, MusicService::class.java)
        intent.putExtra(Constants.COMMAND_DATA, serviceCommand)
        intent.putExtra(Constants.MUSIC_POSITION, storage.lastPlayedPosition)
        return PendingIntent.getService(
            this, serviceCommand.ordinal, intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createView(): RemoteViews {
        val remote = RemoteViews(packageName, R.layout.notification_view)
        cursor?.apply {
            if (moveToPosition(storage.lastPlayedPosition)) {
                val data = toMusicData()

                remote.setTextViewText(R.id.text_name, data.title)
                remote.setTextViewText(R.id.text_author_name, data.artist)
                if (data.imageUri == null) {
                    remote.setImageViewResource(R.id.image, R.drawable.ic_music)
                } else {
                    data.imageUri.let { remote.setImageViewUri(R.id.image, it) }
                }
                when {
                    _mediaPlayer?.isPlaying == true -> {
                        remote.setImageViewResource(R.id.btn_play_pause, R.drawable.ic_pause)
                    }
                    _mediaPlayer?.isPlaying != true -> {
                        remote.setImageViewResource(R.id.btn_play_pause, R.drawable.ic_play)
                    }
                }

                remote.setOnClickPendingIntent(R.id.btn_prev, createPendingIntent(ServiceCommand.PREV))
                remote.setOnClickPendingIntent(
                    R.id.btn_play_pause,
                    createPendingIntent(ServiceCommand.PLAY_PAUSE)
                )
                remote.setOnClickPendingIntent(R.id.btn_next, createPendingIntent(ServiceCommand.NEXT))
                remote.setOnClickPendingIntent(R.id.btn_stop, createPendingIntent(ServiceCommand.STOP))
            }
        }
        return remote
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val command =
            intent?.extras?.getSerializable(Constants.COMMAND_DATA) as? ServiceCommand

        if (cursor == null) {
            getPlayListCursor()
                .onEach { cursor ->
                    this.cursor = cursor
                    doCommand(command)
                }
                .catch { timberErrorLog("Error on getPlayList: $this") }
                .launchIn(serviceScope)
        } else {
            doCommand(command)
        }
        return START_NOT_STICKY
    }

    private fun doCommand(serviceCommand: ServiceCommand?) {
        when (serviceCommand) {
            ServiceCommand.PLAY_NEW -> {
                prepareMediaPlayer()
                _mediaPlayer?.start()

                notifyNotification()

                EventBus.musicStateLiveData.postValue(MusicState.PLAYING(storage.lastPlayedPosition, currentMusic))
                storage.isPlaying = true
            }
            ServiceCommand.PLAY_PAUSE -> {
                if (_mediaPlayer?.isPlaying == true) {
                    _mediaPlayer?.pause()
                    notifyNotification()

                    EventBus.musicStateLiveData.postValue(MusicState.PAUSE(storage.lastPlayedPosition, currentMusic))
                    storage.isPlaying = false
                } else {
                    if (_mediaPlayer == null) {
                        prepareMediaPlayer()
                    }
                    _mediaPlayer?.start()
                    notifyNotification()

                    EventBus.musicStateLiveData.postValue(MusicState.PLAYING(storage.lastPlayedPosition, currentMusic))
                    storage.isPlaying = true
                }
            }
            ServiceCommand.STOP -> {
                _mediaPlayer?.pause()
                stopSelf()

                EventBus.musicStateLiveData.postValue(MusicState.STOP(storage.lastPlayedPosition, currentMusic))
                storage.isPlaying = false
            }
            ServiceCommand.PREV -> {
                prevMusic()
            }
            ServiceCommand.NEXT -> {
                nextMusic()
            }
            else -> {

            }
        }
    }

    private fun prevMusic() {
        cursor?.let { c ->
            if (c.moveToPrevious()) {
                storage.lastPlayedPosition = c.position
                currentMusic = c.toMusicData()
            } else {
                if (c.moveToLast()) {
                    storage.lastPlayedPosition = c.position
                    currentMusic = c.toMusicData()
                }
            }

            try {
                _mediaPlayer?.apply {
                    stop()
                    prepare()
                }

                _mediaPlayer = MediaPlayer.create(this, Uri.fromFile(File(currentMusic?.data ?: ""))).apply {
                    setOnCompletionListener {
                        timberErrorLog("onComplete")
                        nextMusic()
                    }
                    start()
                }
                notifyNotification()

                EventBus.musicStateLiveData.postValue(MusicState.NEXT_OR_PREV(storage.lastPlayedPosition, currentMusic))
                storage.isPlaying = true
            } catch (e: Exception) {
                timberErrorLog(e.message.toString())
            }
        }
    }

    private fun nextMusic() {
        cursor?.let { c ->
            if (c.moveToNext()) {
                storage.lastPlayedPosition = c.position
                currentMusic = c.toMusicData()
            } else {
                if (c.moveToFirst()) {
                    storage.lastPlayedPosition = c.position
                    currentMusic = c.toMusicData()
                }
            }

            try {
                _mediaPlayer?.apply {
                    stop()
                    prepare()
                }

                _mediaPlayer = MediaPlayer.create(this, Uri.fromFile(File(currentMusic?.data ?: ""))).apply {
                    setOnCompletionListener {
                        timberErrorLog("onComplete")
                        nextMusic()
                    }
                    start()
                }
                notifyNotification()

                EventBus.musicStateLiveData.postValue(MusicState.NEXT_OR_PREV(storage.lastPlayedPosition, currentMusic))
                storage.isPlaying = true
            } catch (e: Exception) {
                timberErrorLog(e.message.toString())
            }
        }
    }

    private fun notifyNotification() {
        val mNotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        mNotificationManager?.notify(Constants.notificationId, getNotification())
    }

    private fun prepareMediaPlayer() {
        timberLog(cursor.toString())
        cursor?.let { c ->
            val b = storage.lastPlayedPosition.let { c.moveToPosition(it) }
            timberLog(b.toString())
            if (b) {
                currentMusic = c.toMusicData()

                _mediaPlayer?.apply {
                    stop()
                    prepare()
                }

                _mediaPlayer = MediaPlayer.create(this, Uri.fromFile(File(currentMusic?.data ?: ""))).apply {
                    setOnCompletionListener {
                        timberErrorLog("onComplete")
                        nextMusic()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timberLog("onDestroy")
        serviceScope.cancel()
        storage.isPlaying = false
    }

    override fun onBind(p0: Intent?): IBinder? = null
}