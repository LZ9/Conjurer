package com.lodz.android.conjurer.ocr.task;


import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.googlecode.tesseract.android.TessBaseAPI.PageIteratorLevel;
import com.lodz.android.conjurer.bean.OcrResultBean;
import com.lodz.android.conjurer.config.Constant;
import com.lodz.android.conjurer.ocr.CaptureActivity;

import java.util.ArrayList;

/**
 * Class to send OCR requests to the OCR engine in a separate thread, send a success/failure message,
 * and dismiss the indeterminate progress dialog box. Used for non-continuous mode OCR only.
 */
public final class OcrRecognizeAsyncTask extends AsyncTask<Void, Void, Boolean> {

    //  private static final boolean PERFORM_FISHER_THRESHOLDING = false;
    //  private static final boolean PERFORM_OTSU_THRESHOLDING = false;
    //  private static final boolean PERFORM_SOBEL_THRESHOLDING = false;

    private CaptureActivity activity;
    private TessBaseAPI baseApi;
    private byte[] data;
    private int width;
    private int height;
    private OcrResultBean mOcrResultBean;
    private long timeRequired;

    public OcrRecognizeAsyncTask(CaptureActivity activity, TessBaseAPI baseApi, byte[] data, int width, int height) {
        this.activity = activity;
        this.baseApi = baseApi;
        this.data = data;
        this.width = width;
        this.height = height;
    }

    @Override
    protected Boolean doInBackground(Void... arg0) {
        long start = System.currentTimeMillis();
        Bitmap bitmap = activity.getCameraManager().buildLuminanceSource(data, width, height).renderCroppedGreyscaleBitmap();
        String textResult;

        //      if (PERFORM_FISHER_THRESHOLDING) {
        //        Pix thresholdedImage = Thresholder.fisherAdaptiveThreshold(ReadFile.readBitmap(bitmap), 48, 48, 0.1F, 2.5F);
        //        Log.e("OcrRecognizeAsyncTask", "thresholding completed. converting to bmp. size:" + bitmap.getWidth() + "x" + bitmap.getHeight());
        //        bitmap = WriteFile.writeBitmap(thresholdedImage);
        //      }
        //      if (PERFORM_OTSU_THRESHOLDING) {
        //        Pix thresholdedImage = Binarize.otsuAdaptiveThreshold(ReadFile.readBitmap(bitmap), 48, 48, 9, 9, 0.1F);
        //        Log.e("OcrRecognizeAsyncTask", "thresholding completed. converting to bmp. size:" + bitmap.getWidth() + "x" + bitmap.getHeight());
        //        bitmap = WriteFile.writeBitmap(thresholdedImage);
        //      }
        //      if (PERFORM_SOBEL_THRESHOLDING) {
        //        Pix thresholdedImage = Thresholder.sobelEdgeThreshold(ReadFile.readBitmap(bitmap), 64);
        //        Log.e("OcrRecognizeAsyncTask", "thresholding completed. converting to bmp. size:" + bitmap.getWidth() + "x" + bitmap.getHeight());
        //        bitmap = WriteFile.writeBitmap(thresholdedImage);
        //      }

        try {
            baseApi.setImage(ReadFile.readBitmap(bitmap));
            textResult = baseApi.getUTF8Text();
            timeRequired = System.currentTimeMillis() - start;

            // Check for failure to recognize text
            if (textResult == null || textResult.equals("")) {
                return false;
            }
            mOcrResultBean = new OcrResultBean();
            mOcrResultBean.wordConfidences = baseApi.wordConfidences();
            mOcrResultBean.meanConfidence = baseApi.meanConfidence();
            mOcrResultBean.regionBoundingBoxes = baseApi.getRegions().getBoxRects();
            mOcrResultBean.textlineBoundingBoxes = baseApi.getTextlines().getBoxRects();
            mOcrResultBean.wordBoundingBoxes = baseApi.getWords().getBoxRects();
            mOcrResultBean.stripBoundingBoxes = baseApi.getStrips().getBoxRects();

            // Iterate through the results.
            final ResultIterator iterator = baseApi.getResultIterator();
            int[] lastBoundingBox;
            ArrayList<Rect> charBoxes = new ArrayList<Rect>();
            iterator.begin();
            do {
                lastBoundingBox = iterator.getBoundingBox(PageIteratorLevel.RIL_SYMBOL);
                Rect lastRectBox = new Rect(lastBoundingBox[0], lastBoundingBox[1],
                        lastBoundingBox[2], lastBoundingBox[3]);
                charBoxes.add(lastRectBox);
            } while (iterator.next(PageIteratorLevel.RIL_SYMBOL));
            iterator.delete();
            mOcrResultBean.characterBoundingBoxes = charBoxes;

        } catch (RuntimeException e) {
            Log.e("OcrRecognizeAsyncTask", "Caught RuntimeException in request to Tesseract. Setting state to CONTINUOUS_STOPPED.");
            e.printStackTrace();
            try {
                baseApi.clear();
                activity.stopHandler();
            } catch (NullPointerException e1) {
                // Continue
            }
            return false;
        }
        timeRequired = System.currentTimeMillis() - start;
        mOcrResultBean.bitmap = bitmap;
        mOcrResultBean.text = textResult;
        mOcrResultBean.recognitionTimeRequired = timeRequired;
        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);

        Handler handler = activity.getHandler();
        if (handler != null) {
            // Send results for single-shot mode recognition.
            if (result) {
                Message message = Message.obtain(handler, Constant.CJ_OCR_DECODE_SUCCEEDED, mOcrResultBean);
                message.sendToTarget();
            } else {
                Message message = Message.obtain(handler, Constant.CJ_OCR_DECODE_FAILED, mOcrResultBean);
                message.sendToTarget();
            }
            activity.getProgressDialog().dismiss();
        }
        if (baseApi != null) {
            baseApi.clear();
        }
    }
}
