package com.sablab.android_simple_music_player.playback

import androidx.lifecycle.MutableLiveData
import com.sablab.android_simple_music_player.data.models.enums.MusicState

object EventBus {
    val musicStateLiveData = MutableLiveData<MusicState>()
    val currentValueLiveData = MutableLiveData<Int>()
    val progressChangeLiveData = MutableLiveData<Int>()
}