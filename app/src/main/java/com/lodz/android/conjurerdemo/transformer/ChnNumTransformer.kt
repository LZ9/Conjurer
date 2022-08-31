package com.lodz.android.conjurerdemo.transformer

import com.lodz.android.conjurer.transformer.OcrResultTransformer

/**
 * 中文大写数字金额数据转换器
 * @author zhouL
 * @date 2022/8/12
 */
class ChnNumTransformer : OcrResultTransformer {
    override fun onResultTransformer(text: String): String {
        //默认将每次识别数据拼接为一行
        return text.trim().replace(" ", "").replace("\n","")
    }
}