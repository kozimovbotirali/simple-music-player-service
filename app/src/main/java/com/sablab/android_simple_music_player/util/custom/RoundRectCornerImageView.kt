package com.sablab.android_simple_music_player.util.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.DisplayMetrics
import androidx.appcompat.widget.AppCompatImageView
import com.sablab.android_simple_music_player.R
import kotlin.math.roundToInt

class RoundRectCornerImageView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AppCompatImageView(context, attrs, defStyle) {
    var radius: Int = 6.dpToPx(context)
        set(value) {
            field = value.dpToPx(context)
            invalidate()
        }
    private var path: Path? = null

    init {
        val a = context.resources.obtainAttributes(attrs, R.styleable.RoundRectCornerImageView)
        val value = a.getInt(R.styleable.RoundRectCornerImageView_radius, radius)
        if (value != radius)
            radius = value
        a.recycle()
        path = Path()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        val rect = RectF(0.toFloat(), 0.toFloat(), this.width.toFloat(), this.height.toFloat())
        path?.addRoundRect(rect, radius.toFloat(), radius.toFloat(), Path.Direction.CW)
        path?.let { canvas.clipPath(it) }
        super.onDraw(canvas)
    }
}

fun Int.dpToPx(context: Context): Int {
    return (this * (context.resources.displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()
}