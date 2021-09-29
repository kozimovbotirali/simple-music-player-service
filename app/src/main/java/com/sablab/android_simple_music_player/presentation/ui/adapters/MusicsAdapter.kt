package com.sablab.android_simple_music_player.presentation.ui.adapters

import android.database.Cursor
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.sablab.android_simple_music_player.R
import com.sablab.android_simple_music_player.data.sources.local.LocalStorage
import com.sablab.android_simple_music_player.databinding.ItemMusicBinding
import com.sablab.android_simple_music_player.util.custom.CursorAdapter
import com.sablab.android_simple_music_player.util.extensions.loadImage
import com.sablab.android_simple_music_player.util.extensions.toMusicData

class MusicsAdapter(val storage: LocalStorage) : CursorAdapter<MusicsAdapter.MusicViewHolder>() {
    private var itemClickListener: OnItemClick? = null
    var lastSelected = 0

    inner class MusicViewHolder(private val binding: ItemMusicBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.apply {
            }
        }

        fun bind(position: Int) {
            binding.apply {
                val data = cursor.toMusicData()

                textName.text = data.title
                textAuthorName.text = data.artist

                root.setOnClickListener { itemClickListener?.onClick(position) }
                if (data.imageUri == null) {
                    image.loadImage(R.drawable.ic_music, 15)
                } else {
                    image.loadImage(data.imageUri!!, 15)
                }

                if (storage.lastPlayedPosition == cursor.position) {
                    lastSelected = cursor.position
                    textName.setTextColor(ContextCompat.getColor(binding.root.context, R.color.current_text_color))
                    textAuthorName.setTextColor(ContextCompat.getColor(binding.root.context, R.color.current_text_info_color))
                } else {
                    textName.setTextColor(ContextCompat.getColor(binding.root.context, R.color.text_color))
                    textAuthorName.setTextColor(ContextCompat.getColor(binding.root.context, R.color.text_info_color))
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
        fun onClick(itemPosition: Int)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, cursor: Cursor, position: Int) {
        holder.bind(position)
    }
}