package com.aitangba.swipeback

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.res.TypedArray
import android.os.Bundle
import android.os.Handler
import android.os.Message

import androidx.core.content.ContextCompat
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.Window
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout

/**
 * Created by fhf11991 on 2016/9/18.
 */
/**
 *
 * @param slideBackManager
 */
class SwipeBackHelper(slideBackManager: SlideBackManager?) : Handler(), SwipeIntercept {

    private var edgeSize: Int  //px 拦截手势区间
    private var isSliding: Boolean = false //是否正在滑动
    private var isSlideAnimPlaying: Boolean = false //滑动动画展示过程中
    private var distanceX: Float = 0.toFloat()  //px 当前滑动距离 （正数或0）
    private var lastPointX: Float = 0.toFloat()  //记录手势在屏幕上的X轴坐标

    private val isSupportSlideBack: Boolean //
    private val touchSlop: Int
    private var isInThresholdArea: Boolean = false

    private var activity: Activity? = null
    private val viewManager: ViewManager
    private val currentContentView: FrameLayout?
    private var animatorSet: AnimatorSet? = null

    private val windowBackgroundColor: Int
        get() {
            var array: TypedArray? = null
            try {
                array = activity!!.theme.obtainStyledAttributes(intArrayOf(android.R.attr.windowBackground))
                return array!!.getColor(0, ContextCompat.getColor(activity!!, android.R.color.transparent))
            } finally {
                array?.recycle()
            }
        }

    init {
        activity = slideBackManager?.getSlideActivity() ?: throw RuntimeException("Neither SlideBackManager nor the method 'getSlideActivity()' can be null!")
        isSupportSlideBack = slideBackManager.supportSlideBack()
        currentContentView = getContentView(activity!!)
        viewManager = ViewManager()

        touchSlop = ViewConfiguration.get(activity).scaledTouchSlop

        val density = activity!!.resources.displayMetrics.density
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

        val action = ev.action and MotionEvent.ACTION_MASK
        if (action == MotionEvent.ACTION_DOWN) {
            lastPointX = ev.rawX
            isInThresholdArea = lastPointX >= 0 && lastPointX <= edgeSize
        }

        if (!isInThresholdArea) {  //不满足滑动区域，不做处理
            return false
        }

        val actionIndex = ev.actionIndex
        when (action) {
            MotionEvent.ACTION_DOWN -> sendEmptyMessage(MSG_ACTION_DOWN)

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

                val bundle = Bundle()
                bundle.putFloat(CURRENT_POINT_X, curPointX)
                val message = obtainMessage()
                message.what = MSG_ACTION_MOVE
                message.data = bundle
                sendMessage(message)

                return if (sliding == isSliding) {
                    true
                } else {
                    val cancelEvent = MotionEvent.obtain(ev) //首次判定为滑动需要修正事件：手动修改事件为 ACTION_CANCEL，并通知底层View
                    cancelEvent.action = MotionEvent.ACTION_CANCEL
                    activity!!.window.superDispatchTouchEvent(cancelEvent)
                    true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_OUTSIDE -> {
                if (distanceX == 0f) { //没有进行滑动
                    isSliding = false
                    sendEmptyMessage(MSG_ACTION_UP)
                    return false
                }

                if (isSliding && actionIndex == 0) { // 取消滑动 或 手势抬起 ，而且手势事件是第一手势，开始滑动动画
                    isSliding = false
                    sendEmptyMessage(MSG_ACTION_UP)
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
        if (isSliding) {
            viewManager.addCacheView()
            viewManager.resetPreviousView()
        }

        if (animatorSet != null) {
            animatorSet!!.cancel()
        }

        removeMessages(MSG_ACTION_DOWN)
        removeMessages(MSG_ACTION_MOVE)
        removeMessages(MSG_ACTION_UP)
        removeMessages(MSG_SLIDE_CANCEL)
        removeMessages(MSG_SLIDE_CANCELED)
        removeMessages(MSG_SLIDE_PROCEED)
        removeMessages(MSG_SLIDE_FINISHED)

        activity = null
    }

    override fun handleMessage(msg: Message) {
        super.handleMessage(msg)
        when (msg.what) {
            MSG_ACTION_DOWN -> {
                // hide input method
                val inputMethod = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                val view = activity?.currentFocus
                if (view != null && inputMethod != null) {
                    inputMethod.hideSoftInputFromWindow(view.windowToken, 0)
                }

                if (!viewManager.addViewFropreviousActivity()) return

                // add shadow view on the left of content view
                viewManager.addShadowView()

                if (currentContentView != null && currentContentView.childCount >= 3) {
                    viewManager.displayView?.apply {
                        if (background == null) {
                            setBackgroundColor(windowBackgroundColor)
                        }
                    }
                }
            }

            MSG_ACTION_MOVE -> {
                val curPointX = msg.data.getFloat(CURRENT_POINT_X)
                onSliding(curPointX)
            }

            MSG_ACTION_UP -> {
                val width = activity!!.resources.displayMetrics.widthPixels
                if (distanceX == 0f) {
                    if (currentContentView!!.childCount >= 3) {
                        viewManager.removeShadowView()
                        viewManager.resetPreviousView()
                    }
                } else if (distanceX > width / 4) {
                    sendEmptyMessage(MSG_SLIDE_PROCEED)
                } else {
                    sendEmptyMessage(MSG_SLIDE_CANCEL)
                }
            }

            MSG_SLIDE_CANCEL -> startSlideAnim(true)

            MSG_SLIDE_CANCELED -> {
                distanceX = 0f
                isSliding = false
                viewManager.removeShadowView()
                viewManager.resetPreviousView()
            }

            MSG_SLIDE_PROCEED -> startSlideAnim(false)

            MSG_SLIDE_FINISHED -> {
                viewManager.addCacheView()
                viewManager.removeShadowView()
                viewManager.resetPreviousView()
                
                activity?.apply {
                    finish()
                    overridePendingTransition(android.R.anim.fade_in, R.anim.swipeback_hold_on)
                }
            }
        }
    }

    /**
     * 手动处理滑动事件
     */
    @Synchronized
    private fun onSliding(curPointX: Float) {
        val width = activity!!.resources.displayMetrics.widthPixels
        val previewActivityContentView = viewManager.previousContentView
        val shadowView = viewManager.shadowView
        val currentActivityContentView = viewManager.displayView

        if (previewActivityContentView == null || currentActivityContentView == null || shadowView == null) {
            sendEmptyMessage(MSG_SLIDE_CANCELED)
            return
        }

        val dX = curPointX - lastPointX
        lastPointX = curPointX
        distanceX += dX
        if (distanceX < 0f) {
            distanceX = 0f
        }

        previewActivityContentView.x = -width / 3.0f + distanceX / 3
        shadowView.x = distanceX - SHADOW_WIDTH
        currentActivityContentView.x = distanceX
    }

    /**
     * 开始自动滑动动画
     *
     * @param slideCanceled 是不是要返回（true则不关闭当前页面）
     */
    private fun startSlideAnim(slideCanceled: Boolean) {
        val previewView = viewManager.previousContentView
        val shadowView = viewManager.shadowView
        val currentView = viewManager.displayView

        if (previewView == null || currentView == null) {
            return
        }

        val width = activity!!.resources.displayMetrics.widthPixels
        val interpolator = DecelerateInterpolator(2f)

        // preview activity's animation
        val previewViewAnim = ObjectAnimator()
        previewViewAnim.interpolator = interpolator
        previewViewAnim.setProperty(View.TRANSLATION_X)
        val preViewStart = distanceX / 3 - width / 3.0f
        val preViewStop = if (slideCanceled) -width / 3.0f else 0f
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
                    if (slideCanceled) {
                        isSlideAnimPlaying = false
                        previewView.x = 0f
                        shadowView!!.x = (-SHADOW_WIDTH).toFloat()
                        currentView.x = 0f
                        sendEmptyMessage(MSG_SLIDE_CANCELED)
                    } else {
                        sendEmptyMessage(MSG_SLIDE_FINISHED)
                    }
                }
            })
            start()
        }
        isSlideAnimPlaying = true
    }


    private fun getContentView(activity: Activity): FrameLayout? {
        return activity.findViewById<View>(Window.ID_ANDROID_CONTENT) as FrameLayout
    }

    internal inner class ViewManager {
        private var previousActivity: Activity? = null
        internal var previousContentView: View? = null
        internal var shadowView: View? = null

        internal val displayView: View?
            get() {
                var index = 0
                viewManager.previousContentView?.run {
                    index += 1
                }

                viewManager.shadowView?.run {
                    index += 1
                }
                return currentContentView?.getChildAt(index)
            }

        /**
         * Remove view from previous Activity and add into current Activity
         * @return Is view added successfully
         */
        internal fun addViewFropreviousActivity(): Boolean {
            if (currentContentView!!.childCount == 0) {
                previousActivity = null
                previousContentView = null
                return false
            }

            previousActivity = ActivityLifecycleHelper.getPreviousActivity()
            if (previousActivity == null) {
                previousContentView = null
                return false
            }

            //Previous activity not support to be swipeBack...
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

            previousContentView = previousActivityContainer.getChildAt(0)
            previousActivityContainer.removeView(previousContentView)
            currentContentView.addView(previousContentView, 0)
            return true
        }

        /**
         * Remove the PreviousContentView at current Activity and put it into previous Activity.
         */
        internal fun resetPreviousView() {
            if (previousContentView == null) return

            val view = previousContentView
            view!!.x = 0f
            currentContentView!!.removeView(view)
            previousContentView = null

            if (previousActivity == null || previousActivity!!.isFinishing) return
            val preActivity = previousActivity
            val previewContentView = getContentView(preActivity!!)
            previewContentView!!.addView(view)
            previousActivity = null
        }

        /**
         * add shadow view on the left of content view
         */
        @Synchronized
        internal fun addShadowView() {
            if (shadowView == null) {
                shadowView = ShadowView(activity!!).apply {
                    x = (-SHADOW_WIDTH).toFloat()
                }
            }
            val layoutParams = FrameLayout.LayoutParams(
                    SHADOW_WIDTH, FrameLayout.LayoutParams.MATCH_PARENT)

            if (this.shadowView!!.parent == null) {
                currentContentView!!.addView(this.shadowView, 1, layoutParams)
            } else {
                this.removeShadowView()
                this.addShadowView()
            }
        }

        @Synchronized
        internal fun removeShadowView() {
            if (shadowView == null) return
            getContentView(activity!!)?.apply {
                removeView(shadowView)
            }
            shadowView = null
        }

        internal fun addCacheView() {
            val previousView = previousContentView
            val previousPageView = PreviousPageView(activity!!)
            currentContentView!!.addView(previousPageView, 0)
            previousPageView.cacheView(previousView!!)
        }
    }

    companion object {

        private const val CURRENT_POINT_X = "currentPointX" //点击事件

        private const val MSG_ACTION_DOWN = 1 //点击事件
        private const val MSG_ACTION_MOVE = 2 //滑动事件
        private const val MSG_ACTION_UP = 3  //点击结束
        private const val MSG_SLIDE_CANCEL = 4 //开始滑动，不返回前一个页面
        private const val MSG_SLIDE_CANCELED = 5  //结束滑动，不返回前一个页面
        private const val MSG_SLIDE_PROCEED = 6 //开始滑动，返回前一个页面
        private const val MSG_SLIDE_FINISHED = 7//结束滑动，返回前一个页面

        private const val SHADOW_WIDTH = 50 //px 阴影宽度
        private const val EDGE_SIZE = 20  //dp 默认拦截手势区间
    }
}