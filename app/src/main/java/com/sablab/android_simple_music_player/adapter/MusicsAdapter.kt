package com.sablab.android_simple_music_player.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sablab.android_simple_music_player.databinding.ItemMusicBinding
import com.sablab.android_simple_music_player.model.Music

class MusicsAdapter : ListAdapter<Music, MusicsAdapter.MusicViewHolder>(MusicDiffCallback) {
    private var itemClickListener: OnItemClick? = null

    object MusicDiffCallback : DiffUtil.ItemCallback<Music>() {
        override fun areItemsTheSame(oldItem: Music, newItem: Music): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Music, newItem: Music): Boolean {
            return oldItem == newItem
        }

    }

    inner class MusicViewHolder(private val binding: ItemMusicBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.apply {
                root.setOnClickListener {
                    itemClickListener?.onClick(getItem(absoluteAdapterPosition))
                }
            }
        }

        fun bind(data: Music) {
            binding.apply {
                textName.text = data.displayName
                textAuthorName.text = data.artist
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

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it)
        }
    }

    fun setOnItemClickListener(block: OnItemClick) {
        this.itemClickListener = block
    }

    fun interface OnItemClick {
        fun onClick(item: Music)
    }
}