package com.lodz.android.conjurer.transformer

import java.io.Serializable


/**
 * 识别结果转换器
 * @author zhouL
 * @date 2022/8/12
 */
interface OcrResultTransformer : Serializable {
    fun onResultTransformer(text: String): String
}