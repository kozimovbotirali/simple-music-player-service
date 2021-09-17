package com.sablab.android_simple_music_player.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Music(
    var id: Long? = null,
    var artist: String? = null,
    var title: String? = null,
    var data: String? = null,
    var displayName: String? = null,
    var duration: Long? = null
) : Parcelable
