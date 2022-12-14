package com.lodz.android.conjurer.data.status

/**
 * 初始化状态
 * @author zhouL
 * @date 2022/8/9
 */
enum class InitStatus(val id: Int, val msg: String) {
    START(1, "开始初始化"),
    CHECK_LOCAL_TRAINED_DATA(2, "正在校验本地训练文件"),
    CHECK_LOCAL_TRAINED_DATA_SUCCESS(3, "校验本地训练文件完成"),
    COMPLETE(4, "初始化完成"),
}