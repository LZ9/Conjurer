package com.lodz.android.conjurer.ocr.recog

import com.googlecode.tesseract.android.TessBaseAPI
import com.lodz.android.conjurer.data.bean.OcrRequestBean
import com.lodz.android.conjurer.camera.CameraManager

/**
 * OCR识别管理类
 * @author zhouL
 * @date 2022/8/15
 */
class OcrRecognizeManager private constructor(){

    companion object {
        fun create() = OcrRecognizeManager()
    }


    private var mCameraManager: CameraManager? = null
    /** OCR封装类 */
    private var mBaseApi: TessBaseAPI? = null
    /** 监听器 */
    private var mListener: OnRecognizeListener? = null

    /** 初始化 */
    fun init(cameraManager: CameraManager, requestBean: OcrRequestBean) {
        mCameraManager = cameraManager
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

    fun ocrDecode(isContinuousModeActive: Boolean) {
//        mCameraManager?.requestOcrDecode(isContinuousModeActive, mListener)
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