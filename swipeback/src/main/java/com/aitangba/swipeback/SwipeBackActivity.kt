package com.aitangba.swipeback

import android.app.Activity

import androidx.appcompat.app.AppCompatActivity
import android.view.MotionEvent

open class SwipeBackActivity : AppCompatActivity(), SlideBackManager {

    private var swipeBackHelper: SwipeIntercept? = null

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (swipeBackHelper == null) {
            swipeBackHelper = SwipeHelper(this).apply {
                setEdgeSize(getSwipeEdgeSize())
            }
        }
        return swipeBackHelper!!.processTouchEvent(ev) || super.dispatchTouchEvent(ev)
    }

    override fun getSwipeEdgeSize(): Int = -1

    override fun getSlideActivity(): Activity? {
        return this
    }

    override fun supportSlideBack(): Boolean {
        return true
    }

    override fun canBeSlideBack(): Boolean {
        return true
    }

    override fun finish() {
        if (swipeBackHelper != null) {
            swipeBackHelper!!.finishSwipeImmediately()
            swipeBackHelper = null
        }
        super.finish()
    }
}
