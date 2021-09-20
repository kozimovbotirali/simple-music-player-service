package com.sablab.android_simple_music_player.presentation.ui.screens.playlist_screen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.sablab.android_simple_music_player.R
import com.sablab.android_simple_music_player.data.models.Music
import com.sablab.android_simple_music_player.data.models.enums.ServiceCommand
import com.sablab.android_simple_music_player.data.sources.local.LocalStorage
import com.sablab.android_simple_music_player.databinding.ScreenPlaylistBinding
import com.sablab.android_simple_music_player.playback.MusicService
import com.sablab.android_simple_music_player.presentation.ui.adapters.MusicsAdapter
import com.sablab.android_simple_music_player.util.Constants
import com.sablab.android_simple_music_player.util.checkPermissions
import com.sablab.android_simple_music_player.util.custom.ItemDecorationWithLeftPadding
import com.sablab.android_simple_music_player.util.custom.dpToPx
import com.sablab.android_simple_music_player.util.extensions.getAudioInfo
import com.sablab.android_simple_music_player.util.extensions.getPlayList
import com.sablab.android_simple_music_player.util.extensions.loadImage
import com.sablab.android_simple_music_player.util.timberErrorLog
import com.sablab.android_simple_music_player.util.timberLog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by Botirali Kozimov on 19/09/2021
 */

@AndroidEntryPoint
class PlayListScreen : Fragment(R.layout.screen_playlist) {

    private val binding: ScreenPlaylistBinding by viewBinding(ScreenPlaylistBinding::bind)

    @Inject
    lateinit var storage: LocalStorage

    private var playingData: Music? = null

    private val adapter = MusicsAdapter()
    private val itemDecoration by lazy {
        ItemDecorationWithLeftPadding(requireContext(), 85.dpToPx(requireContext()))
    }

    private val clickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val command = intent.extras?.getSerializable(Constants.COMMAND_DATA) as? ServiceCommand
            timberErrorLog(command?.name.toString())
            binding.apply {
                when (command) {
                    ServiceCommand.PLAY -> {
                        btnPlayPause.setImageResource(R.drawable.ic_pause)
                    }
                    ServiceCommand.PAUSE -> {
                        btnPlayPause.setImageResource(R.drawable.ic_play)
                    }
                    else -> {
                    }
                }
            }
        }
    }

    private val notificationClickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val command = intent.extras?.getSerializable(Constants.COMMAND_DATA) as? ServiceCommand
            timberErrorLog(command?.name.toString())
            binding.apply {
                when (command) {
                    ServiceCommand.PREV -> {
                        prevClicked()
                    }
                    ServiceCommand.NEXT -> {
                        nextClicked()
                    }
                    else -> {
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        loadViews()
        loadData()
    }

    private fun loadViews() {
        requireContext().registerReceiver(clickReceiver, IntentFilter(Constants.ACTION_PLAYER))
        requireContext().registerReceiver(
            notificationClickReceiver,
            IntentFilter(Constants.NOTIFICATION_ACTION_PLAYER)
        )

        binding.apply {
            list.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            list.addItemDecoration(itemDecoration)
            list.adapter = adapter

            adapter.setOnItemClickListener {
                startMusicService(it, ServiceCommand.PLAY_NEW)
                btnPlayPause.setImageResource(R.drawable.ic_pause)

                storage.lastPlayedData = it.data ?: ""
                playingData = it

                loadPlayingData(it)
            }

            btnNext.setOnClickListener {
                val intent = Intent(Constants.NOTIFICATION_ACTION_PLAYER)
                intent.putExtra(Constants.COMMAND_DATA, ServiceCommand.NEXT)
                requireContext().sendBroadcast(intent)
            }

            btnPrev.setOnClickListener {
                val intent = Intent(Constants.NOTIFICATION_ACTION_PLAYER)
                intent.putExtra(Constants.COMMAND_DATA, ServiceCommand.PREV)
                requireContext().sendBroadcast(intent)
            }

            btnPlayPause.setOnClickListener {
                if (storage.isPlaying) {
                    startMusicService(playingData, serviceCommand = ServiceCommand.PAUSE)
                    btnPlayPause.setImageResource(R.drawable.ic_play)
                } else {
                    startMusicService(playingData, serviceCommand = ServiceCommand.PLAY)
                    btnPlayPause.setImageResource(R.drawable.ic_pause)
                }
            }

            /**
             * Its used to make author name and title textview s scrollable(horizontally)
             */
            textName.isSelected = true
            textAuthorName.isSelected = true

            if (storage.isPlaying) {
                btnPlayPause.setImageResource(R.drawable.ic_pause)
            } else {
                btnPlayPause.setImageResource(R.drawable.ic_play)
            }

            if (storage.lastPlayedData.isNotEmpty()) {
                loadLastMusicData()
            }
        }
    }

    private fun prevClicked() {
        binding.apply {
            try {
                val pos = Constants.allMusics.indexOf(playingData?.data)

                var t = pos - 1
                if (t <= 0)
                    t = 0

                if (t < Constants.allMusics.size) {
                    Constants.allMusics.toList()[t].let { it1 ->
                        playingData = requireContext().getAudioInfo(it1)
                    }
                }
//                startMusicService(playingData, serviceCommand = ServiceCommand.PLAY)
                btnPlayPause.setImageResource(R.drawable.ic_pause)
                playingData?.let {
                    storage.lastPlayedData = it.data ?: ""
                    loadPlayingData(it)
                }
                list.scrollToPosition(t)
            } catch (e: Exception) {
                timberErrorLog(e.message.toString())
            }
        }
    }

    private fun nextClicked() {
        binding.apply {
            try {
                val pos = Constants.allMusics.indexOf(playingData?.data)

                val t = pos + 1
                if (t < Constants.allMusics.size) {
                    Constants.allMusics.toList()[t].let { it1 ->
                        playingData = requireContext().getAudioInfo(it1)
                    }
                }
//                startMusicService(playingData, serviceCommand = ServiceCommand.PLAY)
                btnPlayPause.setImageResource(R.drawable.ic_pause)
                playingData?.let {
                    storage.lastPlayedData = it.data ?: ""
                    loadPlayingData(it)
                }
                list.scrollToPosition(t)
            } catch (e: Exception) {
                timberErrorLog(e.message.toString())
            }
        }
    }

    private fun loadLastMusicData() {
        lifecycleScope.launch {
            playingData = requireContext().getAudioInfo(storage.lastPlayedData)
            playingData?.let {
                loadPlayingData(it)
            }
        }
    }

    private fun loadPlayingData(it: Music) {
        binding.apply {
            textName.text = it.title
            textAuthorName.text = it.artist
            it.imageUri.let { it1 -> image.loadImage(it1) }
        }
    }

    private fun startMusicService(data: Music? = null, serviceCommand: ServiceCommand) {
        val intent = Intent(context, MusicService::class.java)
        intent.putExtra(Constants.COMMAND_DATA, serviceCommand)
        intent.putExtra(Constants.MUSIC_DATA, data)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
    }

    private fun loadData() {
        requireActivity().checkPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            requireActivity().getPlayList()
                .onEach { adapter.swapCursor(it) }
                .catch { timberLog(this.toString()) }
                .launchIn(lifecycleScope)
        }
    }

    override fun onDestroyView() {
        binding.list.adapter = null
        binding.list.removeItemDecoration(itemDecoration)
        requireContext().unregisterReceiver(clickReceiver)
        requireContext().unregisterReceiver(notificationClickReceiver)
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!storage.isPlaying) {
            requireContext().stopService(Intent(context, MusicService::class.java))
        }
    }
}