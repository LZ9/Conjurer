package com.lodz.android.conjurer.ocr;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatToggleButton;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.lodz.android.conjurer.R;
import com.lodz.android.conjurer.bean.OcrRequestBean;
import com.lodz.android.conjurer.bean.OcrResultBean;
import com.lodz.android.conjurer.camera.CameraManager;
import com.lodz.android.conjurer.camera.ShutterButton;
import com.lodz.android.conjurer.config.Constant;
import com.lodz.android.conjurer.transformer.OcrResultTransformer;

import java.io.IOException;


/**
 * This activity opens the camera and does the actual scanning on a background thread. It draws a
 * viewfinder to help the user place the text correctly, shows feedback as the image processing
 * is happening, and then overlays the results when a scan is successful.
 *
 * The code for this class was adapted from the ZXing project: http://code.google.com/p/zxing/
 */
public final class CaptureActivity extends Activity {

    private static final String TAG = CaptureActivity.class.getSimpleName();

    private static final long CAMERA_FOCUS_DELAY = 100L;

    public static final String EXTRA_OCR_REQUEST = "extra_ocr_request";

    public static void start(Context context, OcrRequestBean bean) {
        Intent starter = new Intent(context, CaptureActivity.class);
        starter.putExtra(EXTRA_OCR_REQUEST, bean);
        context.startActivity(starter);
    }

    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private boolean isContinuousModeActive = false; // Whether we are doing OCR in continuous mode
    private boolean isPaused;
    private ProgressDialog indeterminateDialog; // also for initOcr - init OCR engine


    public Handler getHandler() {
        return handler;
    }

    TessBaseAPI getBaseApi() {
        return mBaseApi;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    /** OCR请求数据体 */
    private OcrRequestBean mRequestBean;
    /** 识别结果数据体 */
    private OcrResultBean mResultBean;

    /** 相机预览 */
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    /** 扫描框 */
    private ViewfinderView mViewfinderView;
    /** 结果展示布局 */
    private ViewGroup mResultLayout;
    /** 识别结果图 */
    private ImageView mResultImg;
    /** 识别文字 */
    private TextView mResultTv;

    /** 实时预览布局 */
    private ViewGroup mPreviewLayout;
    /** 实时预览识别文字 */
    private TextView mPreviewResultTv;
    /** 实时预览状态 */
    private TextView mPreviewStatusTv;

    /** 拍照按钮 */
    private ShutterButton mShutterButton;
    /** 实时预览开关 */
    private AppCompatToggleButton mToggleBtn;

    /** OCR封装类 */
    private TessBaseAPI mBaseApi;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRequestBean = (OcrRequestBean) getIntent().getSerializableExtra(EXTRA_OCR_REQUEST);
        if (mRequestBean == null){
            finish();
            return;
        }

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.activity_capture);
        mSurfaceView = findViewById(R.id.surface_view);
        mSurfaceHolder = mSurfaceView.getHolder();

        mViewfinderView = findViewById(R.id.viewfinder_view);
        mResultLayout = findViewById(R.id.result_layout);
        mResultImg = findViewById(R.id.image_view);
        mResultTv = findViewById(R.id.result_tv);

        mPreviewLayout = findViewById(R.id.preview_layout);
        mPreviewResultTv = findViewById(R.id.preview_result_tv);
        mPreviewStatusTv = findViewById(R.id.preview_status_tv);

        mShutterButton = findViewById(R.id.shutter_button);
        mShutterButton.setOnShutterButtonListener(new ShutterButton.OnShutterButtonListener() {
            @Override
            public void onActionDownFocus(@NonNull ShutterButton btn, boolean pressed) {
                requestDelayedAutoFocus();
            }

            @Override
            public void onActionUpClick(@NonNull ShutterButton btn) {
                if (isContinuousModeActive) {
                    onShutterButtonPressContinuous();
                } else {
                    if (handler != null) {
                        handler.shutterButtonClick();
                    }
                }
            }
        });

        //实时预览
        mToggleBtn = findViewById(R.id.preview_togbtn);
        mToggleBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isContinuousModeActive = isChecked;
                mPreviewLayout.setVisibility(isContinuousModeActive ? View.VISIBLE : View.GONE);
                resumeOCR();
            }
        });

        mBaseApi = new TessBaseAPI();
        cameraManager = new CameraManager(getApplication());
        mViewfinderView.setCameraManager(cameraManager);
        handler = null;
        mResultBean = null;
    }


    private final SurfaceHolder.Callback mCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(@NonNull SurfaceHolder holder) {
            initCamera(holder);
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            mSurfaceHolder.removeCallback(mCallback);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        showStandardUI();
        mSurfaceHolder.addCallback(mCallback);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        resumeOCR();
    }

    /**
     * Method to start or restart recognition after the OCR engine has been initialized,
     * or after the app regains focus. Sets state related settings and OCR engine parameters,
     * and requests camera initialization.
     */
    public void resumeOCR() {
        Log.d(TAG, "resumeOCR()");

        // This method is called when Tesseract has already been successfully initialized, so set
        // isEngineReady = true here.

        isPaused = false;

        if (handler != null) {
            handler.resetState();
        }
        if (mBaseApi != null) {
            mBaseApi.init(mRequestBean.getDataPath(), mRequestBean.getLanguage(), mRequestBean.getEngineMode());
            mBaseApi.setPageSegMode(mRequestBean.getPageSegMode());
            mBaseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, mRequestBean.getBlackList());//黑名单
            mBaseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, mRequestBean.getWhiteList());//白名单
        }

    }

    /** Called when the shutter button is pressed in continuous mode. */
    void onShutterButtonPressContinuous() {
        isPaused = true;
        handler.stop();
        if (mResultBean != null) {
            handleOcrDecode(mResultBean);
        } else {
            Toast toast = Toast.makeText(this, "OCR failed. Please try again.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP, 0, 0);
            toast.show();
            resumeContinuousDecoding();
        }
    }

    /** Called to resume recognition after translation in continuous mode. */
    @SuppressWarnings("unused")
    void resumeContinuousDecoding() {
        isPaused = false;
        showStandardUI();
        setStatusViewForContinuous();
        DecodeHandler.resetDecodeState();
        handler.resetState();
        mShutterButton.setVisibility(View.VISIBLE);
    }


    /** Initializes the camera and starts the handler to begin previewing. */
    private void initCamera(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "initCamera()");
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        try {

            // Open and initialize the camera
            cameraManager.openDriver(surfaceHolder);

            // Creating the handler starts the preview, which can also throw a RuntimeException.
            handler = new CaptureActivityHandler(this, cameraManager, isContinuousModeActive);

        } catch (IOException ioe) {
            showErrorMessage("Error", "Could not initialize camera. Please try restarting device.");
        }
    }

    @Override
    protected void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
        }

        // Stop using the camera, to avoid conflicting with other camera-based apps
        cameraManager.closeDriver();
        mSurfaceHolder.removeCallback(mCallback);
        super.onPause();
    }

    public void stopHandler() {
        if (handler != null) {
            handler.stop();
        }
    }

    @Override
    protected void onDestroy() {
        if (mBaseApi != null) {
            mBaseApi.end();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // First check if we're paused in continuous mode, and if so, just unpause.
        if (isPaused) {
            Log.d(TAG, "only resuming continuous recognition, not quitting...");
            resumeContinuousDecoding();
            return;
        }
        if (mResultLayout.getVisibility() == View.VISIBLE){
            showStandardUI();
            return;
        }

        // Exit the app if we're not viewing an OCR result.
        if (mResultBean == null) {
            finish();
            return;
        }
        showStandardUI();
        if (handler != null) {
            handler.sendEmptyMessage(Constant.CJ_RESTART_PREVIEW);
        }
        super.onBackPressed();
    }


    /**
     * Displays information relating to the result of OCR, and requests a translation if necessary.
     *
     * @param bean Object representing successful OCR results
     * @return True if a non-null result was received for OCR
     */
    void handleOcrDecode(OcrResultBean bean) {
        // Test whether the result is null
        if (TextUtils.isEmpty(bean.text)) {
            Toast toast = Toast.makeText(this, "OCR failed. Please try again.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP, 0, 0);
            toast.show();
        }
        mResultBean = bean;
        showResultUI(bean);
        Bitmap bitmap = bean.getAnnotatedBitmap(this, R.color.cj_color_00ccff);
        if (bitmap == null) {
            mResultImg.setVisibility(View.GONE);
        } else {
            mResultImg.setVisibility(View.VISIBLE);
            mResultImg.setImageBitmap(bitmap);
        }
        String text = bean.text;
        for (OcrResultTransformer transformer : mRequestBean.getTransformerList()) {
            text = transformer.onResultTransformer(text);
        }
        mResultTv.setText(text);
    }

    /**
     * Displays information relating to the results of a successful real-time OCR request.
     *
     * @param bean Object representing successful OCR results
     */
    void handleOcrContinuousDecode(OcrResultBean bean) {

        mResultBean = bean;

        // Send an OcrResultText object to the ViewfinderView for text rendering
        mViewfinderView.setResultText(bean);


        // 显示实时扫描文本结果
        mPreviewResultTv.setText(bean.text);

        // 显示实时扫描统计信息
        mPreviewStatusTv.setText(
                "OCR: " + mRequestBean.getLanguage() +
                " - Mean confidence: " + bean.meanConfidence +
                " - Time required: " + bean.recognitionTimeRequired + " ms");
    }

    /**
     * Version of handleOcrContinuousDecode for failed OCR requests. Displays a failure message.
     *
     * @param obj Metadata for the failed OCR request.
     */
    void handleOcrContinuousDecodeFail(OcrResultBean obj) {
        mResultBean = null;
        mViewfinderView.removeResultText();

        // Reset the text in the recognized text box.
        mPreviewResultTv.setText("");

        // 将'-'号内的文本设置为红色
        CharSequence cs = setSpanBetweenTokens("OCR: " + mRequestBean.getLanguage() + " - OCR failed - Time required: " + obj.recognitionTimeRequired + " ms", "-", new ForegroundColorSpan(0xFFFF0000));
        mPreviewStatusTv.setText(cs);
    }

    /**
     * Given either a Spannable String or a regular String and a token, apply
     * the given CharacterStyle to the span between the tokens.
     *
     * NOTE: This method was adapted from:
     *  http://www.androidengineer.com/2010/08/easy-method-for-formatting-android.html
     *
     * <p>
     * For example, {@code setSpanBetweenTokens("Hello ##world##!", "##", new
     * ForegroundColorSpan(0xFFFF0000));} will return a CharSequence {@code
     * "Hello world!"} with {@code world} in red.
     *
     */
    private CharSequence setSpanBetweenTokens(CharSequence text, String token, CharacterStyle... cs) {
        // Start and end refer to the points where the span will apply
        int tokenLen = token.length();
        int start = text.toString().indexOf(token) + tokenLen;
        int end = text.toString().indexOf(token, start);

        if (start > -1 && end > -1) {
            // Copy the spannable string to a mutable spannable string
            SpannableStringBuilder ssb = new SpannableStringBuilder(text);
            for (CharacterStyle c : cs)
                ssb.setSpan(c, start, end, 0);
            text = ssb;
        }
        return text;
    }

    /** 显示标准模式UI */
    private void showStandardUI() {
        mResultLayout.setVisibility(View.GONE);
        mResultTv.setText("");
        mPreviewLayout.setVisibility(View.GONE);
        mPreviewStatusTv.setText("");
        mPreviewResultTv.setText("");

        mViewfinderView.setVisibility(View.VISIBLE);
        mShutterButton.setVisibility(View.VISIBLE);
        mResultBean = null;
        mViewfinderView.removeResultText();
    }

    /** 显示结果UI */
    private void showResultUI(OcrResultBean bean){
        mResultLayout.setVisibility(View.VISIBLE);
        mShutterButton.setVisibility(View.GONE);
        mPreviewLayout.setVisibility(View.GONE);
        mViewfinderView.setVisibility(View.GONE);
    }

    /** Displays a pop-up message showing the name of the current OCR source language. */
    public void showLanguageName() {
        Toast toast = Toast.makeText(this, "OCR: " + mRequestBean.getLanguage(), Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 0, 0);
        toast.show();
    }

    /**
     * Displays an initial message to the user while waiting for the first OCR request to be
     * completed after starting realtime OCR.
     */
    void setStatusViewForContinuous() {
        mViewfinderView.removeResultText();
        mPreviewStatusTv.setText("OCR: " + mRequestBean.getLanguage() + " - waiting for OCR...");
    }

    @SuppressWarnings("unused")
    public void setButtonVisibility(boolean visible) {
        mShutterButton.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /**
     * Enables/disables the shutter button to prevent double-clicks on the button.
     *
     * @param clickable True if the button should accept a click
     */
    void setShutterButtonClickable(boolean clickable) {
        mShutterButton.setClickable(clickable);
    }

    /** Request the viewfinder to be invalidated. */
    void drawViewfinder() {
        mViewfinderView.drawViewfinder();
    }


    /**
     * Requests autofocus after a 350 ms delay. This delay prevents requesting focus when the user
     * just wants to click the shutter button without focusing. Quick button press/release will
     * trigger onShutterButtonClick() before the focus kicks in.
     */
    private void requestDelayedAutoFocus() {
        // Wait 350 ms before focusing to avoid interfering with quick button presses when
        // the user just wants to take a picture without focusing.
        cameraManager.requestAutoFocus(CAMERA_FOCUS_DELAY);
    }

    void displayProgressDialog() {
        // Set up the indeterminate progress dialog box
        indeterminateDialog = new ProgressDialog(this);
        indeterminateDialog.setTitle("Please wait");
        indeterminateDialog.setMessage("Performing OCR using Tesseract...");
        indeterminateDialog.setCancelable(false);
        indeterminateDialog.show();
    }

    public ProgressDialog getProgressDialog() {
        return indeterminateDialog;
    }

    /**
     * Displays an error message dialog box to the user on the UI thread.
     *
     * @param title The title for the dialog box
     * @param message The error message to be displayed
     */
    public void showErrorMessage(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        dialog.dismiss();
                        finish();
                    }
                })
                .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                })
                .show();
    }
}
