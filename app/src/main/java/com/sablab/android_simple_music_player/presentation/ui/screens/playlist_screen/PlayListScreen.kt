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
import com.sablab.android_simple_music_player.data.models.enums.ServiceCommand
import com.sablab.android_simple_music_player.databinding.ScreenPlaylistBinding
import com.sablab.android_simple_music_player.playback.MusicService
import com.sablab.android_simple_music_player.presentation.ui.adapters.MusicsAdapter
import com.sablab.android_simple_music_player.util.Constants
import com.sablab.android_simple_music_player.util.checkPermissions
import com.sablab.android_simple_music_player.util.custom.ItemDecorationWithLeftPadding
import com.sablab.android_simple_music_player.util.custom.dpToPx
import com.sablab.android_simple_music_player.util.extensions.getPlayList
import com.sablab.android_simple_music_player.util.extensions.loadImage
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Created by Botirali Kozimov on 19/09/2021
 */

class PlayListScreen : Fragment(R.layout.screen_playlist) {

    private val binding: ScreenPlaylistBinding by viewBinding(ScreenPlaylistBinding::bind)

    private val adapter = MusicsAdapter()
    private val itemDecoration by lazy {
        ItemDecorationWithLeftPadding(requireContext(), 85.dpToPx(requireContext()))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        loadViews()
        loadData()
    }

    private fun loadViews() {
        binding.apply {
            list.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            list.addItemDecoration(itemDecoration)
            list.adapter = adapter

            adapter.setOnItemClickListener {
                startMusicService(it, ServiceCommand.PLAY)

                textName.text = it.title
                textAuthorName.text = it.artist
                it.imageUri?.let { it1 -> image.loadImage(it1) }
            }

            btnNext.setOnClickListener { }
            btnPrev.setOnClickListener { }
            btnPlayPause.setOnClickListener {
                if (btnPlayPause.isChecked) {
                    startMusicService(serviceCommand = ServiceCommand.PLAY)
                } else {
                    startMusicService(serviceCommand = ServiceCommand.PAUSE)
                }
            }

            /**
             * Its used to make author name and title textview s scrollable(horizontally)
             */
            textName.isSelected = true
            textAuthorName.isSelected = true
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
//                .catch { timberLog(this.toString()) }
                .launchIn(lifecycleScope)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.list.adapter = null
        binding.list.removeItemDecoration(itemDecoration)
    }
}