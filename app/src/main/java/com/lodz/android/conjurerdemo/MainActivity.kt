package com.lodz.android.conjurerdemo

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import com.googlecode.tesseract.android.TessBaseAPI
import com.lodz.android.conjurer.data.status.InitStatus
import com.lodz.android.conjurer.config.Constant
import com.lodz.android.conjurer.data.bean.OcrResultBean
import com.lodz.android.conjurer.ocr.Conjurer
import com.lodz.android.conjurer.ocr.OnConjurerListener
import com.lodz.android.conjurerdemo.databinding.ActivityMainBinding
import com.lodz.android.corekt.anko.append
import com.lodz.android.corekt.anko.goAppDetailSetting
import com.lodz.android.corekt.anko.isPermissionGranted
import com.lodz.android.corekt.anko.toastShort
import com.lodz.android.corekt.file.DocumentWrapper
import com.lodz.android.corekt.utils.BitmapUtils
import com.lodz.android.imageloaderkt.ImageLoader
import com.lodz.android.pandora.base.activity.BaseActivity
import com.lodz.android.pandora.picker.file.PickerManager
import com.lodz.android.pandora.utils.viewbinding.bindingLayout
import permissions.dispatcher.PermissionRequest
import permissions.dispatcher.ktx.constructPermissionsRequest

class MainActivity : BaseActivity() {

    private val mBinding: ActivityMainBinding by bindingLayout(ActivityMainBinding::inflate)

    private val hasCameraPermissions by lazy {
        constructPermissionsRequest(
            Manifest.permission.CAMERA,// 相机
            onShowRationale = ::onShowRationaleBeforeRequest,
            onPermissionDenied = ::onDenied,
            onNeverAskAgain = ::onNeverAskAgain,
            requiresPermission = ::onRequestPermission
        )
    }

    private val hasWriteExternalStoragePermissions by lazy {
        constructPermissionsRequest(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,// 存储卡读写
            onShowRationale = ::onShowRationaleBeforeRequest,
            onPermissionDenied = ::onDenied,
            onNeverAskAgain = ::onNeverAskAgain,
            requiresPermission = ::onRequestPermission
        )
    }

    private val hasReadExternalStoragePermissions by lazy {
        constructPermissionsRequest(
            Manifest.permission.READ_EXTERNAL_STORAGE,// 存储卡读写
            onShowRationale = ::onShowRationaleBeforeRequest,
            onPermissionDenied = ::onDenied,
            onNeverAskAgain = ::onNeverAskAgain,
            requiresPermission = ::onRequestPermission
        )
    }

    override fun getViewBindingLayout(): View = mBinding.root

    override fun findViews(savedInstanceState: Bundle?) {
        super.findViews(savedInstanceState)
        getTitleBarLayout().needBackButton(false)
        getTitleBarLayout().setTitleName(R.string.app_name)
    }

    override fun setListeners() {
        super.setListeners()

        mBinding.scanSfzBtn.setOnClickListener {
            Conjurer.create()
                .setLanguage(Constant.DEFAULT_LANGUAGE)
                .setEngineMode(TessBaseAPI.OEM_TESSERACT_ONLY)
                .setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_OSD)
                .setBlackList("")
                .setWhiteList("Xx0123456789")
                .addOcrResultTransformer(SfzhTransformer())
                .setOnConjurerListener(object : OnConjurerListener {
                    override fun onInit(status: InitStatus) {
                        addLog("${Thread.currentThread().name} onInit : ${status.msg}")
                    }

                    override fun onOcrResult(bean: OcrResultBean) {
                        addLog("${Thread.currentThread().name} onOcrResult : ${bean.text}")
                    }

                    override fun onError(type: Int, t: Throwable, msg: String) {
                        addLog("${Thread.currentThread().name} onError : $type , ${t.message} , $msg")
                    }
                })
                .openCamera(getContext())
        }

        mBinding.asynRecogBtn.setOnClickListener {
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
                    if (it.isEmpty()){
                        addLog("未选择图片")
                        return@setOnFilePickerListener
                    }
                    val dw = it[0]
                    addLog("选择图片：".append(dw.fileName))
                    OcrRecog(dw)
                }
                .open(getContext())
        }

        mBinding.cleanDataBtn.setOnClickListener {
            Conjurer.create().deleteTessdataDir(getContext())
            toastShort(R.string.main_clean_trained_data_ok)
        }

        mBinding.cleanLogBtn.setOnClickListener {
            mBinding.resultTv.text = ""
        }
    }

    /** OCR图片识别 */
    private fun OcrRecog(dw: DocumentWrapper) {
        val bitmap = BitmapUtils.uriToBitmap(getContext(), dw.documentFile.uri)
        if (bitmap == null){
            addLog("Uri转Bitmap失败")
            return
        }
        Conjurer.create()
            .setLanguage(Constant.DEFAULT_LANGUAGE)
            .setEngineMode(TessBaseAPI.OEM_TESSERACT_ONLY)
            .setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_OSD)
            .setBlackList("")
            .setWhiteList("Xx0123456789")
            .addOcrResultTransformer(SfzhTransformer())
            .setOnConjurerListener(object : OnConjurerListener {
                override fun onInit(status: InitStatus) {
                    addLog("${Thread.currentThread().name} onInit : ${status.msg}")
                }
                override fun onOcrResult(bean: OcrResultBean) {
                    addLog("${Thread.currentThread().name} onOcrResult : ${bean.text}")
                }
                override fun onError(type: Int, t: Throwable, msg: String) {
                    addLog("${Thread.currentThread().name} onError : $type , ${t.message} , $msg")
                }
            })
            .recogAsync(getContext(), bitmap)

    }

    private fun addLog(log: String) {
        mBinding.resultTv.text = log.append("\n").append(mBinding.resultTv.text)
    }

    override fun initData() {
        super.initData()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {// 6.0以上的手机对权限进行动态申请
            onRequestPermission()//申请权限
        } else {
            init()
        }
    }

    private fun init() {
        mBinding.dataPathTv.text = getString(R.string.main_data_path, Conjurer.create().getDefaultDataPath(getContext()))
        showStatusCompleted()
    }

    /** 权限申请成功 */
    private fun onRequestPermission() {
        if (!isPermissionGranted(Manifest.permission.CAMERA)) {
            hasCameraPermissions.launch()
            return
        }
        if (!isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            hasWriteExternalStoragePermissions.launch()
            return
        }
        if (!isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)){
            hasReadExternalStoragePermissions.launch()
            return
        }
        init()
    }

    /** 用户拒绝后再次申请前告知用户为什么需要该权限 */
    private fun onShowRationaleBeforeRequest(request: PermissionRequest) {
        request.proceed()//请求权限
    }

    /** 被拒绝 */
    private fun onDenied() {
        onRequestPermission()//申请权限
    }

    /** 被拒绝并且勾选了不再提醒 */
    private fun onNeverAskAgain() {
        toastShort(R.string.main_check_permission_tips)
        goAppDetailSetting()
        showStatusError()
    }
}