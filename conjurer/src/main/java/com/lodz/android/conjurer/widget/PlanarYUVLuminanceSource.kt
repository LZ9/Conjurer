package com.lodz.android.conjurer.widget

import android.graphics.Bitmap

/**
 * This object extends LuminanceSource around an array of YUV data returned from the camera driver,
 * with the option to crop to a rectangle within the full data. This can be used to exclude
 * superfluous pixels around the perimeter and speed up decoding.
 *
 * It works for any pixel format where the Y channel is planar and appears first, including
 * YCbCr_420_SP and YCbCr_422_SP.
 *
 * The code for this class was adapted from the ZXing project: http://code.google.com/p/zxing
 */
class PlanarYUVLuminanceSource(
    private val yuvData: ByteArray,//传入的帧数据
    private val dataWidth: Int, //数据源图片宽度
    private val dataHeight: Int,//数据源图片高度
    private val left: Int,//识别区域的左边距
    private val top: Int,//识别区域的上边距
    width: Int,//识别区域的宽度
    height: Int,//识别区域的高度
    reverseHorizontal: Boolean = false//是否需要旋转图片
) : LuminanceSource(width, height) {

    init {
        if (left + width > dataWidth || top + height > dataHeight) {
            throw IllegalArgumentException("Crop rectangle does not fit within image data.")
        }
        if (reverseHorizontal) {
            reverseHorizontal(width, height)
        }
    }

    override fun getRow(y: Int, row: ByteArray?): ByteArray {
        if (y < 0 || y >= getHeight()) {
            throw IllegalArgumentException("Requested row is outside the image: $y")
        }
        val width = getWidth()
        val bytes = if (row == null || row.size < width) ByteArray(width) else row
        val offset = (y + top) * dataWidth + left
        System.arraycopy(yuvData, offset, bytes, 0, width)
        return bytes
    }

    override fun getMatrix(): ByteArray {
        val width = getWidth()
        val height = getHeight()

        // If the caller asks for the entire underlying image, save the copy and give them the
        // original data. The docs specifically warn that result.length must be ignored.
        if (width == dataWidth && height == dataHeight) {
            return yuvData
        }

        val area = width * height
        val matrix = ByteArray(area)
        var inputOffset = top * dataWidth + left

        // If the width matches the full width of the underlying data, perform a single copy.
        if (width == dataWidth) {
            System.arraycopy(yuvData, inputOffset, matrix, 0, area)
            return matrix
        }

        // Otherwise copy one cropped row at a time.
        val yuv = yuvData
        for (y in 0 until height) {
            val outputOffset = y * width
            System.arraycopy(yuv, inputOffset, matrix, outputOffset, width)
            inputOffset += dataWidth
        }
        return matrix
    }

    override fun crop(left: Int, top: Int, width: Int, height: Int): LuminanceSource {
        return PlanarYUVLuminanceSource(
            yuvData,
            dataWidth,
            dataHeight,
            this.left + left,
            this.top + top,
            width,
            height
        )
    }

    fun renderCroppedGreyscaleBitmap(): Bitmap {
        val width = getWidth()
        val height = getHeight()
        val pixels = IntArray(width * height)
        val yuv = yuvData
        var inputOffset = top * dataWidth + left
        for (y in 0 until height) {
            val outputOffset = y * width
            for (x in 0 until width) {
                val grey: Int = yuv[inputOffset + x].toInt() and 0xff
                pixels[outputOffset + x] = -0x1000000 or grey * 0x00010101
            }
            inputOffset += dataWidth
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun reverseHorizontal(width: Int, height: Int) {
        val yuv = yuvData
        var y = 0
        var rowStart = top * dataWidth + left
        while (y < height) {
            val middle = rowStart + width / 2
            var x1 = rowStart
            var x2 = rowStart + width - 1
            while (x1 < middle) {
                val temp = yuv[x1]
                yuv[x1] = yuv[x2]
                yuv[x2] = temp
                x1++
                x2--
            }
            y++
            rowStart += dataWidth
        }
    }
}