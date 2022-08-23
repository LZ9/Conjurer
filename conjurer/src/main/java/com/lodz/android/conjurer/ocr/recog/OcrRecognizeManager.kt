package com.lodz.android.conjurer.ocr.recog

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import com.googlecode.leptonica.android.ReadFile
import com.googlecode.tesseract.android.TessBaseAPI
import com.googlecode.tesseract.android.TessBaseAPI.PageIteratorLevel
import com.lodz.android.conjurer.camera.CameraHelper
import com.lodz.android.conjurer.data.bean.OcrRequestBean
import com.lodz.android.conjurer.data.bean.OcrResultBean
import com.lodz.android.conjurer.widget.PlanarYUVLuminanceSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * OCR识别管理类
 * @author zhouL
 * @date 2022/8/15
 */
class OcrRecognizeManager private constructor(){

    companion object {
        fun create() = OcrRecognizeManager()
    }

    /** OCR封装类 */
    private var mBaseApi: TessBaseAPI? = null
    /** 监听器 */
    private var mListener: OnRecognizeListener? = null
    /** 识别区域 */
    private var mRecogRect: Rect? = null

    /** 初始化 */
    fun init(requestBean: OcrRequestBean) {
        if (mBaseApi != null) {
            release()
        }
        mBaseApi = TessBaseAPI()
        mBaseApi?.init(requestBean.dataPath, requestBean.language, requestBean.engineMode)
        mBaseApi?.pageSegMode = requestBean.pageSegMode
        mBaseApi?.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, requestBean.blackList) //黑名单
        mBaseApi?.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, requestBean.whiteList) //白名单
    }

    /** 设置识别区域 */
    fun setRecogRect(rect: Rect?) {
        mRecogRect = rect
    }

    /** 设置监听器 */
    fun setOnRecognizeListener(listener: OnRecognizeListener?) {
        mListener = listener
    }

    /** OCR识别 */
    fun ocrCameraDecode(cameraHelper: CameraHelper?) {
        val rect = mRecogRect
        if (rect == null){
            mListener?.onOcrDecodeResult(OcrResultBean())
            return
        }
        cameraHelper?.requestOcrDecode { cameraResolution, screenResolution, data ->
            MainScope().launch {
                mListener?.onOcrDecodeStart()
                withContext(Dispatchers.IO) {
                    decode(createCameraGreyscaleBitmap(rect, cameraResolution, screenResolution, data))
                }
                mListener?.onOcrDecodeEnd()
            }
        }
    }

    /** 识别图片内文字 */
    private suspend fun decode(greyscaleBitmap: Bitmap?) {
        val bean = OcrResultBean()
        val startTime = System.currentTimeMillis()
        bean.bitmap = greyscaleBitmap
        try {
            mBaseApi?.setImage(ReadFile.readBitmap(bean.bitmap))
            bean.text = mBaseApi?.utF8Text ?: ""
            bean.recognitionTimeRequired = System.currentTimeMillis() - startTime
            bean.wordConfidences = mBaseApi?.wordConfidences()
            bean.meanConfidence = mBaseApi?.meanConfidence() ?: 0
            bean.regionBoundingBoxes = mBaseApi?.regions?.boxRects
            bean.textlineBoundingBoxes = mBaseApi?.textlines?.boxRects
            bean.wordBoundingBoxes = mBaseApi?.words?.boxRects
            bean.stripBoundingBoxes = mBaseApi?.strips?.boxRects

            val iterator = mBaseApi?.resultIterator
            if (iterator != null) {
                val charBoxes = ArrayList<Rect>()
                iterator.begin()
                do {
                    val lastBoundingBox = iterator.getBoundingBox(PageIteratorLevel.RIL_SYMBOL)
                    charBoxes.add(Rect(lastBoundingBox[0], lastBoundingBox[1],lastBoundingBox[2], lastBoundingBox[3]))
                } while (iterator.next(PageIteratorLevel.RIL_SYMBOL))
                iterator.delete()
                bean.characterBoundingBoxes = charBoxes
            }
        } catch (e: Exception) {
            e.printStackTrace()
            mBaseApi?.clear()
        }
        withContext(Dispatchers.Main) { mListener?.onOcrDecodeResult(bean) }
    }

    /** 生成灰度位图 */
    private fun createCameraGreyscaleBitmap(rect: Rect, cameraResolution: Point, screenResolution: Point, data: ByteArray?): Bitmap? {
        if (data == null) {
            return null
        }
        val frameRect = Rect(rect)
        frameRect.left = frameRect.left * cameraResolution.x / screenResolution.x
        frameRect.right = frameRect.right * cameraResolution.x / screenResolution.x
        frameRect.top = frameRect.top * cameraResolution.y / screenResolution.y
        frameRect.bottom = frameRect.bottom * cameraResolution.y / screenResolution.y
        return PlanarYUVLuminanceSource(data, cameraResolution.x, cameraResolution.y, frameRect.left, frameRect.top, frameRect.width(), frameRect.height(), false).renderCroppedGreyscaleBitmap()
    }

    fun release() {
        try {
            end()
            clear()
            stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mBaseApi = null
    }

    fun stop() {
        mBaseApi?.stop()
    }

    fun clear() {
        mBaseApi?.clear()
    }

    fun end() {
        mBaseApi?.end()
    }
}