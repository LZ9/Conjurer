package com.lodz.android.conjurer.ocr;


import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.googlecode.leptonica.android.Pixa;
import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.lodz.android.conjurer.bean.OcrResultBean;
import com.lodz.android.conjurer.bean.OcrResultFailure;
import com.lodz.android.conjurer.config.Constant;
import com.lodz.android.conjurer.ocr.task.OcrRecognizeAsyncTask;

/**
 * Class to send bitmap data for OCR.
 *
 * The code for this class was adapted from the ZXing project: http://code.google.com/p/zxing/
 */
final class DecodeHandler extends Handler {

    private final CaptureActivity activity;
    private boolean running = true;
    private final TessBaseAPI baseApi;
    private Bitmap bitmap;
    private static boolean isDecodePending;
    private long timeRequired;

    DecodeHandler(CaptureActivity activity) {
        this.activity = activity;
        baseApi = activity.getBaseApi();
    }

    @Override
    public void handleMessage(Message message) {
        if (!running) {
            return;
        }
        switch (message.what) {
            case Constant.CJ_OCR_CONTINUOUS_DECODE:
                // Only request a decode if a request is not already pending.
                if (!isDecodePending) {
                    isDecodePending = true;
                    ocrContinuousDecode((byte[]) message.obj, message.arg1, message.arg2);
                }
                break;
            case Constant.CJ_OCR_DECODE:
                ocrDecode((byte[]) message.obj, message.arg1, message.arg2);
                break;
            case Constant.CJ_QUIT:
                running = false;
                Looper.myLooper().quit();
                break;
        }
    }

    static void resetDecodeState() {
        isDecodePending = false;
    }

    /**
     *  Launch an AsyncTask to perform an OCR decode for single-shot mode.
     *
     * @param data Image data
     * @param width Image width
     * @param height Image height
     */
    private void ocrDecode(byte[] data, int width, int height) {
        activity.displayProgressDialog();
        // Launch OCR asynchronously, so we get the dialog box displayed immediately
        new OcrRecognizeAsyncTask(activity, baseApi, data, width, height).execute();
    }

    /**
     *  Perform an OCR decode for realtime recognition mode.
     *
     * @param data Image data
     * @param width Image width
     * @param height Image height
     */
    private void ocrContinuousDecode(byte[] data, int width, int height) {
        PlanarYUVLuminanceSource source = activity.getCameraManager().buildLuminanceSource(data, width, height);
        if (source == null) {
            sendContinuousOcrFailMessage();
            return;
        }
        bitmap = source.renderCroppedGreyscaleBitmap();

        OcrResultBean bean = getOcrResult();
        Handler handler = activity.getHandler();
        if (handler == null) {
            return;
        }

        if (bean == null) {
            try {
                sendContinuousOcrFailMessage();
            } catch (NullPointerException e) {
                activity.stopHandler();
            } finally {
                bitmap.recycle();
                baseApi.clear();
            }
            return;
        }

        try {
            Message message = Message.obtain(handler, Constant.CJ_OCR_CONTINUOUS_DECODE_SUCCEEDED, bean);
            message.sendToTarget();
        } catch (NullPointerException e) {
            activity.stopHandler();
        } finally {
            baseApi.clear();
        }
    }

    @SuppressWarnings("unused")
    private OcrResultBean getOcrResult() {
        OcrResultBean ocrResultBean;
        String textResult;
        long start = System.currentTimeMillis();

        try {
            baseApi.setImage(ReadFile.readBitmap(bitmap));
            textResult = baseApi.getUTF8Text();
            timeRequired = System.currentTimeMillis() - start;

            // Check for failure to recognize text
            if (textResult == null || textResult.equals("")) {
                return null;
            }
            ocrResultBean = new OcrResultBean();
            ocrResultBean.wordConfidences = baseApi.wordConfidences();
            ocrResultBean.meanConfidence = baseApi.meanConfidence();
            if (ViewfinderView.DRAW_REGION_BOXES) {
                Pixa regions = baseApi.getRegions();
                ocrResultBean.regionBoundingBoxes = regions.getBoxRects();
                regions.recycle();
            }
            if (ViewfinderView.DRAW_TEXTLINE_BOXES) {
                Pixa textlines = baseApi.getTextlines();
                ocrResultBean.textlineBoundingBoxes = textlines.getBoxRects();
                textlines.recycle();
            }
            if (ViewfinderView.DRAW_STRIP_BOXES) {
                Pixa strips = baseApi.getStrips();
                ocrResultBean.stripBoundingBoxes = strips.getBoxRects();
                strips.recycle();
            }

            // Always get the word bounding boxes--we want it for annotating the bitmap after the user
            // presses the shutter button, in addition to maybe wanting to draw boxes/words during the
            // continuous mode recognition.
            Pixa words = baseApi.getWords();
            ocrResultBean.wordBoundingBoxes = words.getBoxRects();
            words.recycle();

//      if (ViewfinderView.DRAW_CHARACTER_BOXES || ViewfinderView.DRAW_CHARACTER_TEXT) {
//        ocrResultBean.setCharacterBoundingBoxes(baseApi.getCharacters().getBoxRects());
//      }
        } catch (RuntimeException e) {
            Log.e("OcrRecognizeAsyncTask", "Caught RuntimeException in request to Tesseract. Setting state to CONTINUOUS_STOPPED.");
            e.printStackTrace();
            try {
                baseApi.clear();
                activity.stopHandler();
            } catch (NullPointerException e1) {
                // Continue
            }
            return null;
        }
        timeRequired = System.currentTimeMillis() - start;
        ocrResultBean.bitmap = bitmap;
        ocrResultBean.text = textResult;
        ocrResultBean.recognitionTimeRequired = timeRequired;
        return ocrResultBean;
    }

    private void sendContinuousOcrFailMessage() {
        Handler handler = activity.getHandler();
        if (handler != null) {
            Message message = Message.obtain(handler, Constant.CJ_OCR_CONTINUOUS_DECODE_FAILED, new OcrResultFailure(timeRequired));
            message.sendToTarget();
        }
    }

}








