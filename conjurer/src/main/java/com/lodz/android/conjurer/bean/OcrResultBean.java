package com.lodz.android.conjurer.bean;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.Rect;

import java.util.List;

/**
 * Encapsulates the result of OCR.
 */
public class OcrResultBean {

    public Bitmap bitmap;
    public String text;

    public int[] wordConfidences;
    public int meanConfidence;

    public List<Rect> regionBoundingBoxes;
    public List<Rect> textlineBoundingBoxes;
    public List<Rect> wordBoundingBoxes;
    public List<Rect> stripBoundingBoxes;
    public List<Rect> characterBoundingBoxes;
    public long recognitionTimeRequired;

    private final long timestamp;
    private final Paint paint;

    public OcrResultBean() {
        timestamp = System.currentTimeMillis();
        this.paint = new Paint();
    }

    public Bitmap getAnnotatedBitmap() {
        Canvas canvas = new Canvas(bitmap);

        // 文字周围绘制边框
        for (int i = 0; i < wordBoundingBoxes.size(); i++) {
            paint.setAlpha(0xFF);
            paint.setColor(0xFF00CCFF);
            paint.setStyle(Style.STROKE);
            paint.setStrokeWidth(2);
            Rect r = wordBoundingBoxes.get(i);
            canvas.drawRect(r, paint);
        }
        return bitmap;
    }

    public Point getBitmapDimensions() {
        return new Point(bitmap.getWidth(), bitmap.getHeight());
    }

    public long getTimestamp() {
        return timestamp;
    }

}
