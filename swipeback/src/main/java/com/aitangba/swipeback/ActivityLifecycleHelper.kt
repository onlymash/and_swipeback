package com.aitangba.swipeback

import android.app.Activity
import android.app.Application
import android.os.Bundle

import java.util.LinkedList

class ActivityLifecycleHelper : Application.ActivityLifecycleCallbacks {

    init {
        activities = LinkedList()
    }

    override fun onActivityPaused(activity: Activity?) {

    }

    override fun onActivityResumed(activity: Activity?) {

    }

    override fun onActivityStarted(activity: Activity?) {

    }

    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {

    }

    override fun onActivityStopped(activity: Activity?) {

    }

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
        activity?.let {
            addActivity(it)
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        activities?.let {
            it.remove(activity)
            if (it.size == 0) {
                activities = null
            }
        }
    }

    /**
     * 添加Activity到堆栈
     */
    private fun addActivity(activity: Activity?) {
        if (activity == null) return
        if (activities == null) {
            activities = LinkedList()
        }
        activities!!.add(activity)
    }

    companion object {

        private var singleton: ActivityLifecycleHelper? = null
        private val lockObj = Any()
        private var activities: MutableList<Activity>? = null

        fun build(): ActivityLifecycleHelper {
            synchronized(lockObj) {
                if (singleton == null) {
                    singleton = ActivityLifecycleHelper()
                }
                return singleton!!
            }
        }

        /**
         * 获取集合中当前Activity
         */
        fun getLatestActivity(): Activity? {
            val count = activities?.size
            if (count == null || count == 0) {
                return null
            }
            return activities!![count - 1]
        }

        /**
         * 获取集合中上一个Activity
         */
        fun getPreviousActivity(): Activity? {
            val count = activities?.size
            if (count == null || count < 2) {
                return null
            }
            return activities!![count - 2]
        }
    }
}