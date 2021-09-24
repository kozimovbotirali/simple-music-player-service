package com.sablab.android_simple_music_player.data.sources.local

import android.content.Context

class LocalStorage private constructor(context: Context) {
    companion object {
        @Volatile
        lateinit var instance: LocalStorage
            private set

        fun init(context: Context) {
            instance =
                LocalStorage(
                    context
                )
        }
    }

    private val pref = context.getSharedPreferences("LocalStorage", Context.MODE_PRIVATE)

    var isPlaying: Boolean by BooleanPreference(pref, false)

    var lastPlayedPosition: Int by IntPreference(pref, 0)

    fun clear() {
        pref.edit().clear().apply()
    }
}