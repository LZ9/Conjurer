package com.lodz.android.conjurer.camera

import android.graphics.Point
import android.hardware.Camera

/**
 * 图像捕获回调
 * @author zhouL
 * @date 2022/8/16
 */
class CaptureCallback(
    private val cameraResolution: Point?,
    private val listener: OnCaptureListener
) : Camera.PreviewCallback {

    override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
        if (cameraResolution != null) {
            listener.onCapture(cameraResolution.x, cameraResolution.y, data)
        }
    }

}