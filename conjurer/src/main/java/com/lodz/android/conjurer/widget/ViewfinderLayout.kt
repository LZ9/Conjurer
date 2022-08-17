package com.lodz.android.conjurer.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.lodz.android.conjurer.R

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


    fun setViewfinderSideLength(width: Int, height: Int) {
        if (width > 0) {
            mViewfinderWidth = width
        }
        if (height > 0) {
            mViewfinderHeight = height
        }
    }

    fun setViewfinderRect(rect: Rect) {
        mViewfinderRect = rect
    }

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

}