package com.lodz.android.conjurer.ocr

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lodz.android.conjurer.camera.CameraManager
import com.lodz.android.conjurer.camera.ShutterButton
import com.lodz.android.conjurer.config.Constant
import com.lodz.android.conjurer.data.bean.OcrRequestBean
import com.lodz.android.conjurer.data.event.OcrEvent
import com.lodz.android.conjurer.databinding.CjActivityCaptureBinding
import com.lodz.android.conjurer.ocr.recog.OcrRecognizeManager
import com.lodz.android.conjurer.ocr.recog.OnRecognizeListener
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

    private val mBinding: CjActivityCaptureBinding by lazy { CjActivityCaptureBinding.inflate(layoutInflater) }

    /** OCR请求数据体  */
    private var mRequestBean: OcrRequestBean? = null
    /** 预览页  */
    private var mSurfaceHolder: SurfaceHolder? = null
    /** 相机  */
    private var mCameraManager: CameraManager = CameraManager(getContext())
    /** 识别封装类  */
    private var mOcrRecognizeManager: OcrRecognizeManager = OcrRecognizeManager.create()

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

        mSurfaceHolder = mBinding.surfaceView.holder

        mBinding.shutterBtn.setOnShutterButtonListener(object :ShutterButton.OnShutterButtonListener{
            override fun onActionDownFocus(btn: ShutterButton, pressed: Boolean) {
                toastShort("onActionDownFocus")
            }

            override fun onActionUpClick(btn: ShutterButton) {
                toastShort("onActionUpClick")
            }
        })

        mBinding.previewTogbtn.setOnCheckedChangeListener { buttonView, isChecked ->
            toastShort("previewTogbtn isChecked : $isChecked")
        }

        mBinding.viewfinderView.setCameraManager(mCameraManager)
        mOcrRecognizeManager.init(mCameraManager, requestBean)
        mOcrRecognizeManager.setOnRecognizeListener(object :OnRecognizeListener{
            override fun onOcrDecode(cameraX: Int, cameraY: Int, data: ByteArray) {
                toastShort("onOcrDecode")
            }

            override fun onOcrDecodeSucceeded() {
                toastShort("onOcrDecodeSucceeded")
            }

            override fun onOcrDecodeFailed() {
                toastShort("onOcrDecodeFailed")
            }

            override fun onOcrContinuousDecode(cameraX: Int, cameraY: Int, data: ByteArray) {
                toastShort("onOcrContinuousDecode")
            }

            override fun onOcrContinuousDecodeSucceeded() {
                toastShort("onOcrContinuousDecodeSucceeded")
            }

            override fun onOcrContinuousDecodeFailed() {
                toastShort("onOcrContinuousDecodeFailed")
            }

            override fun onRestartPreview() {
                toastShort("onRestartPreview")
            }

            override fun onQuit() {
                toastShort("onQuit")
            }
        })
        showStandardUI()
    }

    private fun showStandardUI(){
        mBinding.resultLayout.visibility = View.GONE
        mBinding.resultTv.text = ""
        mBinding.previewLayout.visibility = View.GONE
        mBinding.previewStatusTv.text = ""
        mBinding.previewResultTv.text = ""
        mBinding.viewfinderView.visibility = View.VISIBLE
        mBinding.shutterBtn.visibility = View.VISIBLE
        mBinding.viewfinderView.removeResultText()
    }

    private fun showResultUI(){
        mBinding.resultLayout.visibility = View.VISIBLE
        mBinding.shutterBtn.visibility = View.GONE
        mBinding.previewLayout.visibility = View.GONE
        mBinding.viewfinderView.visibility = View.GONE
    }

    private fun initCamera(holder: SurfaceHolder){
        try {
            mCameraManager.openDriver(holder)
            mCameraManager.startPreview()
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
        mCameraManager.stopPreview()
        mCameraManager.closeDriver()
        mSurfaceHolder?.removeCallback(mSurfaceHolderCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        mOcrRecognizeManager.release()
    }

    override fun onBackPressed() {
        if (mBinding.resultLayout.visibility == View.VISIBLE) {
            showStandardUI()
            return
        }
        super.onBackPressed()
    }


    /** 发送失败事件 */
    private fun sendErrorEvent(type: Int, t: Throwable, msg: String) {
        EventBus.getDefault().post(OcrEvent(type, "", t, msg))
    }

    /** 发送成功事件 */
    private fun sendSuccessEvent(result: String) {
        EventBus.getDefault().post(OcrEvent(Constant.TYPE_EVENT_SUCCESS, result, null, "识别成功"))
    }

    /** 弹出提示语 */
    private fun toastShort(text: String) {
        Toast.makeText(getContext(), text, Toast.LENGTH_SHORT).show()
    }

    private fun getContext(): Context = this

}