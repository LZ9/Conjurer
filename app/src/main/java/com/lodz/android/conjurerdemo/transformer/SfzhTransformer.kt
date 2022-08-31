package com.lodz.android.conjurerdemo.transformer

import com.lodz.android.conjurer.transformer.OcrResultTransformer
import com.lodz.android.corekt.anko.getListBySeparator
import com.lodz.android.corekt.anko.getStringBySeparator
import com.lodz.android.corekt.utils.IdCardUtils

/**
 * 身份证数据转换器
 * @author zhouL
 * @date 2022/8/12
 */
class SfzhTransformer : OcrResultTransformer {
    override fun onResultTransformer(text: String): String {
        val word = text.trim().replace(" ", "")
        val list = word.getListBySeparator("\n")
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
        return if (list.isEmpty()) "" else sfzList.getStringBySeparator("\n")
    }
}