package com.aitangba.swipeback

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.Window
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout

class SwipeHelper(slideBackManager: SlideBackManager) : SwipeIntercept {

    private val isSupportSlideBack: Boolean // 是否支持滑动返回
    private val touchSlop: Int // 判断滑动事件触发
    private var edgeSize: Int  //px 拦截手势区间

    private var isInThresholdArea: Boolean = false // 点击事件是否在监控区域（action_down事件不在范围内，则后续所有事件都不作处理）
    private var lastPointX: Float = 0.toFloat()  //记录手势在屏幕上的X轴坐标

    private var isSlideAnimPlaying: Boolean = false //滑动动画展示过程中
    private var isSliding: Boolean = false //是否正在滑动
    private var distanceX: Float = 0.toFloat()  //px 当前滑动距离 （正数或0）

    private val viewManager: ViewManager
    private val activity: Activity = slideBackManager.getSlideActivity() ?:
    throw RuntimeException("Neither SlideBackManager nor the method 'getSlideActivity()' can be null!")
    private val currentContentView: FrameLayout?
    private var animatorSet: AnimatorSet? = null

    init {
        viewManager = ViewManager()
        isSupportSlideBack = slideBackManager.supportSlideBack()
        currentContentView = getContentView(activity)

        touchSlop = ViewConfiguration.get(activity).scaledTouchSlop

        val density = activity.resources.displayMetrics.density
        edgeSize = (EDGE_SIZE * density + 0.5f).toInt() //滑动拦截事件的区域
    }

    override fun setEdgeSize(size: Int) {
        if (size > 0) edgeSize = size
    }

    override fun processTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null) return false
        if (!isSupportSlideBack) { //不支持滑动返回，则手势事件交给View处理
            return false
        }

        if (isSlideAnimPlaying) {  //正在滑动动画播放中，直接消费手势事件
            return true
        }

        val action = ev.actionMasked
        if (action == MotionEvent.ACTION_DOWN) {
            lastPointX = ev.rawX
            isInThresholdArea = lastPointX >= 0 && lastPointX <= edgeSize
        }

        if (!isInThresholdArea) {  //不满足滑动区域，不做处理
            return false
        }

        val actionIndex = ev.actionIndex
        when (action) {
            MotionEvent.ACTION_DOWN -> if (!viewManager.addViews()) {
                return false
            }

            MotionEvent.ACTION_POINTER_DOWN -> if (isSliding) {  //有第二个手势事件加入，而且正在滑动事件中，则直接消费事件
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                //一旦触发滑动机制，拦截所有其他手指的滑动事件
                if (actionIndex != 0) {
                    return isSliding
                }

                val curPointX = ev.rawX

                val sliding = isSliding
                if (!sliding) {
                    if (Math.abs(curPointX - lastPointX) < touchSlop) { //判断是否满足滑动
                        return false
                    } else {
                        isSliding = true
                    }
                }
                onSliding(curPointX)
                return if (sliding == isSliding) {
                    true
                } else {
                    val cancelEvent = MotionEvent.obtain(ev) //首次判定为滑动需要修正事件：手动修改事件为 ACTION_CANCEL，并通知底层View
                    cancelEvent.action = MotionEvent.ACTION_CANCEL
                    activity.window.superDispatchTouchEvent(cancelEvent)
                    true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_OUTSIDE -> {
                if (distanceX == 0f) { //没有进行滑动
                    isSliding = false
                    onActionUp()
                    return false
                }

                if (isSliding && actionIndex == 0) { // 取消滑动 或 手势抬起 ，而且手势事件是第一手势，开始滑动动画
                    isSliding = false
                    onActionUp()
                    return true
                } else if (isSliding) {
                    return true
                }
            }
            else -> isSliding = false
        }
        return false
    }

    override fun finishSwipeImmediately() {
        animatorSet?.cancel()
    }

    private fun onActionUp() {
        val width = activity.resources.displayMetrics.widthPixels
        if (distanceX == 0f) {
            resetViewsAndStatus()
        } else if (distanceX > width / 4) {
            startSlideAnim(false)
        } else {
            startSlideAnim(true)
        }
    }

    private fun resetViewsAndStatus() {
        distanceX = 0f
        isSliding = false
        isSlideAnimPlaying = false

        viewManager.removeViews()
    }

    /**
     * 手动处理滑动事件
     */
    private fun onSliding(curPointX: Float) {
        val width = activity.resources.displayMetrics.widthPixels
        val previewActivityContentView = viewManager.previousContentView
        val shadow = viewManager.shadowView
        val currentActivityContentView = viewManager.displayView
        if (previewActivityContentView == null || currentActivityContentView == null || shadow == null) {
            resetViewsAndStatus()
            return
        }
        val dX = curPointX - lastPointX
        lastPointX = curPointX
        distanceX += dX
        if (distanceX < 0) {
            distanceX = 0f
        }

        previewActivityContentView.x = -width / 3f + distanceX / 3
        shadow.x = distanceX - SHADOW_WIDTH
        currentActivityContentView.x = distanceX
    }

    /**
     * 开始自动滑动动画
     *
     * @param slideCanceled 是不是要返回（true则不关闭当前页面）
     */
    private fun startSlideAnim(slideCanceled: Boolean) {
        val previewView = viewManager.previousContentView ?: return
        val shadowView = viewManager.shadowView
        val currentView = viewManager.displayView

        val width = activity.resources.displayMetrics.widthPixels
        val interpolator = DecelerateInterpolator(2f)

        // preview activity's animation
        val previewViewAnim = ObjectAnimator()
        previewViewAnim.interpolator = interpolator
        previewViewAnim.setProperty(View.TRANSLATION_X)
        val preViewStart = distanceX / 3 - width / 3f
        val preViewStop = if (slideCanceled) -width / 3f else 0f
        previewViewAnim.setFloatValues(preViewStart, preViewStop)
        previewViewAnim.target = previewView

        // shadow view's animation
        val shadowViewAnim = ObjectAnimator()
        shadowViewAnim.interpolator = interpolator
        shadowViewAnim.setProperty(View.TRANSLATION_X)
        val shadowViewStart = distanceX - SHADOW_WIDTH
        val shadowViewEnd = (if (slideCanceled) SHADOW_WIDTH else width + SHADOW_WIDTH).toFloat()
        shadowViewAnim.setFloatValues(shadowViewStart, shadowViewEnd)
        shadowViewAnim.target = shadowView

        // current view's animation
        val currentViewAnim = ObjectAnimator()
        currentViewAnim.interpolator = interpolator
        currentViewAnim.setProperty(View.TRANSLATION_X)
        val curViewStart = distanceX
        val curViewStop = (if (slideCanceled) 0 else width).toFloat()
        currentViewAnim.setFloatValues(curViewStart, curViewStop)
        currentViewAnim.target = currentView

        // play animation together
        animatorSet = AnimatorSet().apply {
            duration = (if (slideCanceled) 150 else 300).toLong()
            playTogether(previewViewAnim, shadowViewAnim, currentViewAnim)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    animation.removeAllListeners()
                    cancel()
                    if (slideCanceled) {
                        resetViewsAndStatus()
                    } else {
                        activity.finish()
                        activity.overridePendingTransition(android.R.anim.fade_in, R.anim.swipeback_hold_on)
                    }
                }
            })
            start()
        }
        isSlideAnimPlaying = true
    }

    private fun getContentView(activity: Activity): FrameLayout? =
            activity.findViewById<View>(Window.ID_ANDROID_CONTENT) as? FrameLayout

    private inner class ViewManager {
        internal var previousActivity: Activity? = null
        internal var previousContentView: PreviousPageView? = null
        internal var shadowView: View? = null

        internal val displayView: View?
            get() {
                var index = 0
                previousContentView?.run {
                    index += 1
                }
                shadowView?.run {
                    index += 1
                }
                return currentContentView?.getChildAt(index)
            }

        /**
         * Remove view from previous Activity and add into current Activity
         *
         * @return Is view added successfully
         */
        internal fun addViews(): Boolean {
            if (currentContentView == null || currentContentView.childCount == 0) {
                previousActivity = null
                previousContentView = null
                return false
            }
            previousActivity = ActivityLifecycleHelper.getPreviousActivity()
            if (previousActivity == null) {
                previousContentView = null
                return false
            }
            // previous activity not support to be swipeBack...
            if (previousActivity is SlideBackManager && !(previousActivity as SlideBackManager).canBeSlideBack()) {
                previousActivity = null
                previousContentView = null
                return false
            }
            val previousActivityContainer = getContentView(previousActivity!!)
            if (previousActivityContainer == null || previousActivityContainer.childCount == 0) {
                previousActivity = null
                previousContentView = null
                return false
            }
            // add shadow view on the left of content view
            shadowView = ShadowView(activity).apply {
                x = (-SHADOW_WIDTH).toFloat()
            }
            val shadowLayoutParams = FrameLayout.LayoutParams(
                    SHADOW_WIDTH, FrameLayout.LayoutParams.MATCH_PARENT)
            currentContentView.addView(this.shadowView, 0, shadowLayoutParams)

            // add the cache view which cache the view of previous activity
            val view = previousActivityContainer.getChildAt(0)
            previousContentView = PreviousPageView(activity)
            previousContentView!!.cacheView(view)
            val layoutParams = FrameLayout.LayoutParams(view.measuredWidth, view.measuredHeight)
            currentContentView.addView(previousContentView, 0, layoutParams)
            return true
        }

        internal fun removeViews() {
            // remove the shadowView at current Activity
            shadowView?.let {
                currentContentView?.removeView(it)
                shadowView = null
            }
            // remove the previousContentView at current Activity
            previousContentView?.let {
                it.cacheView(null)
                currentContentView?.removeView(it)
                previousContentView = null
            }
            previousActivity = null
        }
    }

    companion object {
        private const val EDGE_SIZE = 20  //dp 默认拦截手势区间
        private const val SHADOW_WIDTH = 50 //px 阴影宽度
    }
}
