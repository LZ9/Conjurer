package com.lodz.android.conjurer.ocr

import com.lodz.android.conjurer.bean.InitStatus

/**
 * OCR接口回调
 * @author zhouL
 * @date 2022/8/9
 */
interface OnConjurerListener {

    fun onInit(status: InitStatus)


    fun onError(t: Throwable, msg: String)
}