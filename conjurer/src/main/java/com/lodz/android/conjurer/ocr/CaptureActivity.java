package com.lodz.android.conjurer.ocr;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatToggleButton;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.lodz.android.conjurer.R;
import com.lodz.android.conjurer.bean.OcrResultBean;
import com.lodz.android.conjurer.camera.CameraManager;
import com.lodz.android.conjurer.camera.ShutterButton;
import com.lodz.android.conjurer.config.Constant;
import com.lodz.android.conjurer.ocr.task.OcrInitAsyncTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


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



    /** Resource to use for data file downloads. */
    public static final String DOWNLOAD_BASE = "http://tesseract-ocr.googlecode.com/files/";

    /** Download filename for orientation and script detection (OSD) data. */
    public static final String OSD_FILENAME = "tesseract-ocr-3.01.osd.tar";

    /** Destination filename for orientation and script detection (OSD) data. */
    public static final String OSD_FILENAME_BASE = "osd.traineddata";

    // Options menu, for copy to clipboard
    private static final int OPTIONS_COPY_RECOGNIZED_TEXT_ID = Menu.FIRST;
    private static final int OPTIONS_SHARE_RECOGNIZED_TEXT_ID = Menu.FIRST + 1;

    public static final int REQUEST_CODE = 700;
    public static final String EXTRA_OCR_RESULT = "extra_ocr_result";

    public static void start(Context context) {
        Intent starter = new Intent(context, CaptureActivity.class);
        context.startActivity(starter);
    }

    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private ViewfinderView viewfinderView;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private TextView statusViewBottom;
    private TextView statusViewTop;
    private TextView ocrResultView;
    private View resultView;
    private OcrResultBean lastResult;
    private boolean hasSurface;
    private TessBaseAPI mBaseApi; // Java interface for the Tesseract OCR engine
    private String sourceLanguageCodeOcr = "eng"; // ISO 639-3 language code
    private String sourceLanguageReadable = "English"; // Language name, for example, "English"
    private int ocrEngineMode = TessBaseAPI.OEM_TESSERACT_ONLY;
    private ShutterButton shutterButton;
    private boolean isContinuousModeActive = false; // Whether we are doing OCR in continuous mode
    private OnSharedPreferenceChangeListener listener;
    private ProgressDialog dialog; // for initOcr - language download & unzip
    private ProgressDialog indeterminateDialog; // also for initOcr - init OCR engine
    private boolean isEngineReady;
    private boolean isPaused;

    public Handler getHandler() {
        return handler;
    }

    TessBaseAPI getBaseApi() {
        return mBaseApi;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_capture);
        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
        resultView = findViewById(R.id.result_view);

        statusViewBottom = (TextView) findViewById(R.id.status_view_bottom);
        registerForContextMenu(statusViewBottom);
        statusViewTop = (TextView) findViewById(R.id.status_view_top);
        registerForContextMenu(statusViewTop);

        //实时预览
        AppCompatToggleButton toggleBtn = findViewById(R.id.preview_togbtn);
        toggleBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isContinuousModeActive = isChecked;
                statusViewBottom.setVisibility(isContinuousModeActive ? View.VISIBLE : View.GONE);
                statusViewTop.setVisibility(isContinuousModeActive ? View.VISIBLE : View.GONE);
                resumeOCR();
            }
        });

        handler = null;
        lastResult = null;
        hasSurface = false;

        shutterButton = findViewById(R.id.shutter_button);
        shutterButton.setOnShutterButtonListener(new ShutterButton.OnShutterButtonListener() {
            @Override
            public void onActionDownFocus(@NonNull ShutterButton btn, boolean pressed) {
                Log.v("testtag", "onShutterButtonFocus");
                requestDelayedAutoFocus();
            }

            @Override
            public void onActionUpClick(@NonNull ShutterButton btn) {
                Log.v("testtag", "onShutterButtonClick");
                if (isContinuousModeActive) {
                    onShutterButtonPressContinuous();
                } else {
                    if (handler != null) {
                        handler.shutterButtonClick();
                    }
                }
            }
        });

        ocrResultView = (TextView) findViewById(R.id.ocr_result_text_view);
        registerForContextMenu(ocrResultView);

        cameraManager = new CameraManager(getApplication());
        viewfinderView.setCameraManager(cameraManager);

        isEngineReady = false;
    }

    private SurfaceHolder.Callback mCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(@NonNull SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated()");

            if (holder == null) {
                Log.e(TAG, "surfaceCreated gave us a null surface");
            }

            // Only initialize the camera if the OCR engine is ready to go.
            if (!hasSurface && isEngineReady) {
                Log.d(TAG, "surfaceCreated(): calling initCamera()...");
                initCamera(holder);
            }
            hasSurface = true;
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            hasSurface = false;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        resetStatusView();

        String previousSourceLanguageCodeOcr = sourceLanguageCodeOcr;
        int previousOcrEngineMode = ocrEngineMode;

        // Retrieve from preferences, and set in this Activity, the page segmentation mode preference
        // Retrieve from preferences, and set in this Activity, the OCR engine mode
        ocrEngineMode = TessBaseAPI.OEM_TESSERACT_ONLY;

        // Set up the camera preview surface.
        surfaceView = findViewById(R.id.preview_view);
        surfaceHolder = surfaceView.getHolder();
        if (!hasSurface) {
            surfaceHolder.addCallback(mCallback);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        // Comment out the following block to test non-OCR functions without an SD card

        // Do OCR engine initialization, if necessary
        boolean doNewInit = (mBaseApi == null) || !sourceLanguageCodeOcr.equals(previousSourceLanguageCodeOcr) ||
                ocrEngineMode != previousOcrEngineMode;
        if (doNewInit) {
            // Initialize the OCR engine
            File storageDirectory = this.getExternalFilesDir("");
            if (storageDirectory != null) {
                initOcrEngine(storageDirectory, sourceLanguageCodeOcr, sourceLanguageReadable);
            }
        } else {
            // We already have the engine initialized, so just start the camera.
            resumeOCR();
        }
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
        isEngineReady = true;

        isPaused = false;

        if (handler != null) {
            handler.resetState();
        }
        if (mBaseApi != null) {
            mBaseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_OSD);
            mBaseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "");//黑名单
            mBaseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "Xx0123456789");//白名单
        }

        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        }
    }

    /** Called when the shutter button is pressed in continuous mode. */
    void onShutterButtonPressContinuous() {
        isPaused = true;
        handler.stop();
        if (lastResult != null) {
            handleOcrDecode(lastResult);
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
        resetStatusView();
        setStatusViewForContinuous();
        DecodeHandler.resetDecodeState();
        handler.resetState();
        shutterButton.setVisibility(View.VISIBLE);
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
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
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

        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(mCallback);
        }
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            // First check if we're paused in continuous mode, and if so, just unpause.
            if (isPaused) {
                Log.d(TAG, "only resuming continuous recognition, not quitting...");
                resumeContinuousDecoding();
                return true;
            }

            // Exit the app if we're not viewing an OCR result.
            if (lastResult == null) {
                setResult(RESULT_CANCELED);
                finish();
                return true;
            } else {
                // Go back to previewing in regular OCR mode.
                resetStatusView();
                if (handler != null) {
                    handler.sendEmptyMessage(Constant.CJ_RESTART_PREVIEW);
                }
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_CAMERA) {
            if (isContinuousModeActive) {
                onShutterButtonPressContinuous();
            } else {
                handler.hardwareShutterButtonClick();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_FOCUS) {
            // Only perform autofocus if user is not holding down the button.
            if (event.getRepeatCount() == 0) {
                cameraManager.requestAutoFocus(CAMERA_FOCUS_DELAY);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    /**
     * Requests initialization of the OCR engine with the given parameters.
     *
     * @param storageRoot Path to location of the tessdata directory to use
     * @param languageCode Three-letter ISO 639-3 language code for OCR
     * @param languageName Name of the language for OCR, for example, "English"
     */
    private void initOcrEngine(File storageRoot, String languageCode, String languageName) {
        isEngineReady = false;

        // Set up the dialog box for the thermometer-style download progress indicator
        if (dialog != null) {
            dialog.dismiss();
        }
        dialog = new ProgressDialog(this);

        // Display the name of the OCR engine we're initializing in the indeterminate progress dialog box
        indeterminateDialog = new ProgressDialog(this);
        indeterminateDialog.setTitle("Please wait");
        indeterminateDialog.setMessage("Initializing Tesseract OCR engine for " + languageName + "...");
        indeterminateDialog.setCancelable(false);
        indeterminateDialog.show();

        if (handler != null) {
            handler.quitSynchronously();
        }

        // Disable continuous mode if we're using Cube. This will prevent bad states for devices
        // with low memory that crash when running OCR with Cube, and prevent unwanted delays.
        if (ocrEngineMode == TessBaseAPI.OEM_CUBE_ONLY || ocrEngineMode == TessBaseAPI.OEM_TESSERACT_CUBE_COMBINED) {
            Log.d(TAG, "Disabling continuous preview");
            isContinuousModeActive = false;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        }

        // Start AsyncTask to install language data and init OCR
        mBaseApi = new TessBaseAPI();
        new OcrInitAsyncTask(this, mBaseApi, dialog, indeterminateDialog, languageCode, languageName, ocrEngineMode)
                .execute(storageRoot.toString());
    }

    /**
     * Displays information relating to the result of OCR, and requests a translation if necessary.
     *
     * @param bean Object representing successful OCR results
     * @return True if a non-null result was received for OCR
     */
    boolean handleOcrDecode(OcrResultBean bean) {
        lastResult = bean;

        // Test whether the result is null
        if (bean.text == null || bean.text.equals("")) {
            Toast toast = Toast.makeText(this, "OCR failed. Please try again.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP, 0, 0);
            toast.show();
            return false;
        }

        // Turn off capture-related UI elements
        shutterButton.setVisibility(View.GONE);
        statusViewBottom.setVisibility(View.GONE);
        statusViewTop.setVisibility(View.GONE);
        viewfinderView.setVisibility(View.GONE);
        resultView.setVisibility(View.VISIBLE);

        ImageView bitmapImageView = (ImageView) findViewById(R.id.image_view);
        Bitmap lastBitmap = bean.getAnnotatedBitmap(this, R.color.cj_color_00ccff);
        if (lastBitmap == null) {
            bitmapImageView.setVisibility(View.GONE);
        } else {
            bitmapImageView.setVisibility(View.VISIBLE);
            bitmapImageView.setImageBitmap(lastBitmap);
        }

        TextView ocrResultTextView = (TextView) findViewById(R.id.ocr_result_text_view);
        ocrResultTextView.setText(bean.text);
        // Crudely scale betweeen 22 and 32 -- bigger font for shorter text
        int scaledSize = Math.max(22, 32 - bean.text.length() / 4);
        ocrResultTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);

        setProgressBarVisibility(false);
//        Intent intent = new Intent();
//        intent.putExtra(EXTRA_OCR_RESULT, bean.getText());
//        setResult(RESULT_OK, intent);
        return true;
    }

//    private String getSfzh(String text) {
//        Log.d("testtag", text);
//        String word = text.trim().replace(" ", "");
//        List<String> list = getListBySeparator(word, "\n");
//        Log.i("testtag", list.size() + "");
//        List<String> sfzList = new ArrayList<String>();
//        for (String str : list) {
//            if (str.length() < 18) {
//                continue;
//            }
//            if (str.length() == 18 && IdCardUtils.validateIdCard(str)) {
//                sfzList.add(str);
//                continue;
//            }
//            boolean isMatchFail = true;
//            int offset = str.length() - 17;
//            for (int i = 0; i < offset; i++) {
//                String section = str.substring(i, 18 + i);
//                if (IdCardUtils.validateIdCard(section)) {
//                    sfzList.add(section);
//                    isMatchFail = false;
//                    break;
//                }
//            }
//            if (isMatchFail) {
//                sfzList.add("error:" + str);
//            }
//        }
//        for (String s : sfzList) {
//            Log.e("testtag", s);
//            ToastUtils.showShort(this, s);
//        }
//        return text;
//    }

    /**
     * 根据分隔符将字符串转为列表
     * @param source 字符串
     * @param separator 分隔符
     */
    private List<String> getListBySeparator(String source, String separator) {
        List<String> list = new ArrayList<String>();
        while (source.contains(separator)) {
            String value = source.substring(0, source.indexOf(separator));
            if (!TextUtils.isEmpty(value)) {
                list.add(value);
            }
            source = source.substring(source.indexOf(separator) + 1, source.length());
        }
        if (!TextUtils.isEmpty(source)) {
            list.add(source);
        }
        return list;
    }

    /**
     * Displays information relating to the results of a successful real-time OCR request.
     *
     * @param bean Object representing successful OCR results
     */
    void handleOcrContinuousDecode(OcrResultBean bean) {

        lastResult = bean;

        // Send an OcrResultText object to the ViewfinderView for text rendering
        viewfinderView.addResultText(bean);

        Integer meanConfidence = bean.meanConfidence;

        // 显示实时扫描文本结果
        statusViewTop.setText(bean.text);
        int scaledSize = Math.max(22, 32 - bean.text.length() / 4);
        statusViewTop.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);
        statusViewTop.setTextColor(Color.BLACK);
        statusViewTop.setBackgroundResource(R.color.status_top_text_background);

        statusViewTop.getBackground().setAlpha(meanConfidence * (255 / 100));

        // 显示实时扫描统计信息
        long recognitionTimeRequired = bean.recognitionTimeRequired;
        statusViewBottom.setTextSize(14);
        statusViewBottom.setText("OCR: " + sourceLanguageReadable + " - Mean confidence: " +
                meanConfidence.toString() + " - Time required: " + recognitionTimeRequired + " ms");
    }

    /**
     * Version of handleOcrContinuousDecode for failed OCR requests. Displays a failure message.
     *
     * @param obj Metadata for the failed OCR request.
     */
    void handleOcrContinuousDecodeFail(OcrResultBean obj) {
        lastResult = null;
        viewfinderView.removeResultText();

        // Reset the text in the recognized text box.
        statusViewTop.setText("");

        // 将'-'号内的文本设置为红色
        statusViewBottom.setTextSize(14);
        CharSequence cs = setSpanBetweenTokens("OCR: " + sourceLanguageReadable + " - OCR failed - Time required: "
                + obj.recognitionTimeRequired + " ms", "-", new ForegroundColorSpan(0xFFFF0000));
        statusViewBottom.setText(cs);
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
    private CharSequence setSpanBetweenTokens(CharSequence text, String token,
                                              CharacterStyle... cs) {
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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.equals(ocrResultView)) {
            menu.add(Menu.NONE, OPTIONS_COPY_RECOGNIZED_TEXT_ID, Menu.NONE, "Copy recognized text");
            menu.add(Menu.NONE, OPTIONS_SHARE_RECOGNIZED_TEXT_ID, Menu.NONE, "Share recognized text");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        switch (item.getItemId()) {

            case OPTIONS_COPY_RECOGNIZED_TEXT_ID:
                clipboardManager.setText(ocrResultView.getText());
                if (clipboardManager.hasText()) {
                    Toast toast = Toast.makeText(this, "Text copied.", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.BOTTOM, 0, 0);
                    toast.show();
                }
                return true;
            case OPTIONS_SHARE_RECOGNIZED_TEXT_ID:
                Intent shareRecognizedTextIntent = new Intent(android.content.Intent.ACTION_SEND);
                shareRecognizedTextIntent.setType("text/plain");
                shareRecognizedTextIntent.putExtra(android.content.Intent.EXTRA_TEXT, ocrResultView.getText());
                startActivity(Intent.createChooser(shareRecognizedTextIntent, "Share via"));
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Resets view elements.
     */
    private void resetStatusView() {
        resultView.setVisibility(View.GONE);
        statusViewBottom.setText("");
        statusViewBottom.setTextSize(14);
        statusViewBottom.setTextColor(getResources().getColor(R.color.status_text));
//            statusViewBottom.setVisibility(View.VISIBLE);

        statusViewTop.setText("");
        statusViewTop.setTextSize(14);
//            statusViewTop.setVisibility(View.VISIBLE);

        viewfinderView.setVisibility(View.VISIBLE);
        shutterButton.setVisibility(View.VISIBLE);
        lastResult = null;
        viewfinderView.removeResultText();
    }

    /** Displays a pop-up message showing the name of the current OCR source language. */
    public void showLanguageName() {
        Toast toast = Toast.makeText(this, "OCR: " + sourceLanguageReadable, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP, 0, 0);
        toast.show();
    }

    /**
     * Displays an initial message to the user while waiting for the first OCR request to be
     * completed after starting realtime OCR.
     */
    void setStatusViewForContinuous() {
        viewfinderView.removeResultText();
        statusViewBottom.setText("OCR: " + sourceLanguageReadable + " - waiting for OCR...");
    }

    @SuppressWarnings("unused")
    public void setButtonVisibility(boolean visible) {
        shutterButton.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /**
     * Enables/disables the shutter button to prevent double-clicks on the button.
     *
     * @param clickable True if the button should accept a click
     */
    void setShutterButtonClickable(boolean clickable) {
        shutterButton.setClickable(clickable);
    }

    /** Request the viewfinder to be invalidated. */
    void drawViewfinder() {
        viewfinderView.drawViewfinder();
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
