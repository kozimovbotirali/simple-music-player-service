package com.sablab.android_simple_music_player.presentation.ui.adapters.info_images

import android.database.Cursor
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.sablab.android_simple_music_player.R
import com.sablab.android_simple_music_player.data.sources.local.LocalStorage
import com.sablab.android_simple_music_player.databinding.ItemInfoImageBinding
import com.sablab.android_simple_music_player.util.Constants
import com.sablab.android_simple_music_player.util.custom.CursorAdapter
import com.sablab.android_simple_music_player.util.extensions.ALBUM_ID
import com.sablab.android_simple_music_player.util.extensions.loadImage
import com.sablab.android_simple_music_player.util.extensions.songArt

/**
 * Created by B.Kozimov on 27.09.2021 10:41.
 */
class ImagesAdapter(val storage: LocalStorage, val onLoadingFinished: () -> Unit = {}) : CursorAdapter<ImagesAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(private val binding: ItemInfoImageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.apply {
            }
        }

        fun bind() {
            binding.apply {
                val imgData = binding.root.context.songArt(cursor.getLong(ALBUM_ID))
                if (imgData == null) {
                    image.setImageResource(R.drawable.ic_music)
                    onLoadingFinished.invoke()
                } else {
                    image.loadImage(imgData) {
                        onLoadingFinished.invoke()
                    }
                }
                ViewCompat.setTransitionName(image, Constants.FOR_IMAGE_LIST + cursor.position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        return ImageViewHolder(
            ItemInfoImageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ImageViewHolder, cursor: Cursor, position: Int) {
        holder.bind()
    }
}