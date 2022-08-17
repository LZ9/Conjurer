package com.lodz.android.conjurer.widget

import android.graphics.Rect

/**
 * 取景器变化监听器
 * @author zhouL
 * @date 2022/8/17
 */
fun interface OnViewfinderChangeListener {

    fun onRectChanged(rect: Rect)

}