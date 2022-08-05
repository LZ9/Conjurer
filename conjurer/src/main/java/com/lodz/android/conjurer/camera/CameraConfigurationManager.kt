package com.lodz.android.conjurer.camera


import android.content.Context
import android.graphics.Point
import android.hardware.Camera
import android.view.WindowManager
import java.util.*
import kotlin.math.abs

/**
 * 相机配置管理器
 * @author zhouL
 * @date 2022/8/5
 */
class CameraConfigurationManager(private val context: Context) {

    private val MIN_PREVIEW_PIXELS = 470 * 320//最小预览分辨率
    private val MAX_PREVIEW_PIXELS = 800 * 600//最大预览分辨率

    private var mScreenResolution: Point? = null
    private var mCameraResolution: Point? = null


    /** 初始化相机参数 */
    fun initFromCameraParameters(camera: Camera) {
        val parameters = camera.parameters
        val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = manager.defaultDisplay
        var width = display.width
        var height = display.height
        if (width < height){//纵向要反转
            val temp = width
            width = height
            height = temp
        }
        val screenResolution = Point(width, height)
        mScreenResolution = screenResolution
        mCameraResolution = findBestPreviewSizeValue(parameters, screenResolution)
    }

    fun setDesiredCameraParameters(camera: Camera){
        val parameters = camera.parameters ?: return
        initializeTorch(parameters)
        var focusMode = findSettableValue(parameters.supportedFocusModes, Camera.Parameters.FOCUS_MODE_AUTO)
        if (focusMode.isEmpty()) {
             focusMode = findSettableValue(parameters.supportedFocusModes, Camera.Parameters.FOCUS_MODE_MACRO, "edof")
        }
        if (focusMode.isNotEmpty()){
            parameters.focusMode = focusMode
        }
        val point = mCameraResolution
        if (point != null) {
            parameters.setPreviewSize(point.x, point.y)
        }
        camera.parameters = parameters
    }

    fun getCameraResolution(): Point? = mCameraResolution

    fun getScreenResolution(): Point? = mScreenResolution

    /** 开关闪光灯 */
    fun setTorch(camera: Camera, isOpen: Boolean) {
        val parameters = camera.parameters
        doSetTorch(parameters, isOpen)
        camera.parameters = parameters
    }

    /** 初始化关闭闪光灯 */
    private fun initializeTorch(parameters: Camera.Parameters) {
        doSetTorch(parameters, false)
    }

    /** 开关闪光灯 */
    private fun doSetTorch(parameters: Camera.Parameters, isOpen: Boolean) {
        val flashMode = if (isOpen){
            findSettableValue(parameters.supportedFlashModes, Camera.Parameters.FLASH_MODE_TORCH, Camera.Parameters.FLASH_MODE_ON)
        } else {
            findSettableValue(parameters.supportedFlashModes, Camera.Parameters.FLASH_MODE_OFF)
        }
        if (flashMode.isNotEmpty()){
            parameters.flashMode = flashMode
        }
    }

    /** 处理预览大小 */
    private fun findBestPreviewSizeValue(parameters: Camera.Parameters, screenResolution: Point): Point {
        val supportedPreviewSizes: List<Camera.Size> = ArrayList(parameters.supportedPreviewSizes)
        Collections.sort(supportedPreviewSizes,
            Comparator { a, b ->
                val aPixels = a.height * a.width
                val bPixels = b.height * b.width
                if (bPixels < aPixels) {
                    return@Comparator -1
                }
                return@Comparator if (bPixels > aPixels) 1 else 0
            })
        var bestSize: Point? = null
        val screenAspectRatio = screenResolution.x.toFloat() / screenResolution.y.toFloat()
        for (previewSize in supportedPreviewSizes) {
            val realWidth = previewSize.width
            val realHeight = previewSize.height
            val pixels = realWidth * realHeight
            if (pixels < MIN_PREVIEW_PIXELS || pixels > MAX_PREVIEW_PIXELS) {
                continue
            }
            val isCandidatePortrait = realWidth < realHeight
            val maybeFlippedWidth = if (isCandidatePortrait) realHeight else realWidth
            val maybeFlippedHeight = if (isCandidatePortrait) realWidth else realHeight
            if (maybeFlippedWidth == screenResolution.x && maybeFlippedHeight == screenResolution.y) {
                return Point(realWidth, realHeight)
            }
            val aspectRatio = maybeFlippedWidth.toFloat() / maybeFlippedHeight.toFloat()
            val newDiff = abs(aspectRatio - screenAspectRatio)
            if (newDiff < Float.POSITIVE_INFINITY) {
                bestSize = Point(realWidth, realHeight)
            }
        }
        if (bestSize == null) {
            val defaultSize = parameters.previewSize
            bestSize = Point(defaultSize.width, defaultSize.height)
        }
        return bestSize
    }

    private fun findSettableValue(supportedValues: Collection<String>, vararg desiredValues: String): String {
        var result = ""
        for (value in desiredValues) {
            if (supportedValues.contains(value)) {
                result = value
            }
        }
        return result
    }
}