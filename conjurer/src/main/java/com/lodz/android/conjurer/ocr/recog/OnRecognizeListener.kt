package com.lodz.android.conjurer.ocr.recog

import com.lodz.android.conjurer.data.bean.OcrResultBean

/**
 * 识别监听器
 * @author zhouL
 * @date 2022/8/15
 */
interface OnRecognizeListener {

    /** 开始解码 */
    fun onOcrDecodeStart()
    /** 返回识别结果 */
    fun onOcrDecodeResult(resultBean: OcrResultBean)
    /** 结束解码 */
    fun onOcrDecodeEnd()
}