@file:Suppress("unused")

package com.sablab.android_simple_music_player.util.extensions

import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.sablab.android_simple_music_player.R

/**
 * Created by Botirali Kozimov on 12.03.21
 **/

fun ImageView.loadImage(data: Bitmap) {
    Glide.with(this).load(data).centerCrop().into(this)
}

fun ImageView.loadImage(data: Uri?) {
    Glide.with(this).load(data).centerCrop().placeholder(R.drawable.ic_music).into(this)
}