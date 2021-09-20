package com.sablab.android_simple_music_player.presentation.ui.adapters

import android.database.Cursor
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sablab.android_simple_music_player.R
import com.sablab.android_simple_music_player.data.models.Music
import com.sablab.android_simple_music_player.databinding.ItemMusicBinding
import com.sablab.android_simple_music_player.util.Constants
import com.sablab.android_simple_music_player.util.custom.CursorAdapter
import com.sablab.android_simple_music_player.util.extensions.*

class MusicsAdapter : CursorAdapter<MusicsAdapter.MusicViewHolder>() {
    private var itemClickListener: OnItemClick? = null

    inner class MusicViewHolder(private val binding: ItemMusicBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.apply {
            }
        }

        fun bind() {
            binding.apply {
                val data = Music(
                    id = cursor.getLong(ID),
                    artist = cursor.getString(ARTIST),
                    title = cursor.getString(TITLE),
                    data = cursor.getString(DATA),
                    displayName = cursor.getString(DISPLAY_NAME),
                    duration = cursor.getLong(DURATION),
                    imageUri = root.context.songArt(cursor.getLong(ALBUM_ID))
                )
                data.data?.let { Constants.allMusics.add(it) }
                textName.text = data.title
                textAuthorName.text = data.artist

                root.setOnClickListener { itemClickListener?.onClick(data) }
                if (data.imageUri == null) {
                    image.setImageResource(R.drawable.ic_music)
                } else {
                    image.loadImage(data.imageUri!!)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        return MusicViewHolder(
            ItemMusicBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    fun setOnItemClickListener(block: OnItemClick) {
        this.itemClickListener = block
    }

    fun interface OnItemClick {
        fun onClick(item: Music)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, cursor: Cursor, position: Int) {
        holder.bind()
    }
}