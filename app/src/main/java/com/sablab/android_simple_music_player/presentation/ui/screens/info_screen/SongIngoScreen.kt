package com.sablab.android_simple_music_player.presentation.ui.screens.info_screen

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.app.SharedElementCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import com.sablab.android_simple_music_player.R
import com.sablab.android_simple_music_player.data.models.Music
import com.sablab.android_simple_music_player.data.models.enums.MusicState
import com.sablab.android_simple_music_player.data.models.enums.ServiceCommand
import com.sablab.android_simple_music_player.data.sources.local.LocalStorage
import com.sablab.android_simple_music_player.databinding.ScreenSongInfoBinding
import com.sablab.android_simple_music_player.playback.EventBus
import com.sablab.android_simple_music_player.playback.MusicService
import com.sablab.android_simple_music_player.presentation.ui.adapters.info_images.ImagesAdapter
import com.sablab.android_simple_music_player.util.Constants
import com.sablab.android_simple_music_player.util.checkPermissions
import com.sablab.android_simple_music_player.util.custom.SlowdownRecyclerView
import com.sablab.android_simple_music_player.util.custom.SnapOnScrollListener
import com.sablab.android_simple_music_player.util.extensions.getPlayListCursor
import com.sablab.android_simple_music_player.util.extensions.toFormattedString
import com.sablab.android_simple_music_player.util.extensions.toMusicData
import com.sablab.android_simple_music_player.util.timberErrorLog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Created by B.Kozimov on 27.09.2021 9:59.
 */
@AndroidEntryPoint
class SongIngoScreen : Fragment(R.layout.screen_song_info) {

    private val binding: ScreenSongInfoBinding by viewBinding(ScreenSongInfoBinding::bind)

    @Inject
    lateinit var storage: LocalStorage

    private val adapter by lazy {
        ImagesAdapter(storage) {
            startPostponedEnterTransition()
        }
    }
    private val snapHelper = LinearSnapHelper()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        sharedElementEnterTransition = TransitionInflater.from(requireContext())
            .inflateTransition(R.transition.shared_image)

        setEnterSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(names: MutableList<String>, sharedElements: MutableMap<String, View>) {
                try {
                    // Locate the ViewHolder for the clicked position.
                    val selectedViewHolder: RecyclerView.ViewHolder = binding.list
                        .findViewHolderForAdapterPosition(storage.lastPlayedPosition) ?: return

                    val image = selectedViewHolder.itemView.findViewById<View>(R.id.image)

                    // Map the first shared element name to the child ImageView.
                    sharedElements[names[0]] = image
                } catch (e: Exception) {
                    timberErrorLog(e.message.toString())
                }
            }
        })
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        postponeEnterTransition()

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
                    }
                    is MusicState.STOP -> {
                        btnPlayPause.setImageResource(R.drawable.ic_play)
                        seekbar.progress = 0
                    }
                    is MusicState.NEXT_OR_PREV -> {
                        btnPlayPause.setImageResource(R.drawable.ic_pause)
                        it.data?.let { it1 -> loadPlayingData(it1) }
                        list.smoothScrollToPosition(it.position)
                    }
                }
            }
        }
        EventBus.currentValueLiveData.observe(viewLifecycleOwner) {
            binding.apply {
                seekbar.progress = it.toInt()
                textCurrentDuration.text = it.toLong().toFormattedString()
            }
        }
    }

    private fun loadViews() {
        binding.apply {
            list.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            list.adapter = adapter
            snapHelper.attachToRecyclerView(list)
            list.addOnScrollListener(snapOnScrollListener)
            /**
             * Its used to make author name and title textview s scrollable(horizontally)
             */
            /*textNameBottom.isSelected = true
            textAuthorNameBottom.isSelected = true*/

            if (storage.isPlaying) {
                btnPlayPause.setImageResource(R.drawable.ic_pause)
            } else {
                btnPlayPause.setImageResource(R.drawable.ic_play)
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

            btnBack.setOnClickListener { findNavController().navigateUp() }

            seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        seekbar.progress = progress
                        textCurrentDuration.text = progress.toLong().toFormattedString()
                        EventBus.progressChangeLiveData.postValue(progress)
                    }
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {

                }

                override fun onStopTrackingTouch(p0: SeekBar?) {

                }

            })
        }
    }

    private fun loadData() {
        requireActivity().checkPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            requireActivity().getPlayListCursor()
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
                                startMusicService(serviceCommand = ServiceCommand.INIT)
                            }
                        }
                    }
                }
                .catch { timberErrorLog(this.toString()) }
                .launchIn(lifecycleScope)
        }
    }

    private val snapOnScrollListener = SnapOnScrollListener(
        snapHelper,
        SnapOnScrollListener.Behavior.NOTIFY_ON_SCROLL_STATE_IDLE,
        object : SnapOnScrollListener.OnSnapPositionChangeListener {
            override fun onSnapPositionChange(position: Int) {
                if (position != storage.lastPlayedPosition) {
                    storage.lastPlayedPosition = position
                    storage.lastPlayedDuration = 0
                    startMusicService(serviceCommand = ServiceCommand.PLAY_NEW)
                }
            }
        })

    private fun loadPlayingData(it: Music) {
        binding.apply {
            textNameBottom.text = it.title
            textAuthorNameBottom.text = it.artist
            it.duration?.let {
                seekbar.max = it.toInt()
                textDuration.text = it.toFormattedString()
                storage.lastPlayedDuration.let { progress ->
                    seekbar.progress = progress
                    textCurrentDuration.text = progress.toLong().toFormattedString()
                }
            }

            /*val bitmap = requireContext().songArt(it.data.toString())
            if (bitmap != null) {
                createPaletteAsync(bitmap)
            } else {
                getLargeIcon(requireContext())?.let { it1 -> createPaletteAsync(it1) }
            }*/
        }
    }

    /*private fun createPaletteAsync(bitmap: Bitmap) {
        Palette.from(bitmap).generate { palette ->
//            val color = palette?.getDominantColor(ContextCompat.getColor(requireContext(), R.color.black))
//            color?.let { binding.root.setBackgroundColor(it) }
            val vibrantSwatch = palette?.vibrantSwatch

            binding.toolbar.setBackgroundColor(
                vibrantSwatch?.rgb ?: ContextCompat.getColor(requireContext(), R.color.black)
            )
            binding.title.setTextColor(
                vibrantSwatch?.titleTextColor ?: ContextCompat.getColor(requireContext(), R.color.white)
            )
        }
    }*/

    private fun startMusicService(serviceCommand: ServiceCommand) {
        val intent = Intent(context, MusicService::class.java)
        intent.putExtra(Constants.COMMAND_DATA, serviceCommand)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
    }

    override fun onDestroyView() {
        view?.findViewById<SlowdownRecyclerView>(R.id.list)?.adapter = null
        view?.findViewById<SlowdownRecyclerView>(R.id.list)?.removeOnScrollListener(snapOnScrollListener)
        super.onDestroyView()
    }
}