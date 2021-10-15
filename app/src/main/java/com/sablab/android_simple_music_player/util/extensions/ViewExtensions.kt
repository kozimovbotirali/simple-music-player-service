package com.sablab.android_simple_music_player.util.extensions

import android.view.View

/**
 * Created by B.Kozimov on 04.10.2021 10:24.
 */
fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.inVisible() {
    this.visibility = View.INVISIBLE
}

fun View.gone() {
    this.visibility = View.GONE
}