package com.lodz.android.conjurerdemo

import com.lodz.android.pandora.base.application.BaseApplication

/**
 * @author zhouL
 * @date 2022/8/4
 */
class App :BaseApplication(){


    override fun onStartCreate() {
        getBaseLayoutConfig().getTitleBarLayoutConfig().backgroundColor = R.color.color_9bd3ec
    }

    override fun onExit() {

    }
}