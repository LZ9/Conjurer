package com.lodz.android.conjurerdemo.transformer

import com.lodz.android.conjurer.transformer.OcrResultTransformer

/**
 * 身份证数据转换器
 * @author zhouL
 * @date 2022/8/12
 */
class ChnNumTransformer : OcrResultTransformer {
    override fun onResultTransformer(text: String): String {
        return text.trim().replace(" ", "").replace("\n","")
    }
}