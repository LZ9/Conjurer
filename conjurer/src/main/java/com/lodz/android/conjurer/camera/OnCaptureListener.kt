package com.lodz.android.conjurer.camera

import android.graphics.Point

/**
 * 图像捕获回调
 * @author zhouL
 * @date 2022/8/16
 */
fun interface OnCaptureListener {
    fun onCapture(cameraResolution: Point, screenResolution: Point, data: ByteArray?)
}