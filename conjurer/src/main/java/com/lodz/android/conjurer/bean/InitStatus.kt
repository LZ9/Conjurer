package com.lodz.android.conjurer.bean

/**
 * 初始化状态
 * @author zhouL
 * @date 2022/8/9
 */
enum class InitStatus(val id: Int, val msg: String) {
    START(1, "开始初始化"),
    CHECK_LOCAL_TRAINED_DATA(2, "正在校验本地训练文件"),
    COMPLETE(3, "初始化完成"),
}