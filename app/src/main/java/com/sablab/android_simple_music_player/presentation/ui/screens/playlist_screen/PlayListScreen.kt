package com.sablab.android_simple_music_player.presentation.ui.screens.playlist_screen

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.SharedElementCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.sablab.android_simple_music_player.util.dpToPx
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        exitTransition = TransitionInflater.from(requireContext())
            .inflateTransition(R.transition.grid_exit_transition)

        // A similar mapping is set at the ImagePagerFragment with a setEnterSharedElementCallback.
        setExitSharedElementCallback(
            object : SharedElementCallback() {
                override fun onMapSharedElements(names: List<String>, sharedElements: MutableMap<String, View>) {
                    try {
                        val image = view?.findViewById<View>(R.id.image_bottom)
                        // Map the first shared element name to the child ImageView.
                        image?.let { sharedElements[names[0]] = image }
                    } catch (e: Exception) {
                        timberErrorLog(e.message.toString())
                    }
                }
            })

        if (adapter.itemCount != 0) {
            postponeEnterTransition()
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        loadViews()
        loadData()
        loadObservers()
    }

    @SuppressLint("FragmentLiveDataObserve")
    private fun loadObservers() {
        EventBus.musicStateLiveData.observe(this) {
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
            textNameBottom.text = it.title
            textAuthorNameBottom.text = it.artist
            if (it.imageUri != null) {
                imageBottom.loadImage(it.imageUri) {
                    startPostponedEnterTransition()
                }
            } else {
                imageBottom.loadImage(R.drawable.ic_music)
                startPostponedEnterTransition()
            }
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
                    if (it == storage.lastPlayedPosition) {
                        startMusicService(serviceCommand = ServiceCommand.PLAY_PAUSE)
                    } else {
                        storage.lastPlayedPosition = it
                        storage.lastPlayedDuration = 0
                        startMusicService(serviceCommand = ServiceCommand.PLAY_NEW)
                    }
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
            textNameBottom.isSelected = true
            textAuthorNameBottom.isSelected = true

            if (storage.isPlaying) {
                btnPlayPause.setImageResource(R.drawable.ic_pause)
            } else {
                btnPlayPause.setImageResource(R.drawable.ic_play)
            }

            layoutBottom.setOnClickListener {
                try {
                    val extras = FragmentNavigatorExtras(
                        imageBottom to (Constants.FOR_IMAGE_LIST + storage.lastPlayedPosition)
                    )
                    findNavController().navigate(PlayListScreenDirections.actionPlayListScreenToSongIngoScreen(), extras)
                } catch (e: Exception) {
                    timberErrorLog(e.message.toString())
                }
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
                .catch { timberErrorLog(this.toString()) }
                .onEach {
                    adapter.swapCursor(it)

                    if (EventBus.musicStateLiveData.value != null) {
                        EventBus.musicStateLiveData.value?.let { musicState ->
                            musicState.data?.let { it2 -> loadPlayingData(it2) }
                            binding.list.scrollToPosition(musicState.position)
                        }
                    } else {
                        storage.lastPlayedPosition.let { pos ->
                            if (it.moveToPosition(pos)) {
                                val data = it.toMusicData()
                                binding.list.scrollToPosition(pos)
                                loadPlayingData(data)
                                // TODO: 24.09.2021 next after start
                                startMusicService(serviceCommand = ServiceCommand.INIT)
                            }
                        }
                    }
                }.launchIn(lifecycleScope)
        }
    }

    override fun onDestroyView() {
        view?.findViewById<RecyclerView>(R.id.list)?.adapter = null
        view?.findViewById<RecyclerView>(R.id.list)?.removeItemDecoration(itemDecoration)
        super.onDestroyView()
    }
}