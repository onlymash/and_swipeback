package com.aitangba.swipeback

import android.view.MotionEvent

interface SwipeIntercept {
    fun processTouchEvent(ev: MotionEvent?): Boolean
    fun finishSwipeImmediately()
    fun setEdgeSize(size: Int)
}
