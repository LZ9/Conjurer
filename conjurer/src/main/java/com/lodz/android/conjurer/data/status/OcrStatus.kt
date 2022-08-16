package com.lodz.android.conjurer.data.status


/**
 * OCR识别状态
 * @author zhouL
 * @date 2022/8/9
 */
enum class OcrStatus(val id: Int, val msg: String) {
    PREVIEW(1, "预览"),
    PREVIEW_PAUSED(2, "预览暂停"),
    CONTINUOUS(3, "实时预览识别"),
    CONTINUOUS_PAUSED(4, "实时预览识别暂停"),
    SUCCESS(5, "识别成功"),
    DONE(6, "识别完成"),
}