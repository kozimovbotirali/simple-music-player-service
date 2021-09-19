package com.sablab.android_simple_music_player.util.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.sablab.android_simple_music_player.R

class ItemDecorationWithLeftPadding(context: Context, private val intLeft: Int) :
    RecyclerView.ItemDecoration() {

    private var mDivider: Drawable? = ContextCompat.getDrawable(context, R.drawable.line_divider)

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        if (mDivider == null) {
            super.onDrawOver(c, parent, state)
            return
        }

        val left = intLeft
        val right = parent.width - parent.paddingRight
        val childCount = parent.childCount - 1

        for (i in 1 until childCount) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams
            val size = mDivider?.intrinsicHeight ?: 0
            val top = (child.top - params.topMargin - size + child.translationY)
            val bottom = top + size
            mDivider?.setBounds(left, top.toInt(), right, bottom.toInt())
            mDivider?.draw(c)

            if (i == childCount - 1) {
                val newTop = (child.bottom + params.bottomMargin + child.translationY)
                val newBottom = newTop + size
                mDivider?.setBounds(left, newTop.toInt(), right, newBottom.toInt())
                mDivider?.draw(c)
            }
        }
    }
}