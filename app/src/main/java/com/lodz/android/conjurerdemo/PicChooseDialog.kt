package com.lodz.android.conjurerdemo

import android.content.Context
import android.view.View
import com.lodz.android.conjurerdemo.databinding.DialogPicChooseBinding
import com.lodz.android.pandora.utils.viewbinding.bindingLayout
import com.lodz.android.pandora.widget.dialog.BaseBottomDialog

/**
 * 图片选择弹框
 * @author zhouL
 * @date 2022/8/25
 */
class PicChooseDialog(context: Context) : BaseBottomDialog(context) {

    private val mBinding : DialogPicChooseBinding by bindingLayout(DialogPicChooseBinding::inflate)

    override fun getViewBindingLayout(): View = mBinding.root

    override fun setListeners() {
        super.setListeners()
        mBinding.case1Btn.setOnClickListener {

        }

        mBinding.case2Btn.setOnClickListener {


        }

        mBinding
    }


}