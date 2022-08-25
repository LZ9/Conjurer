package com.lodz.android.conjurer.data.bean

import android.content.Context
import android.graphics.*
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.lodz.android.conjurer.R

/**
 * OCR识别结果数据体
 * @author zhouL
 * @date 2022/8/4
 */
class OcrResultBean {

    /** 识别结果图片 */
    @JvmField
    var bitmap: Bitmap? = null

    /** 识别文字 */
    @JvmField
    var text: String = ""

    /** 单词置信值（0到100之间） */
    @JvmField
    var wordConfidences: IntArray? = null

    /** 平均置信值（0到100之间） */
    @JvmField
    var meanConfidence: Int = 0

    /** 区域边界框 */
    @JvmField
    var regionBoundingBoxes: MutableList<Rect>? = null

    /** 文本行框 */
    @JvmField
    var textlineBoundingBoxes: MutableList<Rect>? = null

    /** 文字框 */
    @JvmField
    var wordBoundingBoxes: MutableList<Rect>? = null

    /** 带状框 */
    @JvmField
    var stripBoundingBoxes: MutableList<Rect>? = null

    /** 字符边界框 */
    @JvmField
    var characterBoundingBoxes: MutableList<Rect>? = null

    /** 识别时长 */
    @JvmField
    var recognitionTimeRequired: Long = -1

    /** 本次识别时间 */
    @JvmField
    val timestamp: Long = System.currentTimeMillis()

    /** 对识别图片内文字画框，颜色为[color] */
    fun getAnnotatedBitmap(context: Context, @ColorRes color: Int = R.color.cj_color_00ccff): Bitmap? = getAnnotatedBitmap(ContextCompat.getColor(context, color))

    /** 对识别图片内文字画框，颜色为[color] */
    fun getAnnotatedBitmap(@ColorInt color: Int): Bitmap? {
        val origin = bitmap ?: return null
        val canvas = Canvas(origin)
        val paint = Paint()
        paint.alpha = 255
        paint.color = color
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f

        // 文字周围绘制边框
        wordBoundingBoxes?.forEach {
            canvas.drawRect(it, paint)
        }
        return origin
    }

    /** 获取识别图片宽高 */
    fun getBitmapDimensions(): Point? {
        val origin = bitmap ?: return null
        return Point(origin.width, origin.height)
    }

}