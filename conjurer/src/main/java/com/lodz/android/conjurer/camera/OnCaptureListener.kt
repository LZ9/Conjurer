package com.lodz.android.conjurer.camera

/**
 * 图像捕获回调
 * @author zhouL
 * @date 2022/8/16
 */
fun interface OnCaptureListener {
    fun onCapture(width: Int, height: Int, data: ByteArray?)
}