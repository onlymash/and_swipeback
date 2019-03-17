package com.aitangba.swipeback;

import android.app.Activity;

public interface SlideBackManager {

    Activity getSlideActivity();

    /**
     * 是否支持滑动返回
     */
    boolean supportSlideBack();

    /**
     * 能否滑动返回至当前Activity
     */
    boolean canBeSlideBack();
}
