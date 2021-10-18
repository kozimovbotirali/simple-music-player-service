package com.sablab.android_simple_music_player.playback

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.sablab.android_simple_music_player.R
import com.sablab.android_simple_music_player.data.models.Music
import com.sablab.android_simple_music_player.data.models.enums.MusicState
import com.sablab.android_simple_music_player.data.models.enums.ServiceCommand
import com.sablab.android_simple_music_player.data.sources.local.LocalStorage
import com.sablab.android_simple_music_player.databinding.OverlayButtonBinding
import com.sablab.android_simple_music_player.databinding.OverlayCloseBinding
import com.sablab.android_simple_music_player.util.Constants
import com.sablab.android_simple_music_player.util.Constants.Companion.channelID
import com.sablab.android_simple_music_player.util.Constants.Companion.foregroundServiceNotificationTitle
import com.sablab.android_simple_music_player.util.extensions.getPlayListCursor
import com.sablab.android_simple_music_player.util.extensions.gone
import com.sablab.android_simple_music_player.util.extensions.toMusicData
import com.sablab.android_simple_music_player.util.extensions.visible
import com.sablab.android_simple_music_player.util.timberErrorLog
import com.sablab.android_simple_music_player.util.timberLog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : LifecycleService()/*, AudioManager.OnAudioFocusChangeListener*/ {

    companion object {
        private val layoutFlag = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        private const val windowFlag = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        private const val windowFormat = PixelFormat.TRANSLUCENT
        private const val MAX_CLICK_DURATION = 200
        private val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            windowFlag,
            windowFormat
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }

        private val layoutParamsClose = WindowManager.LayoutParams(
            300,
            300,
            layoutFlag,
            windowFlag,
            windowFormat
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER
            y = 100
        }
    }

    private var _viewBinding: OverlayButtonBinding? = null
    private val viewBinding: OverlayButtonBinding get() = _viewBinding!!

    private var _viewBindingClose: OverlayCloseBinding? = null
    private val viewBindingClose: OverlayCloseBinding get() = _viewBindingClose!!

    private var _windowManager: WindowManager? = null
    private val windowManager: WindowManager get() = _windowManager!!

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var startClickTime = 0L
    private var clickDuration = 0L
    private val width by lazy { windowManager.defaultDisplay.width }
    private val height by lazy { windowManager.defaultDisplay.height }

    @SuppressLint("ClickableViewAccessibility")
    private fun createOverlayButton() {
        _viewBinding = OverlayButtonBinding.inflate(LayoutInflater.from(this), null, false)

        viewBinding.btnPlayPause.setOnTouchListener { _, event ->
            clickDuration = Calendar.getInstance().timeInMillis - startClickTime

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startClickTime = Calendar.getInstance().timeInMillis

                    initialX = layoutParams.x
                    initialY = layoutParams.y

                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                }
                MotionEvent.ACTION_UP -> {
                    viewBindingClose.btnClose.gone()
                    layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()

                    if (clickDuration < MAX_CLICK_DURATION) {
                        playPauseOverlay()
                    } else {
                        if (isRemoveVisible()) {
                            _mediaPlayer?.pause()
                            durationJob?.cancel()
                            stopSelf()

                            EventBus.musicStateLiveData.postValue(MusicState.STOP(storage.lastPlayedPosition, currentMusic))
                            storage.isPlaying = false
                        }
                        moveTo()
                    }
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(viewBinding.root, layoutParams)

                    if (isRemoveVisible()) {
                        viewBindingClose.btnClose.setImageResource(R.drawable.ic_baseline_delete_black)
                    } else {
                        viewBindingClose.btnClose.setImageResource(R.drawable.ic_baseline_delete_24)
                    }

                    if (clickDuration >= MAX_CLICK_DURATION) {
                        viewBindingClose.btnClose.visible()
                    }
                }
            }
            return@setOnTouchListener false
        }

        //close Button

        _viewBindingClose = OverlayCloseBinding.inflate(LayoutInflater.from(this), null, false)
        viewBindingClose.btnClose.gone()

        windowManager.addView(viewBindingClose.root, layoutParamsClose)
        windowManager.addView(viewBinding.root, layoutParams)
    }

    private fun isRemoveVisible() = layoutParams.y > (height * 0.7) && layoutParams.x > (width * 0.3) && layoutParams.x < (width - width * 0.3)

    private fun removeOverlayButton() {
        _viewBinding?.let {
            _windowManager?.removeView(it.root)
        }
        _viewBindingClose?.let {
            _windowManager?.removeView(it.root)
        }
        _viewBinding = null
        _viewBindingClose = null
    }

    private fun moveTo() {
        val xPos = if ((layoutParams.x + viewBinding.btnPlayPause.width / 2) >= width / 2) {
            width
        } else 0

        layoutParams.x = xPos
        windowManager.updateViewLayout(viewBinding.root, layoutParams)
    }

    private fun playPauseOverlay() {
        if (_mediaPlayer?.isPlaying == true) {
            viewBinding.btnPlayPause.setImageResource(R.drawable.ic_play)
            _mediaPlayer?.pause()
            durationJob?.cancel()
            notifyNotification()
            stopForeground(false)

            EventBus.musicStateLiveData.postValue(MusicState.PAUSE(storage.lastPlayedPosition, currentMusic))
            storage.isPlaying = false
        } else {
            viewBinding.btnPlayPause.setImageResource(R.drawable.ic_pause)
            if (_mediaPlayer == null) {
                prepareMediaPlayer()
            }
            _mediaPlayer?.start()
            startSendDuration()
            startForeground(Constants.notificationId, getNotification())

            EventBus.musicStateLiveData.postValue(MusicState.PLAYING(storage.lastPlayedPosition, currentMusic))
            storage.isPlaying = true
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var durationJob: Job? = null
    private var currentDuration: Int = 0

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
        _windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(Constants.notificationId, getNotification())
        EventBus.progressChangeLiveData.observe(this) {
            _mediaPlayer?.seekTo(it)
            currentDuration = it
        }
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
                        _viewBinding?.btnPlayPause?.setImageResource(R.drawable.ic_pause)
                    }
                    _mediaPlayer?.isPlaying != true -> {
                        remote.setImageViewResource(R.id.btn_play_pause, R.drawable.ic_play)
                        _viewBinding?.btnPlayPause?.setImageResource(R.drawable.ic_play)
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
        super.onStartCommand(intent, flags, startId)
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
//        if (checkAudioFocusState())
        when (serviceCommand) {
            ServiceCommand.CREATE_OVERLAY -> {
                createOverlayButton()
            }
            ServiceCommand.REMOVE_OVERLAY -> {
                removeOverlayButton()
            }
            ServiceCommand.PLAY_NEW -> {
                prepareMediaPlayer()
                _mediaPlayer?.start()
                startSendDuration()

                startForeground(Constants.notificationId, getNotification())

                EventBus.musicStateLiveData.postValue(MusicState.PLAYING(storage.lastPlayedPosition, currentMusic))
                storage.isPlaying = true
            }
            ServiceCommand.PLAY_PAUSE -> {
                if (_mediaPlayer?.isPlaying == true) {
                    _mediaPlayer?.pause()
                    durationJob?.cancel()
                    notifyNotification()
                    stopForeground(false)

                    EventBus.musicStateLiveData.postValue(MusicState.PAUSE(storage.lastPlayedPosition, currentMusic))
                    storage.isPlaying = false
                } else {
                    if (_mediaPlayer == null) {
                        prepareMediaPlayer()
                    }
                    _mediaPlayer?.start()
                    startSendDuration()
                    startForeground(Constants.notificationId, getNotification())

                    EventBus.musicStateLiveData.postValue(MusicState.PLAYING(storage.lastPlayedPosition, currentMusic))
                    storage.isPlaying = true
                }
            }
            ServiceCommand.STOP -> {
                _mediaPlayer?.pause()
                durationJob?.cancel()
                stopSelf()

                EventBus.musicStateLiveData.postValue(MusicState.STOP(storage.lastPlayedPosition, currentMusic))
                storage.isPlaying = false
            }
            ServiceCommand.INIT -> {
                if (_mediaPlayer == null) {
                    prepareMediaPlayer()
                }
                notifyNotification()
                stopForeground(true)
//                stopSelf()

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
                    storage.lastPlayedDuration = 0
                    currentDuration = 0

                    setOnCompletionListener {
                        timberErrorLog("onComplete")
                        nextMusic()
                    }
                    start()
                    startSendDuration()
                }
                startForeground(Constants.notificationId, getNotification())

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
                    storage.lastPlayedDuration = 0
                    currentDuration = 0

                    setOnCompletionListener {
                        timberErrorLog("onComplete")
                        nextMusic()
                    }
                    start()
                    startSendDuration()
                }
                startForeground(Constants.notificationId, getNotification())

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
            currentDuration = 0

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
                    if (storage.lastPlayedDuration != 0) {
                        seekTo(storage.lastPlayedDuration)
                        EventBus.currentValueLiveData.postValue(storage.lastPlayedDuration)
                    }
                }
            }
        }
    }

    private fun startSendDuration() {
        durationJob?.cancel()

        durationJob = getMusicChangesFlow().onEach {
            storage.lastPlayedDuration = it
            currentDuration = it
            EventBus.currentValueLiveData.postValue(it)
        }.launchIn(serviceScope)
    }

    private fun getMusicChangesFlow() = flow {
        for (i in currentDuration..(_mediaPlayer?.duration ?: 0)) {
            emit(_mediaPlayer?.currentPosition ?: 0)
            delay(1000)
        }
    }.flowOn(Dispatchers.Default)

    override fun onDestroy() {
        super.onDestroy()
        timberLog("onDestroy")
        serviceScope.cancel()
        storage.isPlaying = false

        _viewBinding?.let {
            _windowManager?.removeView(it.root)
        }
        _viewBindingClose?.let {
            _windowManager?.removeView(it.root)
        }
        _viewBinding = null
        _viewBindingClose = null
        _windowManager = null
    }

    override fun onBind(p0: Intent): IBinder? {
        super.onBind(p0)
        return null
    }
}