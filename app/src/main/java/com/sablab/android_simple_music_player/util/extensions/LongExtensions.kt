package com.sablab.android_simple_music_player.util.extensions

import java.util.concurrent.TimeUnit

/**
 * Created by B.Kozimov on 27.09.2021 14:15.
 */

fun Long.toFormattedString(): String =
    String.format(
        "%02d:%02d",
        TimeUnit.MILLISECONDS.toMinutes(this) - TimeUnit.HOURS.toMinutes(
            TimeUnit.MILLISECONDS.toHours(
                this
            )
        ),
        TimeUnit.MILLISECONDS.toSeconds(
            this
        ) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(this))
    )
