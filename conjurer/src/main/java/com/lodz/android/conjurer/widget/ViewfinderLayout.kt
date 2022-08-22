package com.lodz.android.conjurer.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.lodz.android.conjurer.R
import com.lodz.android.conjurer.data.bean.OcrResultBean

/**
 * 取景器控件
 * @author zhouL
 * @date 2022/8/17
 */
class ViewfinderLayout : View {

    companion object{

        /** 取景框默认宽度 */
        private const val DEFAULT_VIEWFINDER_WIDTH: Int = 1200

        /** 取景框默认高度 */
        private const val DEFAULT_VIEWFINDER_HEIGHT: Int = 200

        /** 触摸点位缓冲最小值 */
        private const val MIN_BUFFER = 50
        /** 触摸点位缓冲最大值 */
        private const val MAX_BUFFER = 60
    }

    /** 画笔 */
    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    /** 遮罩层颜色 */
    @ColorRes
    private val mMaskColor = R.color.cj_color_60000000
    /** 边框颜色 */
    @ColorRes
    private val mFrameColor = R.color.cj_color_d6d6d6
    /** 边角颜色 */
    @ColorRes
    private val mCornerColor = R.color.cj_color_ffffff

    /** 取景器矩形 */
    private var mViewfinderRect: Rect? = null
    /** 取景器宽度 */
    private var mViewfinderWidth = DEFAULT_VIEWFINDER_WIDTH
    /** 取景器高度 */
    private var mViewfinderHeight = DEFAULT_VIEWFINDER_HEIGHT

    /** 扫描结果实体 */
    private var mOcrResultBean: OcrResultBean? = null

    /** 用户最后触摸的X轴坐标 */
    private var mLastX = -1
    /** 用户最后触摸的Y轴坐标 */
    private var mLastY = -1

    /** 取景框变化监听器 */
    private var mListener: OnViewfinderChangeListener? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)


    /** 设置取景框宽[width]，高[height] */
    fun setViewfinderSideLength(width: Int, height: Int) {
        if (width > 0) {
            mViewfinderWidth = width
        }
        if (height > 0) {
            mViewfinderHeight = height
        }
    }

    /** 设置取景框矩形[rect] */
    fun setViewfinderRect(rect: Rect) {
        mViewfinderRect = rect
    }

    /** 设置OCR识别结果数据[bean] */
    fun setOcrResultBean(bean: OcrResultBean?) {
        mOcrResultBean = bean
    }

    /** 绘制取景器 */
    fun drawViewfinder() {
        invalidate()
    }

    /** 设置监听器[listener] */
    fun setOnViewfinderChangeListener(listener: OnViewfinderChangeListener?) {
        mListener = listener
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null){
            return
        }
        var rect = mViewfinderRect
        if (rect == null) {
            mViewfinderRect = getViewfinderRect(mViewfinderWidth, mViewfinderHeight)
            rect = mViewfinderRect
        }
        if (rect == null){
            return
        }
        drawMask(canvas, rect)//绘制遮罩层
        drawFrame(canvas, rect)//绘制边框
        drawCorner(canvas, rect)//绘制边角

        val resultBean = mOcrResultBean
        if (resultBean != null) {
            val bitmapSize= resultBean.getBitmapDimensions()

//            Log.d("testtag", "bitmapSize width : ${bitmapSize?.x}")
//            Log.d("testtag", "rect width : ${rect.width()}")
//            Log.i("testtag", "bitmapSize height : ${bitmapSize?.y}")
//            Log.i("testtag", "rect height : ${rect.height()}")


        }



        //  // If we have an OCR result, overlay its information on the viewfinder.
        //        if (resultBean != null) {
        //
        //            // Only draw text/bounding boxes on viewfinder if it hasn't been resized since the OCR was requested.
        //            Point bitmapSize = resultBean.getBitmapDimensions();
        //            previewFrame = mCameraHelper.getFramingRectInPreview();
        //            if (bitmapSize.x == previewFrame.width() && bitmapSize.y == previewFrame.height()) {
        //
        //
        //                float scaleX = frame.width() / (float) previewFrame.width();
        //                float scaleY = frame.height() / (float) previewFrame.height();
        //
        //                if (DRAW_REGION_BOXES) {
        //                    regionBoundingBoxes = resultBean.regionBoundingBoxes;
        //                    for (int i = 0; i < regionBoundingBoxes.size(); i++) {
        //                        paint.setAlpha(0xA0);
        //                        paint.setColor(Color.MAGENTA);
        //                        paint.setStyle(Style.STROKE);
        //                        paint.setStrokeWidth(1);
        //                        rect = regionBoundingBoxes.get(i);
        //                        canvas.drawRect(frame.left + rect.left * scaleX,
        //                                frame.top + rect.top * scaleY,
        //                                frame.left + rect.right * scaleX,
        //                                frame.top + rect.bottom * scaleY, paint);
        //                    }
        //                }
        //
        //                if (DRAW_TEXTLINE_BOXES) {
        //                    // Draw each textline
        //                    textlineBoundingBoxes = resultBean.textlineBoundingBoxes;
        //                    paint.setAlpha(0xA0);
        //                    paint.setColor(Color.RED);
        //                    paint.setStyle(Style.STROKE);
        //                    paint.setStrokeWidth(1);
        //                    for (int i = 0; i < textlineBoundingBoxes.size(); i++) {
        //                        rect = textlineBoundingBoxes.get(i);
        //                        canvas.drawRect(frame.left + rect.left * scaleX,
        //                                frame.top + rect.top * scaleY,
        //                                frame.left + rect.right * scaleX,
        //                                frame.top + rect.bottom * scaleY, paint);
        //                    }
        //                }
        //
        //                if (DRAW_STRIP_BOXES) {
        //                    stripBoundingBoxes = resultBean.stripBoundingBoxes;
        //                    paint.setAlpha(0xFF);
        //                    paint.setColor(Color.YELLOW);
        //                    paint.setStyle(Style.STROKE);
        //                    paint.setStrokeWidth(1);
        //                    for (int i = 0; i < stripBoundingBoxes.size(); i++) {
        //                        rect = stripBoundingBoxes.get(i);
        //                        canvas.drawRect(frame.left + rect.left * scaleX,
        //                                frame.top + rect.top * scaleY,
        //                                frame.left + rect.right * scaleX,
        //                                frame.top + rect.bottom * scaleY, paint);
        //                    }
        //                }
        //
        //                if (DRAW_WORD_BOXES || DRAW_WORD_TEXT) {
        //                    // Split the text into words
        //                    wordBoundingBoxes = resultBean.wordBoundingBoxes;
        //                    //      for (String w : words) {
        //                    //        Log.e("ViewfinderView", "word: " + w);
        //                    //      }
        //                    //Log.d("ViewfinderView", "There are " + words.length + " words in the string array.");
        //                    //Log.d("ViewfinderView", "There are " + wordBoundingBoxes.size() + " words with bounding boxes.");
        //                }
        //
        //                if (DRAW_WORD_BOXES) {
        //                    paint.setAlpha(0xFF);
        //                    paint.setColor(0xFF00CCFF);
        //                    paint.setStyle(Style.STROKE);
        //                    paint.setStrokeWidth(1);
        //                    for (int i = 0; i < wordBoundingBoxes.size(); i++) {
        //                        // Draw a bounding box around the word
        //                        rect = wordBoundingBoxes.get(i);
        //                        canvas.drawRect(
        //                                frame.left + rect.left * scaleX,
        //                                frame.top + rect.top * scaleY,
        //                                frame.left + rect.right * scaleX,
        //                                frame.top + rect.bottom * scaleY, paint);
        //                    }
        //                }
        //
        //                if (DRAW_WORD_TEXT) {
        //                    words = resultBean.text.replace("\n"," ").split(" ");
        //                    int[] wordConfidences = resultBean.wordConfidences;
        //                    for (int i = 0; i < wordBoundingBoxes.size(); i++) {
        //                        boolean isWordBlank = true;
        //                        try {
        //                            if (!words[i].equals("")) {
        //                                isWordBlank = false;
        //                            }
        //                        } catch (ArrayIndexOutOfBoundsException e) {
        //                            e.printStackTrace();
        //                        }
        //
        //                        // Only draw if word has characters
        //                        if (!isWordBlank) {
        //                            // Draw a white background around each word
        //                            rect = wordBoundingBoxes.get(i);
        //                            paint.setColor(Color.WHITE);
        //                            paint.setStyle(Style.FILL);
        //                            if (DRAW_TRANSPARENT_WORD_BACKGROUNDS) {
        //                                // Higher confidence = more opaque, less transparent background
        //                                paint.setAlpha(wordConfidences[i] * 255 / 100);
        //                            } else {
        //                                paint.setAlpha(255);
        //                            }
        //                            canvas.drawRect(frame.left + rect.left * scaleX,
        //                                    frame.top + rect.top * scaleY,
        //                                    frame.left + rect.right * scaleX,
        //                                    frame.top + rect.bottom * scaleY, paint);
        //
        //                            // Draw the word in black text
        //                            paint.setColor(Color.BLACK);
        //                            paint.setAlpha(0xFF);
        //                            paint.setAntiAlias(true);
        //                            paint.setTextAlign(Align.LEFT);
        //
        //                            // Adjust text size to fill rect
        //                            paint.setTextSize(100);
        //                            paint.setTextScaleX(1.0f);
        //                            // ask the paint for the bounding rect if it were to draw this text
        //                            Rect bounds = new Rect();
        //                            paint.getTextBounds(words[i], 0, words[i].length(), bounds);
        //                            // get the height that would have been produced
        //                            int h = bounds.bottom - bounds.top;
        //                            // figure out what textSize setting would create that height of text
        //                            float size  = (((float)(rect.height())/h)*100f);
        //                            // and set it into the paint
        //                            paint.setTextSize(size);
        //                            // Now set the scale.
        //                            // do calculation with scale of 1.0 (no scale)
        //                            paint.setTextScaleX(1.0f);
        //                            // ask the paint for the bounding rect if it were to draw this text.
        //                            paint.getTextBounds(words[i], 0, words[i].length(), bounds);
        //                            // determine the width
        //                            int w = bounds.right - bounds.left;
        //                            // calculate the baseline to use so that the entire text is visible including the descenders
        //                            int text_h = bounds.bottom-bounds.top;
        //                            int baseline =bounds.bottom+((rect.height()-text_h)/2);
        //                            // determine how much to scale the width to fit the view
        //                            float xscale = ((float) (rect.width())) / w;
        //                            // set the scale for the text paint
        //                            paint.setTextScaleX(xscale);
        //                            canvas.drawText(words[i], frame.left + rect.left * scaleX, frame.top + rect.bottom * scaleY - baseline, paint);
        //                        }
        //
        //                    }
        //                }
        //            }
        //
        //        }
        mListener?.onRectChanged(rect)
    }

    /** 计算得到取景框 */
    private fun getViewfinderRect(viewfinderWidth: Int, viewfinderHeight: Int): Rect {
        val sideWidth = if (viewfinderWidth > width) width else viewfinderWidth
        val sideHeight = if (viewfinderHeight > height) height else viewfinderHeight
        val left = width / 2 - sideWidth / 2
        val right = width / 2 + sideWidth / 2
        val top = height / 2 - sideHeight / 2
        val bottom = height / 2 + sideHeight / 2
        return Rect(left, top, right, bottom)
    }

    /** 绘制遮罩层 */
    private fun drawMask(canvas: Canvas, rect: Rect) {
        mPaint.color = ContextCompat.getColor(context, mMaskColor)
        canvas.drawRect(0f, 0f, width.toFloat(), rect.top.toFloat(), mPaint)
        canvas.drawRect(0f, rect.top.toFloat(), rect.left.toFloat(), rect.bottom.toFloat() + 1, mPaint)
        canvas.drawRect(rect.right.toFloat() + 1, rect.top.toFloat(), width.toFloat(), rect.bottom.toFloat() + 1, mPaint)
        canvas.drawRect(0f, rect.bottom.toFloat() + 1, width.toFloat(), height.toFloat(), mPaint)
    }

    /** 绘制边框 */
    private fun drawFrame(canvas: Canvas, rect: Rect) {
        mPaint.alpha = 0
        mPaint.style = Paint.Style.FILL
        mPaint.color = ContextCompat.getColor(context, mFrameColor)
        canvas.drawRect(rect.left.toFloat(), rect.top.toFloat(), rect.right.toFloat() + 1, rect.top.toFloat() + 2, mPaint)
        canvas.drawRect(rect.left.toFloat(), rect.top.toFloat() + 2, rect.left.toFloat() + 2, rect.bottom.toFloat() - 1, mPaint)
        canvas.drawRect(rect.right.toFloat() - 1, rect.top.toFloat(), rect.right.toFloat() + 1, rect.bottom.toFloat() - 1, mPaint)
        canvas.drawRect(rect.left.toFloat(), rect.bottom.toFloat() - 1, rect.right.toFloat() + 1, rect.bottom.toFloat() + 1, mPaint)
    }

    /** 绘制边角 */
    private fun drawCorner(canvas: Canvas, rect: Rect) {
        mPaint.color = ContextCompat.getColor(context, mCornerColor)
        canvas.drawRect(rect.left.toFloat() - 15, rect.top.toFloat() - 15, rect.left.toFloat() + 15, rect.top.toFloat(), mPaint)
        canvas.drawRect(rect.left.toFloat() - 15, rect.top.toFloat(), rect.left.toFloat(), rect.top.toFloat() + 15, mPaint)
        canvas.drawRect(rect.right.toFloat() - 15, rect.top.toFloat() - 15, rect.right.toFloat() + 15, rect.top.toFloat(), mPaint)
        canvas.drawRect(rect.right.toFloat(), rect.top.toFloat() - 15, rect.right.toFloat() + 15, rect.top.toFloat() + 15, mPaint)
        canvas.drawRect(rect.left.toFloat() - 15, rect.bottom.toFloat(), rect.left.toFloat() + 15, rect.bottom.toFloat() + 15, mPaint)
        canvas.drawRect(rect.left.toFloat() - 15, rect.bottom.toFloat() - 15, rect.left.toFloat(), rect.bottom.toFloat(), mPaint)
        canvas.drawRect(rect.right.toFloat() - 15, rect.bottom.toFloat(), rect.right.toFloat() + 15, rect.bottom.toFloat() + 15, mPaint)
        canvas.drawRect(rect.right.toFloat(), rect.bottom.toFloat() - 15, rect.right.toFloat() + 15, rect.bottom.toFloat() + 15, mPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val action = event?.action ?: return super.onTouchEvent(event)
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP -> {
                mLastX = -1
                mLastY = -1
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val rect = mViewfinderRect ?: return true
                val currentX = event.x.toInt()
                val currentY = event.y.toInt()
                if (mLastX >= 0) {
                    if (((currentX >= rect.left - MAX_BUFFER && currentX <= rect.left + MAX_BUFFER) || (mLastX >= rect.left - MAX_BUFFER && mLastX <= rect.left + MAX_BUFFER))
                        && ((currentY <= rect.top + MAX_BUFFER && currentY >= rect.top - MAX_BUFFER) || (mLastY <= rect.top + MAX_BUFFER && mLastY >= rect.top - MAX_BUFFER))
                    ) { // 触摸左上边角
                        setOcrResultBean(null)
                        mViewfinderRect = adjustRect(rect, 2 * (mLastX - currentX), 2 * (mLastY - currentY))
                    } else if ((currentX >= rect.right - MAX_BUFFER && currentX <= rect.right + MAX_BUFFER || mLastX >= rect.right - MAX_BUFFER && mLastX <= rect.right + MAX_BUFFER)
                        && (currentY <= rect.top + MAX_BUFFER && currentY >= rect.top - MAX_BUFFER || mLastY <= rect.top + MAX_BUFFER && mLastY >= rect.top - MAX_BUFFER)
                    ) { // 触摸右上边角
                        setOcrResultBean(null)
                        mViewfinderRect = adjustRect(rect, 2 * (currentX - mLastX), 2 * (mLastY - currentY))
                    } else if (((currentX >= rect.left - MAX_BUFFER && currentX <= rect.left + MAX_BUFFER) || (mLastX >= rect.left - MAX_BUFFER && mLastX <= rect.left + MAX_BUFFER))
                        && ((currentY <= rect.bottom + MAX_BUFFER && currentY >= rect.bottom - MAX_BUFFER) || (mLastY <= rect.bottom + MAX_BUFFER && mLastY >= rect.bottom - MAX_BUFFER))
                    ) { // 触摸左下边角
                        setOcrResultBean(null)
                        mViewfinderRect = adjustRect(rect, 2 * (mLastX - currentX), 2 * (currentY - mLastY))
                    } else if (((currentX >= rect.right - MAX_BUFFER && currentX <= rect.right + MAX_BUFFER) || (mLastX >= rect.right - MAX_BUFFER && mLastX <= rect.right + MAX_BUFFER))
                        && ((currentY <= rect.bottom + MAX_BUFFER && currentY >= rect.bottom - MAX_BUFFER) || (mLastY <= rect.bottom + MAX_BUFFER && mLastY >= rect.bottom - MAX_BUFFER))
                    ) { // 触摸右下边角
                        setOcrResultBean(null)
                        mViewfinderRect = adjustRect(rect, 2 * (currentX - mLastX), 2 * (currentY - mLastY))
                    } else if (((currentX >= rect.left - MIN_BUFFER && currentX <= rect.left + MIN_BUFFER) || (mLastX >= rect.left - MIN_BUFFER && mLastX <= rect.left + MIN_BUFFER))
                        && ((currentY <= rect.bottom && currentY >= rect.top) || (mLastY <= rect.bottom && mLastY >= rect.top))
                    ) { // 触摸左边框
                        setOcrResultBean(null)
                        mViewfinderRect = adjustRect(rect, 2 * (mLastX - currentX), 0)
                    } else if (((currentX >= rect.right - MIN_BUFFER && currentX <= rect.right + MIN_BUFFER) || (mLastX >= rect.right - MIN_BUFFER && mLastX <= rect.right + MIN_BUFFER))
                        && ((currentY <= rect.bottom && currentY >= rect.top) || (mLastY <= rect.bottom && mLastY >= rect.top))
                    ) { // 触摸右边框
                        setOcrResultBean(null)
                        mViewfinderRect = adjustRect(rect, 2 * (currentX - mLastX), 0)
                    } else if (((currentY <= rect.top + MIN_BUFFER && currentY >= rect.top - MIN_BUFFER) || (mLastY <= rect.top + MIN_BUFFER && mLastY >= rect.top - MIN_BUFFER))
                        && ((currentX <= rect.right && currentX >= rect.left) || (mLastX <= rect.right && mLastX >= rect.left))
                    ) { // 触摸上边框
                        setOcrResultBean(null)
                        mViewfinderRect = adjustRect(rect, 0, 2 * (mLastY - currentY))
                    } else if (((currentY <= rect.bottom + MIN_BUFFER && currentY >= rect.bottom - MIN_BUFFER) || (mLastY <= rect.bottom + MIN_BUFFER && mLastY >= rect.bottom - MIN_BUFFER))
                        && ((currentX <= rect.right && currentX >= rect.left) || (mLastX <= rect.right && mLastX >= rect.left))
                    ) { // 触摸下边框
                        setOcrResultBean(null)
                        mViewfinderRect = adjustRect(rect, 0, 2 * (currentY - mLastY))
                    }
                }
                invalidate()
                mLastX = currentX
                mLastY = currentY
            }
        }
        return true
    }

    private fun adjustRect(rect: Rect, x: Int, y: Int): Rect {
        var deltaWidth = x
        var deltaHeight = y
        if ((rect.width() + deltaWidth > width - 4) || (rect.width() + deltaWidth < 50)) {
            deltaWidth = 0
        }
        if ((rect.height() + deltaHeight > height - 4) || (rect.height() + deltaHeight < 50)) {
            deltaHeight = 0
        }
        val newWidth = rect.width() + deltaWidth
        val newHeight = rect.height() + deltaHeight
        val leftOffset = (width - newWidth) / 2
        val topOffset = (height - newHeight) / 2
        return Rect(leftOffset, topOffset, leftOffset + newWidth, topOffset + newHeight)
    }

}