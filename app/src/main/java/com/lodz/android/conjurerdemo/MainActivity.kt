package com.lodz.android.conjurerdemo

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import com.googlecode.tesseract.android.TessBaseAPI
import com.lodz.android.conjurer.bean.InitStatus
import com.lodz.android.conjurer.config.Constant
import com.lodz.android.conjurer.ocr.CaptureActivity
import com.lodz.android.conjurer.ocr.Conjurer.Companion.create
import com.lodz.android.conjurer.ocr.OnConjurerListener
import com.lodz.android.conjurerdemo.databinding.ActivityMainBinding
import com.lodz.android.corekt.anko.append
import com.lodz.android.corekt.anko.goAppDetailSetting
import com.lodz.android.corekt.anko.isPermissionGranted
import com.lodz.android.corekt.anko.toastShort
import com.lodz.android.corekt.utils.DateUtils
import com.lodz.android.pandora.base.activity.BaseActivity
import com.lodz.android.pandora.utils.viewbinding.bindingLayout
import permissions.dispatcher.PermissionRequest
import permissions.dispatcher.ktx.constructPermissionsRequest

class MainActivity : BaseActivity() {

    private val mBinding: ActivityMainBinding by bindingLayout(ActivityMainBinding::inflate)

    private val hasPermissions by lazy {
        constructPermissionsRequest(
            Manifest.permission.CAMERA,// 相机
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

        mBinding.scanBtn.setOnClickListener {

            create()
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
                    override fun onError(t: Throwable, msg: String) {
                        addLog("${Thread.currentThread().name} error : ${t.message} , $msg")
                    }
                })
                .openCamera(this)
        }
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
        showStatusCompleted()
    }

    /** 权限申请成功 */
    private fun onRequestPermission() {
        if (!isPermissionGranted(Manifest.permission.CAMERA)) {
            hasPermissions.launch()
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