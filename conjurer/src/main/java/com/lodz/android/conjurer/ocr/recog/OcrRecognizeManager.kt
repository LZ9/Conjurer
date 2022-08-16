package com.lodz.android.conjurer.ocr.recog

import android.graphics.Rect
import com.googlecode.leptonica.android.ReadFile
import com.googlecode.tesseract.android.TessBaseAPI
import com.googlecode.tesseract.android.TessBaseAPI.PageIteratorLevel
import com.lodz.android.conjurer.camera.CameraHelper
import com.lodz.android.conjurer.data.bean.OcrRequestBean
import com.lodz.android.conjurer.data.bean.OcrResultBean
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

    private var mCameraHelper: CameraHelper? = null
    /** OCR封装类 */
    private var mBaseApi: TessBaseAPI? = null
    /** 监听器 */
    private var mListener: OnRecognizeListener? = null

    /** 初始化 */
    fun init(cameraHelper: CameraHelper, requestBean: OcrRequestBean) {
        mCameraHelper = cameraHelper
        if (mBaseApi != null) {
            release()
        }
        mBaseApi = TessBaseAPI()
        mBaseApi?.init(requestBean.dataPath, requestBean.language, requestBean.engineMode)
        mBaseApi?.pageSegMode = requestBean.pageSegMode
        mBaseApi?.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, requestBean.blackList) //黑名单
        mBaseApi?.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, requestBean.whiteList) //白名单
    }

    /** 设置监听器 */
    fun setOnRecognizeListener(listener: OnRecognizeListener?) {
        mListener = listener
    }

    fun ocrDecode() {
        mCameraHelper?.requestOcrDecode { width, height, data ->
            MainScope().launch {
                mListener?.onOcrDecodeStart()
                withContext(Dispatchers.IO) {
                    decode(width, height, data)
                }
                mListener?.onOcrDecodeEnd()
            }
        }
    }

    private suspend fun decode(width: Int, height: Int, data: ByteArray?) {
        val bean = OcrResultBean()
        val startTime = System.currentTimeMillis()
        bean.bitmap = mCameraHelper?.buildLuminanceSource(width, height, data)?.renderCroppedGreyscaleBitmap()
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

    fun release() {
        mBaseApi?.stop()
        mBaseApi?.clear()
        mBaseApi?.end()
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