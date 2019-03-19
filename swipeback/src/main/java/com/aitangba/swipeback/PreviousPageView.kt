package com.aitangba.swipeback

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View

import java.lang.ref.WeakReference

internal class PreviousPageView(context: Context?) : View(context) {
    private var view: View? = null
    private val innerHandler: InnerHandler

    init {
        innerHandler = InnerHandler(this)
    }

    fun cacheView(view: View?) {
        this.view = view
        display(false)
    }

    override fun onDraw(canvas: Canvas) {
        view?.draw(canvas)
    }

    private fun display(delay: Boolean) {
        invalidate()
        innerHandler.sendEmptyMessageDelayed(MSG_SHOW_FRAME, (if (delay) 60 else 0).toLong())
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        innerHandler.removeCallbacksAndMessages(null)
    }

    @SuppressLint("HandlerLeak")
    private inner class InnerHandler constructor(previousPageView: PreviousPageView) : Handler(Looper.getMainLooper()) {
        private val weakReference: WeakReference<PreviousPageView> = WeakReference(previousPageView)
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            weakReference.get()?.display(true)
        }
    }

    companion object {

        private const val MSG_SHOW_FRAME = 1
    }
}
