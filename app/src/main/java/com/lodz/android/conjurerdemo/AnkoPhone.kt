package com.lodz.android.conjurerdemo

import com.google.i18n.phonenumbers.PhoneNumberToCarrierMapper
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder
import java.util.*
import java.util.regex.Pattern

/**
 * 手机号扩展类
 * @author zhouL
 * @date 2022/8/31
 */
class AnkoPhone {
    companion object {

        /** 手机号正则 */
        const val REGEX_PHONE = "^1[3-9]\\d{9}\$"

        /** 中国区域缩写 */
        const val CHINA_REGION: String = "CN"

        /** 中国国家代码 */
        const val CHINA_COUNTRY_CODE = 86
    }
}

/** 获取手机运营商，国家代码[countryCode]（默认是中国的86） */
fun String.getCarrier(countryCode: Int = AnkoPhone.CHINA_COUNTRY_CODE): String {
    if (this.length != 11) {
        return ""
    }
    try {
        val pnu = PhoneNumberUtil.getInstance().parse(this, AnkoPhone.CHINA_REGION)
        val carrierEn = PhoneNumberToCarrierMapper.getInstance().getNameForNumber(pnu, Locale.ENGLISH)
        if (countryCode == AnkoPhone.CHINA_COUNTRY_CODE && Locale.CHINA.country.equals(Locale.getDefault().country)){
            if (carrierEn.contains("Mobile")){
                return "中国移动"
            }
            if (carrierEn.contains("Unicom")){
                return "中国联通"
            }
            if (carrierEn.contains("Telecom")){
                return "中国电信"
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return ""
}

/** 获取手机归属地 */
fun String.getGeo(): String {
    if (this.length != 11) {
        return ""
    }
    try {
        val pnu = PhoneNumberUtil.getInstance().parse(this, AnkoPhone.CHINA_REGION)
        return PhoneNumberOfflineGeocoder.getInstance().getDescriptionForNumber(pnu, Locale.CHINA)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return ""
}

/** 正则[regex]校验手机号 */
fun String.isPhoneNum(regex: String = AnkoPhone.REGEX_PHONE): Boolean = Pattern.matches(regex, this)
