package com.sablab.android_simple_music_player.util.extensions

import com.google.gson.Gson

fun <T> String.fromGson(clazz: Class<T>): T? {
    return Gson().fromJson(this, clazz)
}

fun Any.toGson(): String {
    return Gson().toJson(this)
}