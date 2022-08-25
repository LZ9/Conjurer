package com.lodz.android.conjurerdemo

import com.lodz.android.conjurer.transformer.OcrResultTransformer
import com.lodz.android.conjurer.util.OcrUtils
import com.lodz.android.corekt.utils.IdCardUtils

/**
 * 身份证数据转换器
 * @author zhouL
 * @date 2022/8/12
 */
class SfzhTransformer : OcrResultTransformer {
    override fun onResultTransformer(text: String): String {
        val word = text.trim().replace(" ", "")
        val list = OcrUtils.getListBySeparator(word, "\n")
        val sfzList = ArrayList<String>()
        for (str in list) {
            if (str.length < 18) {
                continue
            }
            if (str.length == 18 && IdCardUtils.validateIdCard(str)) {
                sfzList.add(str)
                continue
            }
            val offset = str.length - 17
            for (i in 0 until offset) {
                val section = str.substring(i, 18 + i)
                if (IdCardUtils.validateIdCard(section)) {
                    sfzList.add(section)
                    break
                }
            }
        }
        return if (list.isEmpty()) "" else OcrUtils.getStringBySeparator(sfzList, "\n")
    }
}