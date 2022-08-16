package com.lodz.android.conjurer.ocr.recog

/**
 * 识别监听器
 * @author zhouL
 * @date 2022/8/15
 */
interface OnRecognizeListener {

    fun onOcrDecode(cameraX: Int, cameraY: Int, data: ByteArray)

    fun onOcrDecodeSucceeded()

    fun onOcrDecodeFailed()

    fun onOcrContinuousDecode(cameraX: Int, cameraY: Int, data: ByteArray)

    fun onOcrContinuousDecodeSucceeded()

    fun onOcrContinuousDecodeFailed()

    fun onRestartPreview()

    fun onQuit()
}