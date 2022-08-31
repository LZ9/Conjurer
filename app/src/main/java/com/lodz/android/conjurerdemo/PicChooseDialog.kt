package com.lodz.android.conjurerdemo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.view.View
import com.lodz.android.conjurerdemo.databinding.DialogPicChooseBinding
import com.lodz.android.corekt.utils.BitmapUtils
import com.lodz.android.imageloaderkt.ImageLoader
import com.lodz.android.pandora.picker.file.PickerManager
import com.lodz.android.pandora.utils.viewbinding.bindingLayout
import com.lodz.android.pandora.widget.dialog.BaseBottomDialog

/**
 * 图片选择弹框
 * @author zhouL
 * @date 2022/8/25
 */
class PicChooseDialog(context: Context) : BaseBottomDialog(context) {

    private var mListener: OnChooseListener? = null

    private val mBinding : DialogPicChooseBinding by bindingLayout(DialogPicChooseBinding::inflate)

    override fun getViewBindingLayout(): View = mBinding.root

    override fun setListeners() {
        super.setListeners()
        mBinding.case1Btn.setOnClickListener {
            dismiss()
            mListener?.onPicChoose(
                BitmapFactory.decodeResource(context.resources, R.drawable.ic_sfzh_case1),
                context.getString(R.string.main_dialog_pic_case_1),
            )
        }

        mBinding.case2Btn.setOnClickListener {
            dismiss()
            mListener?.onPicChoose(
                BitmapFactory.decodeResource(context.resources, R.drawable.ic_sfzh_case2),
                context.getString(R.string.main_dialog_pic_case_2),
            )
        }

        mBinding.picAlbumBtn.setOnClickListener {
            PickerManager.pickPhoneAlbum()
                .setMaxCount(1)
                .setNeedPreview(false)
                .setNeedBottomInfo(false)
                .setNeedCamera(true, Environment.DIRECTORY_DCIM)
                .setAuthority(BuildConfig.FILE_AUTHORITY)
                .setImgLoader { context, source, imageView ->
                    ImageLoader.create(context)
                        .loadUri(source.documentFile.uri)
                        .setPlaceholder(com.lodz.android.pandora.R.drawable.pandora_ic_img)
                        .setError(com.lodz.android.pandora.R.drawable.pandora_ic_img)
                        .setCenterCrop()
                        .into(imageView)
                }
                .setOnFilePickerListener{
                    dismiss()
                    if (it.isEmpty()){
                        mListener?.onPicChoose(null, "")
                        return@setOnFilePickerListener
                    }
                    val dw = it[0]
                    val bitmap = BitmapUtils.uriToBitmap(context, dw.documentFile.uri)
                    mListener?.onPicChoose(bitmap, dw.fileName)
                }
                .open(context)
        }
    }

    fun setOnChooseListener(listener: OnChooseListener?) {
        mListener = listener
    }

    fun interface OnChooseListener {
        /** 图片[bitmap]选中回调，图片名称[picName] */
        fun onPicChoose(bitmap: Bitmap?, picName: String)
    }

}