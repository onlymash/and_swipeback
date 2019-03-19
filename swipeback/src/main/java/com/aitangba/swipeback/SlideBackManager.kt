package com.aitangba.swipeback

import android.app.Activity

interface SlideBackManager {

    fun getSlideActivity(): Activity?

    /**
     * 是否支持滑动返回
     */
    fun supportSlideBack(): Boolean

    /**
     * 能否滑动返回至当前Activity
     */
    fun canBeSlideBack(): Boolean

    fun getSwipeEdgeSize(): Int
}
