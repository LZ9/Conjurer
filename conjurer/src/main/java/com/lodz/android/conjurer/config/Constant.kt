package com.lodz.android.conjurer.config

/**
 * 常量
 * @author zhouL
 * @date 2022/8/4
 */
object Constant {

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

    /** 识别成功 */
    const val TYPE_EVENT_SUCCESS = 0
    /** 关闭页面 */
    const val TYPE_EVENT_FINISH = 1
    /** 相机打开失败 */
    const val TYPE_EVENT_ERROR_CAMERA_OPEN_FAIL = 2
    /** 训练数据存放目录创建失败 */
    const val TYPE_EVENT_ERROR_DIR_CREATE_FAIL = 3
    /** 训练文件安装失败 */
    const val TYPE_EVENT_ERROR_TRAINED_DATA_INSTALL_FAIL = 4
    /** OCR初始化失败 */
    const val TYPE_EVENT_ERROR_OCR_INIT_FAIL = 5
    /** 请求参数为空 */
    const val TYPE_EVENT_ERROR_REQUEST_PARAM_NULL = 6
    /** 无法创建Bitmap */
    const val TYPE_EVENT_ERROR_CAN_NOT_CREATE_BITMAP = 7


}