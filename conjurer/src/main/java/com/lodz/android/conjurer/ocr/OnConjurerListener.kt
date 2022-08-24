package com.lodz.android.conjurer.ocr

import com.lodz.android.conjurer.data.bean.OcrResultBean
import com.lodz.android.conjurer.data.status.InitStatus

/**
 * OCR接口回调
 * @author zhouL
 * @date 2022/8/9
 */
interface OnConjurerListener {

    /** 初始化回调 */
    fun onInit(status: InitStatus)

    /** 识别结果回调 */
    fun onOcrResult(bean: OcrResultBean)

    /** 异常回调 */
    fun onError(type: Int, t: Throwable, msg: String)
}