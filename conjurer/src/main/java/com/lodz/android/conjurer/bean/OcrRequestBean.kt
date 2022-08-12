package com.lodz.android.conjurer.bean

import java.io.Serializable

/**
 * Ocr请求类
 * @author zhouL
 * @date 2022/8/11
 */
class OcrRequestBean(
    val dataPath: String,//训练数据路径
    val language: String,//识别语言
    val engineMode: Int,//识别引擎模式
    val pageSegMode: Int,//页面分段模式
    val blackList: String,//黑名单
    val whiteList: String,//白名单
) : Serializable