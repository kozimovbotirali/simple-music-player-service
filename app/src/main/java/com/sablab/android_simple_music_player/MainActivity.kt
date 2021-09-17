package com.sablab.android_simple_music_player

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.sablab.android_simple_music_player.adapter.MusicsAdapter
import com.sablab.android_simple_music_player.databinding.ActivityMainBinding
import com.sablab.android_simple_music_player.extensions.Constants
import com.sablab.android_simple_music_player.extensions.checkPermissions
import com.sablab.android_simple_music_player.extensions.getPlayList
import com.sablab.android_simple_music_player.extensions.timberLog
import com.sablab.android_simple_music_player.foreground_service.MusicService
import com.sablab.android_simple_music_player.model.Music


class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding: ActivityMainBinding
        get() = _binding ?: throw NullPointerException("View wasn't created")

    private val adapter = MusicsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadViews()
        loadData()
    }

    private fun loadViews() {
        binding.apply {
            list.layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)
            list.adapter = adapter

            adapter.setOnItemClickListener {
                startAudio(it)
            }
        }
    }

    private fun startAudio(data: Music) {
        val intent = Intent(this, MusicService::class.java)
        intent.putExtra(Constants.inputExtra, data)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopService() {
        val serviceIntent = Intent(this, MusicService::class.java)
        stopService(serviceIntent)
    }

    private fun loadData() {
        checkPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            val ls = getPlayList()

            adapter.submitList(ls)
            timberLog(ls.toString())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}