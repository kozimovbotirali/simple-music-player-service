package com.sablab.android_simple_music_player.data.models.enums

import com.sablab.android_simple_music_player.data.models.Music

sealed class MusicState(val position: Int, val data: Music?) {
    class PLAYING(position: Int, data: Music?) : MusicState(position, data)
    class PAUSE(position: Int, data: Music?) : MusicState(position, data)
    class STOP(position: Int, data: Music?) : MusicState(position, data)
    class NEXT_OR_PREV(position: Int, data: Music?) : MusicState(position, data)
}