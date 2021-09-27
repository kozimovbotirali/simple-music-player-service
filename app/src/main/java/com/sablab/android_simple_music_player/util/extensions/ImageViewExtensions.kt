@file:Suppress("unused")

package com.sablab.android_simple_music_player.util.extensions

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.sablab.android_simple_music_player.util.dpToPx

/**
 * Created by Botirali Kozimov on 12.03.21
 **/

fun ImageView.loadImage(data: Bitmap) {
    Glide.with(this).load(data).centerCrop().into(this)
}

fun ImageView.loadImage(data: Int?, corners: Int? = null, onLoadingFinished: () -> Unit = {}) {
    val listener = object : RequestListener<Drawable> {
        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: com.bumptech.glide.request.target.Target<Drawable>?,
            isFirstResource: Boolean
        ): Boolean {
            onLoadingFinished()
            return false
        }

        override fun onResourceReady(
            resource: Drawable?,
            model: Any?,
            target: com.bumptech.glide.request.target.Target<Drawable>?,
            dataSource: DataSource?,
            isFirstResource: Boolean
        ): Boolean {
            onLoadingFinished()
            return false
        }
    }
    Glide.with(this)
        .load(data)
//        .placeholder(R.drawable.ic_music)
        .transform(
            RoundedCorners((corners ?: 6).dpToPx(this.context))
        )
        .listener(listener)
        .into(this)
}

fun ImageView.loadImage(data: Uri?, corners: Int? = null, onLoadingFinished: () -> Unit = {}) {
    val listener = object : RequestListener<Drawable> {
        override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: com.bumptech.glide.request.target.Target<Drawable>?,
            isFirstResource: Boolean
        ): Boolean {
            onLoadingFinished()
            return false
        }

        override fun onResourceReady(
            resource: Drawable?,
            model: Any?,
            target: com.bumptech.glide.request.target.Target<Drawable>?,
            dataSource: DataSource?,
            isFirstResource: Boolean
        ): Boolean {
            onLoadingFinished()
            return false
        }
    }
    Glide.with(this)
        .load(data)
//        .placeholder(R.drawable.ic_music)
        .transform(
            RoundedCorners((corners ?: 6).dpToPx(this.context))
        )
        .listener(listener)
        .into(this)
}