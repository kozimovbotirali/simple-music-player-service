package com.sablab.android_simple_music_player.util.custom

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import com.sablab.android_simple_music_player.R

@SuppressLint("AppCompatCustomView")
class PlayPauseButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ImageView(context, attrs, defStyle) {
    private var listenerChange: OnChangeListener? = null

    var isChecked: Boolean = false
        set(value) {
            field = value
            updateSwitchState()
        }

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.PlayPauseButton)
        isChecked = typedArray.getBoolean(R.styleable.PlayPauseButton_icChecked, isChecked)
        typedArray.recycle()

        updateSwitchState()
        super.setOnClickListener {
            isChecked = !isChecked
            updateSwitchState()
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        //super.setOnClickListener(l)
    }

    private fun updateSwitchState() {
        setImageResource(if (!isChecked) R.drawable.ic_play else R.drawable.ic_pause)
        listenerChange?.onChanged(isChecked)
    }

    fun setOnChangeListener(block: OnChangeListener) {
        listenerChange = block
    }

    //SAM
    fun interface OnChangeListener {
        fun onChanged(isChecked: Boolean)
    }
}