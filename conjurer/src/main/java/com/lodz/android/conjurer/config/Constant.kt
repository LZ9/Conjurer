package com.lodz.android.conjurer.config

/**
 * @author zhouL
 * @date 2022/8/4
 */
object Constant {

    const val CJ_RESTART_PREVIEW = 1001

    const val CJ_QUIT = 1002

    const val CJ_OCR_CONTINUOUS_DECODE_FAILED = 1003

    const val CJ_OCR_DECODE_SUCCEEDED = 1004

    const val CJ_OCR_DECODE_FAILED = 1005

    const val CJ_OCR_DECODE = 1006

    const val CJ_OCR_CONTINUOUS_DECODE_SUCCEEDED = 1007

    const val CJ_OCR_CONTINUOUS_DECODE = 1008


    /** 默认识别的语言 */
    const val DEFAULT_LANGUAGE = "eng"
    /** 默认英文训练数据 */
    const val DEFAULT_ENG_TRAINEDDATA = "eng.traineddata.zip"
    /** 默认osd训练数据 */
    const val DEFAULT_OSD_TRAINEDDATA = "osd.traineddata.zip"
    /** 默认训练数据的存放文件夹名称 */
    const val DEFAULT_TRAINEDDATA_DIR_NAME = "tessdata"
    /** 默认识别黑名单 */
    const val DEFAULT_BLACKLIST = "!@#$%^&*()_+=-[]}{;:'\"\\|~`,./<>?"
    /** 默认识别白名单 */
    const val DEFAULT_WHITELIST = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
}