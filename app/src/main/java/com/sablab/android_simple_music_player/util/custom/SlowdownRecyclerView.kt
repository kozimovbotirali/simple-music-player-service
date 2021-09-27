package com.sablab.android_simple_music_player.util.custom

import android.content.Context
import android.util.AttributeSet
import android.view.animation.Interpolator
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.pow

class SlowdownRecyclerView : RecyclerView {

    private var interpolator: Interpolator? = null

    constructor(context: Context) : super(context) {
        createInterpolator()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        createInterpolator()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        createInterpolator()
    }

    private fun createInterpolator() {
        interpolator = Interpolator { t ->
            var t1 = t
            t1 = abs(t1 - 1.0f)
            (1.0f - t1.toDouble().pow(POW.toDouble())).toFloat()
        }
    }

    override fun smoothScrollBy(dx: Int, dy: Int) {
        super.smoothScrollBy(dx, dy, interpolator)
    }

    companion object {

        // Change pow to control speed.
        // Bigger = faster. RecyclerView default is 5.
        private const val POW = 10
    }
}
