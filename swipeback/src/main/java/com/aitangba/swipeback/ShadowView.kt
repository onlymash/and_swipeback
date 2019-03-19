package com.aitangba.swipeback

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View

internal class ShadowView(context: Context) : View(context) {

    private val drawable: Drawable

    init {
        val colors = intArrayOf(0x00000000, 0x00000000, 0x00000000)//分别为开始颜色，中间夜色，结束颜色
        drawable = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawable.setBounds(0, 0, measuredWidth, measuredHeight)
        drawable.draw(canvas)
    }
}

