package com.lodz.android.conjurer.data.event

import com.lodz.android.conjurer.config.Constant
import com.lodz.android.conjurer.data.bean.OcrResultBean

/**
 * OCR识别事件
 * @author zhouL
 * @date 2022/8/15
 */
open class OcrEvent(
    val type: Int,//事件类型
    val bean: OcrResultBean?,//识别结果
    val t: Throwable?,//异常
    val msg: String//提示语
) {
    fun isSuccess() = type == Constant.TYPE_EVENT_SUCCESS

    fun isFinish() = type == Constant.TYPE_EVENT_FINISH
}