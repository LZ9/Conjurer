package com.lodz.android.conjurer.ocr

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lodz.android.conjurer.R
import com.lodz.android.conjurer.camera.CameraHelper
import com.lodz.android.conjurer.camera.ShutterButton
import com.lodz.android.conjurer.config.Constant
import com.lodz.android.conjurer.data.bean.OcrRequestBean
import com.lodz.android.conjurer.data.bean.OcrResultBean
import com.lodz.android.conjurer.data.event.OcrEvent
import com.lodz.android.conjurer.databinding.CjActivityOcrCameraBinding
import com.lodz.android.conjurer.ocr.recog.OcrRecognizeManager
import com.lodz.android.conjurer.ocr.recog.OnRecognizeListener
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus

/**
 * OCR相机识别页面
 * @author zhouL
 * @date 2022/8/15
 */
class OcrCameraActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_OCR_REQUEST = "extra_ocr_request"

        fun start(context: Context, bean: OcrRequestBean) {
            val intent = Intent(context, OcrCameraActivity::class.java)
            intent.putExtra(EXTRA_OCR_REQUEST, bean)
            context.startActivity(intent)
        }
    }

    private val mBinding: CjActivityOcrCameraBinding by lazy { CjActivityOcrCameraBinding.inflate(layoutInflater) }

    /** OCR请求数据体  */
    private var mRequestBean: OcrRequestBean? = null
    /** 预览页  */
    private var mSurfaceHolder: SurfaceHolder? = null
    /** 相机  */
    private var mCameraHelper: CameraHelper = CameraHelper()
    /** 识别封装类  */
    private var mOcrRecognizeManager: OcrRecognizeManager = OcrRecognizeManager.create()
    /** 加载框  */
    private var mPgDialog: ProgressDialog? = null
    /** OCR识别结果  */
    private var mOcrResultBean: OcrResultBean? = null

    private val mSurfaceHolderCallback = object :SurfaceHolder.Callback{
        override fun surfaceCreated(holder: SurfaceHolder) {
            initCamera(holder)
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

        override fun surfaceDestroyed(holder: SurfaceHolder) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requestBean = intent.getSerializableExtra(EXTRA_OCR_REQUEST) as? OcrRequestBean
        mRequestBean = requestBean
        if (requestBean == null){
            sendErrorEvent(Constant.TYPE_EVENT_ERROR_REQUEST_PARAM_NULL, IllegalArgumentException("request bean is null"), "请求参数为空")
            finish()
            return
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS)

        setContentView(mBinding.root)

        initPgDialog()

        mSurfaceHolder = mBinding.surfaceView.holder

        mBinding.shutterBtn.setOnShutterButtonListener(object :ShutterButton.OnShutterButtonListener{
            override fun onActionDownFocus(btn: ShutterButton, pressed: Boolean) {
                mCameraHelper.requestAutoFocus()//相机聚焦
            }

            override fun onActionUpClick(btn: ShutterButton) {
                mOcrRecognizeManager.ocrCameraDecode(mCameraHelper)
            }
        })

        mBinding.confirmBtn.setOnClickListener {
            sendSuccessEvent(mOcrResultBean)
            finish()
        }

        mBinding.cancelBtn.setOnClickListener {
            showStandardUI()
        }

        mBinding.viewfinderLayout.setOnViewfinderChangeListener {
            mOcrRecognizeManager.setRecogRect(it)
        }
        mOcrRecognizeManager.init(requestBean)
        mOcrRecognizeManager.setOnRecognizeListener(object : OnRecognizeListener {
            override fun onOcrDecodeStart() {
                mPgDialog?.show()
            }

            override fun onOcrDecodeResult(resultBean: OcrResultBean) {
                if (resultBean.text.isEmpty()){
                    toastShort(getString(R.string.cj_app_ocr_decode_fail))
                    return
                }
                showResultUI(resultBean)
                mOcrResultBean = resultBean
            }

            override fun onOcrDecodeEnd() {
                mPgDialog?.dismiss()
            }
        })
        showStandardUI()
    }

    /** 初始化加载库 */
    private fun initPgDialog() {
        mPgDialog = ProgressDialog(getContext())
        mPgDialog?.setMessage(getString(R.string.cj_app_ocr_decoding))
        mPgDialog?.setCancelable(false)
        mPgDialog?.setCanceledOnTouchOutside(false)
    }

    /** 显示标准扫描UI */
    private fun showStandardUI() {
        mBinding.resultLayout.visibility = View.GONE
        mBinding.resultTv.text = ""
        mBinding.viewfinderLayout.visibility = View.VISIBLE
        mBinding.viewfinderLayout.drawViewfinder()
        mBinding.shutterBtn.visibility = View.VISIBLE
    }

    /** 显示结果UI */
    private fun showResultUI(bean: OcrResultBean) {
        mBinding.resultLayout.visibility = View.VISIBLE
        mBinding.shutterBtn.visibility = View.GONE
        mBinding.viewfinderLayout.visibility = View.GONE

        if (bean.text.isEmpty()){
            toastShort(getString(R.string.cj_app_ocr_decode_fail))
        }
        val bitmap = bean.getAnnotatedBitmap(this, R.color.cj_color_00ccff)
        if (bitmap == null) {
            mBinding.resultImg.visibility = View.GONE
        } else {
            mBinding.resultImg.visibility = View.VISIBLE
            mBinding.resultImg.setImageBitmap(bitmap)
        }
        var text = bean.text
        val requestBean = mRequestBean
        if (requestBean != null) {
            for (transformer in requestBean.transformerList) {
                text = transformer.onResultTransformer(text)
            }
        }
        bean.text = text
        mBinding.resultTv.text = text
    }

    /** 初始化相机 */
    private fun initCamera(holder: SurfaceHolder){
        try {
            mCameraHelper.openDriver(getContext(), holder)
            mCameraHelper.startPreview()
        } catch (e: Exception) {
            e.printStackTrace()
            sendErrorEvent(Constant.TYPE_EVENT_ERROR_CAMERA_OPEN_FAIL, e, "相机启动失败")
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        mSurfaceHolder?.addCallback(mSurfaceHolderCallback)
        mSurfaceHolder?.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    override fun onPause() {
        super.onPause()
        mCameraHelper.stopPreview()
        mCameraHelper.closeDriver()
        mSurfaceHolder?.removeCallback(mSurfaceHolderCallback)
    }

    override fun onDestroy() {
        mOcrRecognizeManager.release()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (mBinding.resultLayout.visibility == View.VISIBLE) {
            showStandardUI()
            return
        }
        sendFinishEvent()
        super.onBackPressed()
    }

    /** 发送失败事件 */
    private fun sendErrorEvent(type: Int, t: Throwable, msg: String) {
        EventBus.getDefault().post(OcrEvent(type, null, t, msg))
    }

    /** 发送成功事件 */
    private fun sendSuccessEvent(bean: OcrResultBean?) {
        EventBus.getDefault().post(OcrEvent(Constant.TYPE_EVENT_SUCCESS, bean, null, "识别成功"))
    }

    /** 发送页面关闭事件 */
    private fun sendFinishEvent() {
        EventBus.getDefault().post(OcrEvent(Constant.TYPE_EVENT_FINISH, null, null, "取消识别"))
    }

    /** 弹出提示语 */
    private fun toastShort(text: String) {
        Toast.makeText(getContext(), text, Toast.LENGTH_SHORT).show()
    }

    private fun getContext(): Context = this

}