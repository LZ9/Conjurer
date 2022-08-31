package com.lodz.android.conjurerdemo.transformer

import com.lodz.android.conjurer.transformer.OcrResultTransformer
import com.lodz.android.conjurerdemo.getCarrier
import com.lodz.android.conjurerdemo.getGeo
import com.lodz.android.conjurerdemo.isPhoneNum
import com.lodz.android.corekt.anko.append
import com.lodz.android.corekt.anko.getListBySeparator
import com.lodz.android.corekt.anko.getStringBySeparator

/**
 * 手机号数据转换器
 * @author zhouL
 * @date 2022/8/12
 */
class PhoneTransformer : OcrResultTransformer {

    override fun onResultTransformer(text: String): String {
        val word = text.trim().replace(" ", "")
        val list = word.getListBySeparator("\n")
        val phoneList = ArrayList<String>()//只返回身份证格式校验成功的数据
        for (phone in list) {
            if (phone.length < 11) {
                continue
            }
            if (phone.length == 11 && phone.isPhoneNum()) {
                phoneList.add(getPhoneInfo(phone))
                continue
            }
            val offset = phone.length - 10
            for (i in 0 until offset) {
                val section = phone.substring(i, 11 + i)
                if (section.isPhoneNum()) {
                    phoneList.add(section)
                    break
                }
            }
        }
        return if (list.isEmpty()) "" else phoneList.getStringBySeparator("\n")
    }

    /** 补充手机号信息 */
    private fun getPhoneInfo(phone: String): String =
        phone.append(",")
            .append(phone.getCarrier())
            .append(",")
            .append(phone.getGeo())
}