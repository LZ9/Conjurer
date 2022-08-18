package com.lodz.android.conjurer.camera

import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.hardware.Camera
import android.view.SurfaceHolder
import android.view.WindowManager
import com.lodz.android.conjurer.widget.PlanarYUVLuminanceSource
import java.io.IOException
import java.util.*
import kotlin.Comparator
import kotlin.math.abs

/**
 * 相机帮助类
 * @author zhouL
 * @date 2022/8/16
 */
class CameraHelper {

    companion object{
        private const val MIN_FRAME_WIDTH = 50 // originally 240
        private const val MIN_FRAME_HEIGHT = 20 // originally 240
        private const val MAX_FRAME_WIDTH = 1920 // originally 480
        private const val MAX_FRAME_HEIGHT = 1080 // originally 360
        private const val MIN_PREVIEW_PIXELS = 470 * 320//最小预览分辨率
        private const val MAX_PREVIEW_PIXELS = 1920 * 1080//最大预览分辨率
    }

    /** 相机 */
    private var mCamera: Camera? = null
    /** 自动对焦 */
    private var mAutoFocusManager: AutoFocusManager? = null
    /**  */
    private var mFramingRect: Rect? = null
    /**  */
    private var mFramingRectInPreview: Rect? = null
    /** 是否初始化 */
    private var isInitialized = false
    /** 是否正在预览 */
    private var isPreviewing = false
    /**  */
    private var mRequestedFramingRectWidth = 0
    /**  */
    private var mRequestedFramingRectHeight = 0

    private var mScreenResolution: Point? = null
    private var mCameraResolution: Point? = null

    /** 开启相机 */
    @Throws(IOException::class)
    fun openDriver(context: Context, holder: SurfaceHolder) {
        var camera = mCamera
        if (camera == null) {
            camera = Camera.open()
            if (camera == null) {
                throw IOException("camera open fail")
            }
            mCamera = camera
        }
        camera.setPreviewDisplay(holder)
        if (!isInitialized){
            isInitialized = true
            initFromCameraParameters(context, camera)
            if (mRequestedFramingRectWidth > 0 && mRequestedFramingRectHeight > 0) {
                adjustFramingRect(mRequestedFramingRectWidth, mRequestedFramingRectHeight)
                mRequestedFramingRectWidth = 0
                mRequestedFramingRectHeight = 0
            }
        }
        setDesiredCameraParameters(camera)
    }

    /** 关闭相机 */
    fun closeDriver() {
        mCamera?.release()
        mCamera = null
        mFramingRect = null
        mFramingRectInPreview = null
    }

    /** 开始预览 */
    fun startPreview(){
        val camera = mCamera
        if (camera != null && !isPreviewing) {
            camera.startPreview()
            isPreviewing = true
            mAutoFocusManager = AutoFocusManager(camera)
        }
    }

    /** 结束预览 */
    fun stopPreview() {
        mAutoFocusManager?.stop()
        mAutoFocusManager = null
        mCamera?.stopPreview()
        isPreviewing = false
    }

    /** OCR解码 */
    fun requestOcrDecode(listener: OnCaptureListener) {
        val callback = CaptureCallback(getCameraResolution(), getScreenResolution(), listener)
        if (mCamera != null && isPreviewing) {
            mCamera?.setOneShotPreviewCallback(callback)
        }
    }

    /** 手动对焦对焦 */
    fun requestManualFocus(delay: Long) {
        mAutoFocusManager?.start(delay)
    }

    /** 自动对焦 */
    fun requestAutoFocus(){
        mAutoFocusManager?.start()
    }


    fun getFramingRect(): Rect? {
        if (mCamera == null){
            return null
        }
        if (mFramingRect == null){
            val screenResolution = getScreenResolution() ?: return null

            var width = screenResolution.x * 3 / 5
            if (width < MIN_FRAME_WIDTH) {
                width = MIN_FRAME_WIDTH
            } else if (width > MAX_FRAME_WIDTH) {
                width = MAX_FRAME_WIDTH
            }

            var height = screenResolution.y * 1 / 5
            if (height < MIN_FRAME_HEIGHT) {
                height = MIN_FRAME_HEIGHT
            } else if (height > MAX_FRAME_HEIGHT) {
                height = MAX_FRAME_HEIGHT
            }
            val leftOffset = (screenResolution.x - width) / 2
            val topOffset = (screenResolution.y - height) / 2
            mFramingRect = Rect(leftOffset, topOffset, leftOffset + width, topOffset + height)
        }
        return mFramingRect
    }

    fun getFramingRectInPreview(): Rect? {
        if (mFramingRectInPreview == null) {
            val rect = Rect(getFramingRect())
            val cameraResolution = getCameraResolution()
            val screenResolution = getScreenResolution()
            if (cameraResolution == null || screenResolution == null) {
                return null
            }
            rect.left = rect.left * cameraResolution.x / screenResolution.x
            rect.right = rect.right * cameraResolution.x / screenResolution.x
            rect.top = rect.top * cameraResolution.y / screenResolution.y
            rect.bottom = rect.bottom * cameraResolution.y / screenResolution.y
            mFramingRectInPreview = rect
        }
        return mFramingRectInPreview
    }

    fun adjustFramingRect(width: Int, height: Int) {
        if (!isInitialized) {
            mRequestedFramingRectWidth = width
            mRequestedFramingRectHeight = height
            return
        }
        val screenResolution = getScreenResolution()
        val rect = mFramingRect
        if (screenResolution == null || rect == null) {
            mRequestedFramingRectWidth = width
            mRequestedFramingRectHeight = height
            return
        }
        var deltaWidth = width
        var deltaHeight = height

        if ((rect.width() + deltaWidth > screenResolution.x - 4) || (rect.width() + deltaWidth < 50)) {
            deltaWidth = 0
        }
        if ((rect.height() + deltaHeight > screenResolution.y - 4) || (rect.height() + deltaHeight < 50)) {
            deltaHeight = 0
        }

        val newWidth = rect.width() + deltaWidth
        val newHeight = rect.height() + deltaHeight
        val leftOffset = (screenResolution.x - newWidth) / 2
        val topOffset = (screenResolution.y - newHeight) / 2
        mFramingRect = Rect(leftOffset, topOffset, leftOffset + newWidth, topOffset + newHeight)
        mFramingRectInPreview = null
    }

    fun buildLuminanceSource(width: Int, height: Int, data: ByteArray?): PlanarYUVLuminanceSource? {
        if (data == null) {
            return null
        }
        val rect = getFramingRectInPreview() ?: return  null
        return PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top, rect.width(), rect.height(), false)
    }

    /** 初始化相机参数 */
    fun initFromCameraParameters(context: Context, camera: Camera) {
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
        var diff = Float.POSITIVE_INFINITY
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
            if (newDiff < diff) {
                bestSize = Point(realWidth, realHeight)
                diff = newDiff
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