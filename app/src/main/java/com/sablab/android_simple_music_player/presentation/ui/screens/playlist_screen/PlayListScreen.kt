package com.sablab.android_simple_music_player.presentation.ui.screens.playlist_screen

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.sablab.android_simple_music_player.R
import com.sablab.android_simple_music_player.data.models.Music
import com.sablab.android_simple_music_player.data.models.enums.MusicState
import com.sablab.android_simple_music_player.data.models.enums.ServiceCommand
import com.sablab.android_simple_music_player.data.sources.local.LocalStorage
import com.sablab.android_simple_music_player.databinding.ScreenPlaylistBinding
import com.sablab.android_simple_music_player.playback.EventBus
import com.sablab.android_simple_music_player.playback.MusicService
import com.sablab.android_simple_music_player.presentation.ui.adapters.MusicsAdapter
import com.sablab.android_simple_music_player.util.Constants
import com.sablab.android_simple_music_player.util.checkPermissions
import com.sablab.android_simple_music_player.util.custom.ItemDecorationWithLeftPadding
import com.sablab.android_simple_music_player.util.custom.dpToPx
import com.sablab.android_simple_music_player.util.extensions.getPlayListCursor
import com.sablab.android_simple_music_player.util.extensions.loadImage
import com.sablab.android_simple_music_player.util.extensions.toMusicData
import com.sablab.android_simple_music_player.util.timberErrorLog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Created by Botirali Kozimov on 19/09/2021
 */

@AndroidEntryPoint
class PlayListScreen : Fragment(R.layout.screen_playlist) {

    private val binding: ScreenPlaylistBinding by viewBinding(ScreenPlaylistBinding::bind)

    @Inject
    lateinit var storage: LocalStorage

    private val adapter by lazy { MusicsAdapter(storage) }
    private val itemDecoration by lazy {
        ItemDecorationWithLeftPadding(requireContext(), 85.dpToPx(requireContext()))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        loadViews()
        loadData()
        loadObservers()
    }

    private fun loadObservers() {
        EventBus.musicStateLiveData.observe(viewLifecycleOwner) {
            binding.apply {
                when (it) {
                    is MusicState.PAUSE -> {
                        btnPlayPause.setImageResource(R.drawable.ic_play)
                    }
                    is MusicState.PLAYING -> {
                        btnPlayPause.setImageResource(R.drawable.ic_pause)
                        it.data?.let { it1 -> loadPlayingData(it1) }

                        adapter.notifyItemChanged(it.position)
                        adapter.notifyItemChanged(adapter.lastSelected)
                    }
                    is MusicState.STOP -> {
                        btnPlayPause.setImageResource(R.drawable.ic_play)
                    }
                    is MusicState.NEXT_OR_PREV -> {
                        btnPlayPause.setImageResource(R.drawable.ic_pause)
                        it.data?.let { it1 -> loadPlayingData(it1) }
                        list.scrollToPosition(it.position)

                        adapter.notifyItemChanged(it.position)
                        adapter.notifyItemChanged(adapter.lastSelected)
                    }
                }
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

    private fun loadViews() {

        binding.apply {
            list.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            list.addItemDecoration(itemDecoration)
            list.adapter = adapter

            adapter.setOnItemClickListener {
                requireActivity().checkPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    storage.lastPlayedPosition = it
                    startMusicService(serviceCommand = ServiceCommand.PLAY_NEW)
                }
            }

            btnNext.setOnClickListener {
                requireActivity().checkPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    startMusicService(serviceCommand = ServiceCommand.NEXT)
                }
            }

            btnPrev.setOnClickListener {
                requireActivity().checkPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    startMusicService(serviceCommand = ServiceCommand.PREV)
                }
            }

            btnPlayPause.setOnClickListener {
                requireActivity().checkPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    startMusicService(serviceCommand = ServiceCommand.PLAY_PAUSE)
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
        }
    }

    private fun startMusicService(serviceCommand: ServiceCommand) {
        val intent = Intent(context, MusicService::class.java)
        intent.putExtra(Constants.COMMAND_DATA, serviceCommand)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
    }

    private fun loadData() {
        requireActivity().checkPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            requireActivity().getPlayListCursor()
                .onEach {
                    adapter.swapCursor(it)

                    if (storage.isPlaying && EventBus.musicStateLiveData.value != null) {
                        EventBus.musicStateLiveData.value?.data?.let { it1 -> loadPlayingData(it1) }
                    } else {
                        storage.lastPlayedPosition.let { pos ->
                            binding.list.scrollToPosition(pos)
                            if (it.moveToPosition(pos)) {
                                loadPlayingData(it.toMusicData())
                                // TODO: 24.09.2021 next after start
                                startMusicService(serviceCommand = ServiceCommand.STOP)
                            }
                        }
                    }
                }
                .catch { timberErrorLog(this.toString()) }
                .launchIn(lifecycleScope)
        }
    }

    override fun onDestroyView() {
        binding.list.adapter = null
        binding.list.removeItemDecoration(itemDecoration)
        super.onDestroyView()
    }
}