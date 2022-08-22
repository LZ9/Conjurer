package com.lodz.android.conjurer.ocr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.lodz.android.conjurer.R;
import com.lodz.android.conjurer.camera.CameraHelper;
import com.lodz.android.conjurer.camera.CameraManager;
import com.lodz.android.conjurer.data.bean.OcrResultBean;

import java.util.List;


/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the result text.
 *
 * The code for this class was adapted from the ZXing project: http://code.google.com/p/zxing
 */
public final class ViewfinderView extends View {

    /** Flag to draw boxes representing the results from TessBaseAPI::GetRegions(). */
    static final boolean DRAW_REGION_BOXES = false;

    /** Flag to draw boxes representing the results from TessBaseAPI::GetTextlines(). */
    static final boolean DRAW_TEXTLINE_BOXES = true;

    /** Flag to draw boxes representing the results from TessBaseAPI::GetStrips(). */
    static final boolean DRAW_STRIP_BOXES = false;

    /** Flag to draw boxes representing the results from TessBaseAPI::GetWords(). */
    static final boolean DRAW_WORD_BOXES = true;

    /** Flag to draw word text with a background varying from transparent to opaque. */
    static final boolean DRAW_TRANSPARENT_WORD_BACKGROUNDS = false;

    /** Flag to draw the text of words within their respective boxes from TessBaseAPI::GetWords(). */
    static final boolean DRAW_WORD_TEXT = false;


    private final static int BUFFER = 50;
    private final static int BIG_BUFFER = 60;

    private CameraHelper mCameraHelper;
    private final Paint paint;
    private final int maskColor;
    private final int frameColor;
    private final int cornerColor;
    private OcrResultBean resultBean;
    private String[] words;
    private List<Rect> regionBoundingBoxes;
    private List<Rect> textlineBoundingBoxes;
    private List<Rect> stripBoundingBoxes;
    private List<Rect> wordBoundingBoxes;
    //  Rect bounds;
    private Rect previewFrame;
    private Rect rect;

    // This constructor is used when the class is built from an XML resource.
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Initialize these once for performance rather than calling them every time in onDraw().
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Resources resources = getResources();
        maskColor = resources.getColor(R.color.cj_color_60000000);
        frameColor = resources.getColor(R.color.cj_color_d6d6d6);
        cornerColor = resources.getColor(R.color.cj_color_ffffff);

        //    bounds = new Rect();
        previewFrame = new Rect();
        rect = new Rect();
    }

    public void setCameraManager(CameraManager cameraManager) {
//        this.cameraManager = cameraManager;
    }

    public void setCameraHelper(CameraHelper cameraHelper) {
        this.mCameraHelper = cameraHelper;
    }

    @SuppressWarnings("unused")
    @Override
    public void onDraw(Canvas canvas) {
        Rect frame = mCameraHelper.getFramingRect();

        if (frame == null) {
            return;
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Draw the exterior (i.e. outside the framing rect) darkened
        paint.setColor(maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);

        // If we have an OCR result, overlay its information on the viewfinder.
        if (resultBean != null) {

            // Only draw text/bounding boxes on viewfinder if it hasn't been resized since the OCR was requested.
            Point bitmapSize = resultBean.getBitmapDimensions();
            previewFrame = mCameraHelper.getFramingRectInPreview();
            if (bitmapSize.x == previewFrame.width() && bitmapSize.y == previewFrame.height()) {


                float scaleX = frame.width() / (float) previewFrame.width();
                float scaleY = frame.height() / (float) previewFrame.height();

                if (DRAW_REGION_BOXES) {
                    regionBoundingBoxes = resultBean.regionBoundingBoxes;
                    for (int i = 0; i < regionBoundingBoxes.size(); i++) {
                        paint.setAlpha(0xA0);
                        paint.setColor(Color.MAGENTA);
                        paint.setStyle(Style.STROKE);
                        paint.setStrokeWidth(1);
                        rect = regionBoundingBoxes.get(i);
                        canvas.drawRect(frame.left + rect.left * scaleX,
                                frame.top + rect.top * scaleY,
                                frame.left + rect.right * scaleX,
                                frame.top + rect.bottom * scaleY, paint);
                    }
                }

                if (DRAW_TEXTLINE_BOXES) {
                    // Draw each textline
                    textlineBoundingBoxes = resultBean.textlineBoundingBoxes;
                    paint.setAlpha(0xA0);
                    paint.setColor(Color.RED);
                    paint.setStyle(Style.STROKE);
                    paint.setStrokeWidth(1);
                    for (int i = 0; i < textlineBoundingBoxes.size(); i++) {
                        rect = textlineBoundingBoxes.get(i);
                        canvas.drawRect(frame.left + rect.left * scaleX,
                                frame.top + rect.top * scaleY,
                                frame.left + rect.right * scaleX,
                                frame.top + rect.bottom * scaleY, paint);
                    }
                }

                if (DRAW_STRIP_BOXES) {
                    stripBoundingBoxes = resultBean.stripBoundingBoxes;
                    paint.setAlpha(0xFF);
                    paint.setColor(Color.YELLOW);
                    paint.setStyle(Style.STROKE);
                    paint.setStrokeWidth(1);
                    for (int i = 0; i < stripBoundingBoxes.size(); i++) {
                        rect = stripBoundingBoxes.get(i);
                        canvas.drawRect(frame.left + rect.left * scaleX,
                                frame.top + rect.top * scaleY,
                                frame.left + rect.right * scaleX,
                                frame.top + rect.bottom * scaleY, paint);
                    }
                }

                if (DRAW_WORD_BOXES || DRAW_WORD_TEXT) {
                    // Split the text into words
                    wordBoundingBoxes = resultBean.wordBoundingBoxes;
                    //      for (String w : words) {
                    //        Log.e("ViewfinderView", "word: " + w);
                    //      }
                    //Log.d("ViewfinderView", "There are " + words.length + " words in the string array.");
                    //Log.d("ViewfinderView", "There are " + wordBoundingBoxes.size() + " words with bounding boxes.");
                }

                if (DRAW_WORD_BOXES) {
                    paint.setAlpha(0xFF);
                    paint.setColor(0xFF00CCFF);
                    paint.setStyle(Style.STROKE);
                    paint.setStrokeWidth(1);
                    for (int i = 0; i < wordBoundingBoxes.size(); i++) {
                        // Draw a bounding box around the word
                        rect = wordBoundingBoxes.get(i);
                        canvas.drawRect(
                                frame.left + rect.left * scaleX,
                                frame.top + rect.top * scaleY,
                                frame.left + rect.right * scaleX,
                                frame.top + rect.bottom * scaleY, paint);
                    }
                }

                if (DRAW_WORD_TEXT) {
                    words = resultBean.text.replace("\n"," ").split(" ");
                    int[] wordConfidences = resultBean.wordConfidences;
                    for (int i = 0; i < wordBoundingBoxes.size(); i++) {
                        boolean isWordBlank = true;
                        try {
                            if (!words[i].equals("")) {
                                isWordBlank = false;
                            }
                        } catch (ArrayIndexOutOfBoundsException e) {
                            e.printStackTrace();
                        }

                        // Only draw if word has characters
                        if (!isWordBlank) {
                            // Draw a white background around each word
                            rect = wordBoundingBoxes.get(i);
                            paint.setColor(Color.WHITE);
                            paint.setStyle(Style.FILL);
                            if (DRAW_TRANSPARENT_WORD_BACKGROUNDS) {
                                // Higher confidence = more opaque, less transparent background
                                paint.setAlpha(wordConfidences[i] * 255 / 100);
                            } else {
                                paint.setAlpha(255);
                            }
                            canvas.drawRect(frame.left + rect.left * scaleX,
                                    frame.top + rect.top * scaleY,
                                    frame.left + rect.right * scaleX,
                                    frame.top + rect.bottom * scaleY, paint);

                            // Draw the word in black text
                            paint.setColor(Color.BLACK);
                            paint.setAlpha(0xFF);
                            paint.setAntiAlias(true);
                            paint.setTextAlign(Align.LEFT);

                            // Adjust text size to fill rect
                            paint.setTextSize(100);
                            paint.setTextScaleX(1.0f);
                            // ask the paint for the bounding rect if it were to draw this text
                            Rect bounds = new Rect();
                            paint.getTextBounds(words[i], 0, words[i].length(), bounds);
                            // get the height that would have been produced
                            int h = bounds.bottom - bounds.top;
                            // figure out what textSize setting would create that height of text
                            float size  = (((float)(rect.height())/h)*100f);
                            // and set it into the paint
                            paint.setTextSize(size);
                            // Now set the scale.
                            // do calculation with scale of 1.0 (no scale)
                            paint.setTextScaleX(1.0f);
                            // ask the paint for the bounding rect if it were to draw this text.
                            paint.getTextBounds(words[i], 0, words[i].length(), bounds);
                            // determine the width
                            int w = bounds.right - bounds.left;
                            // calculate the baseline to use so that the entire text is visible including the descenders
                            int text_h = bounds.bottom-bounds.top;
                            int baseline =bounds.bottom+((rect.height()-text_h)/2);
                            // determine how much to scale the width to fit the view
                            float xscale = ((float) (rect.width())) / w;
                            // set the scale for the text paint
                            paint.setTextScaleX(xscale);
                            canvas.drawText(words[i], frame.left + rect.left * scaleX, frame.top + rect.bottom * scaleY - baseline, paint);
                        }

                    }
                }
            }

        }
        // Draw a two pixel solid border inside the framing rect
        paint.setAlpha(0);
        paint.setStyle(Style.FILL);
        paint.setColor(frameColor);
        canvas.drawRect(frame.left, frame.top, frame.right + 1, frame.top + 2, paint);
        canvas.drawRect(frame.left, frame.top + 2, frame.left + 2, frame.bottom - 1, paint);
        canvas.drawRect(frame.right - 1, frame.top, frame.right + 1, frame.bottom - 1, paint);
        canvas.drawRect(frame.left, frame.bottom - 1, frame.right + 1, frame.bottom + 1, paint);

        // Draw the framing rect corner UI elements
        paint.setColor(cornerColor);
        canvas.drawRect(frame.left - 15, frame.top - 15, frame.left + 15, frame.top, paint);
        canvas.drawRect(frame.left - 15, frame.top, frame.left, frame.top + 15, paint);
        canvas.drawRect(frame.right - 15, frame.top - 15, frame.right + 15, frame.top, paint);
        canvas.drawRect(frame.right, frame.top - 15, frame.right + 15, frame.top + 15, paint);
        canvas.drawRect(frame.left - 15, frame.bottom, frame.left + 15, frame.bottom + 15, paint);
        canvas.drawRect(frame.left - 15, frame.bottom - 15, frame.left, frame.bottom, paint);
        canvas.drawRect(frame.right - 15, frame.bottom, frame.right + 15, frame.bottom + 15, paint);
        canvas.drawRect(frame.right, frame.bottom - 15, frame.right + 15, frame.bottom + 15, paint);


        // Request another update at the animation interval, but don't repaint the entire viewfinder mask.
        //postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top, frame.right, frame.bottom);
    }

    public void drawViewfinder() {
        invalidate();
    }

    /**
     * Adds the given OCR results for drawing to the view.
     *
     * @param bean Object containing OCR-derived text and corresponding data.
     */
    public void setResultText(OcrResultBean bean) {
        resultBean = bean;
    }

    /**
     * Nullifies OCR text to remove it at the next onDraw() drawing.
     */
    public void removeResultText() {
        resultBean = null;
    }

    private int lastX = -1;
    private int lastY = -1;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_UP:
                lastX = -1;
                lastY = -1;
                return true;
            case MotionEvent.ACTION_MOVE:
                int currentX = (int) event.getX();
                int currentY = (int) event.getY();

                try {
                    Rect rect = mCameraHelper.getFramingRect();


                    if (lastX >= 0) {
                        // Adjust the size of the viewfinder rectangle. Check if the touch event occurs in the corner areas first, because the regions overlap.
                        if (((currentX >= rect.left - BIG_BUFFER && currentX <= rect.left + BIG_BUFFER) || (lastX >= rect.left - BIG_BUFFER && lastX <= rect.left + BIG_BUFFER))
                                && ((currentY <= rect.top + BIG_BUFFER && currentY >= rect.top - BIG_BUFFER) || (lastY <= rect.top + BIG_BUFFER && lastY >= rect.top - BIG_BUFFER))) {
                            // Top left corner: adjust both top and left sides
                            mCameraHelper.adjustFramingRect( 2 * (lastX - currentX), 2 * (lastY - currentY));
                            removeResultText();
                        } else if (((currentX >= rect.right - BIG_BUFFER && currentX <= rect.right + BIG_BUFFER) || (lastX >= rect.right - BIG_BUFFER && lastX <= rect.right + BIG_BUFFER))
                                && ((currentY <= rect.top + BIG_BUFFER && currentY >= rect.top - BIG_BUFFER) || (lastY <= rect.top + BIG_BUFFER && lastY >= rect.top - BIG_BUFFER))) {
                            // Top right corner: adjust both top and right sides
                            mCameraHelper.adjustFramingRect( 2 * (currentX - lastX), 2 * (lastY - currentY));
                            removeResultText();
                        } else if (((currentX >= rect.left - BIG_BUFFER && currentX <= rect.left + BIG_BUFFER) || (lastX >= rect.left - BIG_BUFFER && lastX <= rect.left + BIG_BUFFER))
                                && ((currentY <= rect.bottom + BIG_BUFFER && currentY >= rect.bottom - BIG_BUFFER) || (lastY <= rect.bottom + BIG_BUFFER && lastY >= rect.bottom - BIG_BUFFER))) {
                            // Bottom left corner: adjust both bottom and left sides
                            mCameraHelper.adjustFramingRect(2 * (lastX - currentX), 2 * (currentY - lastY));
                            removeResultText();
                        } else if (((currentX >= rect.right - BIG_BUFFER && currentX <= rect.right + BIG_BUFFER) || (lastX >= rect.right - BIG_BUFFER && lastX <= rect.right + BIG_BUFFER))
                                && ((currentY <= rect.bottom + BIG_BUFFER && currentY >= rect.bottom - BIG_BUFFER) || (lastY <= rect.bottom + BIG_BUFFER && lastY >= rect.bottom - BIG_BUFFER))) {
                            // Bottom right corner: adjust both bottom and right sides
                            mCameraHelper.adjustFramingRect(2 * (currentX - lastX), 2 * (currentY - lastY));
                            removeResultText();
                        } else if (((currentX >= rect.left - BUFFER && currentX <= rect.left + BUFFER) || (lastX >= rect.left - BUFFER && lastX <= rect.left + BUFFER))
                                && ((currentY <= rect.bottom && currentY >= rect.top) || (lastY <= rect.bottom && lastY >= rect.top))) {
                            // Adjusting left side: event falls within BUFFER pixels of left side, and between top and bottom side limits
                            mCameraHelper.adjustFramingRect(2 * (lastX - currentX), 0);
                            removeResultText();
                        } else if (((currentX >= rect.right - BUFFER && currentX <= rect.right + BUFFER) || (lastX >= rect.right - BUFFER && lastX <= rect.right + BUFFER))
                                && ((currentY <= rect.bottom && currentY >= rect.top) || (lastY <= rect.bottom && lastY >= rect.top))) {
                            // Adjusting right side: event falls within BUFFER pixels of right side, and between top and bottom side limits
                            mCameraHelper.adjustFramingRect(2 * (currentX - lastX), 0);
                            removeResultText();
                        } else if (((currentY <= rect.top + BUFFER && currentY >= rect.top - BUFFER) || (lastY <= rect.top + BUFFER && lastY >= rect.top - BUFFER))
                                && ((currentX <= rect.right && currentX >= rect.left) || (lastX <= rect.right && lastX >= rect.left))) {
                            // Adjusting top side: event falls within BUFFER pixels of top side, and between left and right side limits
                            mCameraHelper.adjustFramingRect(0, 2 * (lastY - currentY));
                            removeResultText();
                        } else if (((currentY <= rect.bottom + BUFFER && currentY >= rect.bottom - BUFFER) || (lastY <= rect.bottom + BUFFER && lastY >= rect.bottom - BUFFER))
                                && ((currentX <= rect.right && currentX >= rect.left) || (lastX <= rect.right && lastX >= rect.left))) {
                            // Adjusting bottom side: event falls within BUFFER pixels of bottom side, and between left and right side limits
                            mCameraHelper.adjustFramingRect(0, 2 * (currentY - lastY));
                            removeResultText();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("TAG", "Framing rect not available", e);
                }
                invalidate();
                lastX = currentX;
                lastY = currentY;
                return true;
        }
        return super.onTouchEvent(event);
    }
}
