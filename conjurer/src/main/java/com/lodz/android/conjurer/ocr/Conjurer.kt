package com.lodz.android.conjurer.ocr

import android.content.Context
import com.googlecode.tesseract.android.TessBaseAPI
import com.lodz.android.conjurer.bean.InitStatus
import com.lodz.android.conjurer.config.Constant
import com.lodz.android.conjurer.util.OcrUtils
import kotlinx.coroutines.*
import java.io.File

/**
 * OCR文字识别
 * @author zhouL
 * @date 2022/8/9
 */
class Conjurer private constructor(){

    companion object {
        /** 创建 */
        @JvmStatic
        fun create(): Conjurer = Conjurer()
    }

    /** 训练数据路径 */
    private var mDataPath = ""
    /** 识别语言 */
    private var mLanguage = Constant.DEFAULT_LANGUAGE
    /** 识别引擎模式 */
    @TessBaseAPI.OcrEngineMode
    private var mEngineMode = TessBaseAPI.OEM_TESSERACT_ONLY
    /** 训练数据的压缩包文件全称 */
    private var mTrainedDataZipFileNames = arrayListOf(Constant.DEFAULT_ENG_TRAINEDDATA, Constant.DEFAULT_OSD_TRAINEDDATA)
    /** 页面分段模式 */
    @TessBaseAPI.PageSegMode.Mode
    private var mPageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO_OSD
    /** 黑名单 */
    private var mBlackList = Constant.DEFAULT_BLACKLIST
    /** 白名单 */
    private var mWhiteList = Constant.DEFAULT_WHITELIST
    /** 监听器 */
    private var mListener: OnConjurerListener? = null

    /** OCR的API方法 */
    private var mTessApi: TessBaseAPI? = null

    /** 设置训练数据路径[path] */
    fun setDataPath(path: String): Conjurer {
        if (path.isNotEmpty()) {
            mDataPath = path
        }
        return this
    }

    /** 设置识别语言[language] */
    fun setLanguage(language: String): Conjurer {
        if (language.isNotEmpty()) {
            mLanguage = language
        }
        return this
    }

    /** 设置识别引擎模式[mode] */
    fun setEngineMode(@TessBaseAPI.OcrEngineMode mode: Int): Conjurer {
        mEngineMode = mode
        return this
    }

    /** 设置页面分段模式[mode] */
    fun setPageSegMode(@TessBaseAPI.PageSegMode.Mode mode: Int): Conjurer {
        mPageSegMode = mode
        return this
    }

    /** 设置训练数据的压缩包文件全称[zipFileNames]（可将训练数据的压缩包放入Assets目录当中） */
    fun setTrainedDataFileName(vararg zipFileNames: String): Conjurer {
        val array: Array<out String> = zipFileNames
        if (array.isNotEmpty()) {
            mTrainedDataZipFileNames.clear()//替换安装默认的训练包
            mTrainedDataZipFileNames.addAll(array.toMutableList())
        }
        return this
    }

    /** 设置黑名单[blackList] */
    fun setBlackList(blackList: String): Conjurer {
        mBlackList = blackList
        return this
    }

    /** 设置白名单[whiteList] */
    fun setWhiteList(whiteList: String): Conjurer {
        mWhiteList = whiteList
        return this
    }

    /** 设置监听器[listener] */
    fun setOnConjurerListener(listener: OnConjurerListener?): Conjurer {
        mListener = listener
        return this
    }

    /** 删除目录中已经存在的训练数据，上下文[context] */
    fun deleteTessdataDir(context: Context) : Conjurer {
        checkPath(context)
        val path = mDataPath + Constant.DEFAULT_TRAINEDDATA_DIR_NAME
        val dir = File(path)
        if (dir.isDirectory && dir.exists()) {//如果目录存在就删掉里面的数据
            OcrUtils.delFile(path)
        }
        return this
    }

    /** 完成构建，上下文[context]，是否打开相机[isOpenCamera]（默认打开） */
    fun build(context: Context, isOpenCamera: Boolean = true) {
        MainScope().launch {
            checkPath(context)
            mListener?.onInit(InitStatus.START)
            val path = mDataPath + Constant.DEFAULT_TRAINEDDATA_DIR_NAME + File.separator
            val dir = File(path)
            dir.mkdirs()//创建训练数据存放目录
            if (!dir.exists()) {
                mListener?.onError(IllegalArgumentException("couldn't create $path"), "无法创建训练数据存放目录")
                return@launch
            }
            mListener?.onInit(InitStatus.CHECK_LOCAL_TRAINED_DATA)
            for (zipFileName in mTrainedDataZipFileNames) {
                var hasTrainedData: Pair<Boolean, Throwable?>
                withContext(Dispatchers.IO) {
                    hasTrainedData = OcrUtils.installZipFromAssets(context, path, zipFileName) //训练文件是否安装
                }
                if (!hasTrainedData.first) {
                    mListener?.onError(hasTrainedData.second ?: IllegalArgumentException("trained data install fail"), "训练文件安装失败")
                    return@launch
                }
            }
            if (mTessApi == null) {
                mTessApi = TessBaseAPI()
                val isSuccess = mTessApi?.init(mDataPath, mLanguage, mEngineMode) ?: false
                if (!isSuccess){
                    mListener?.onError(IllegalArgumentException("TessApi init fail"), "OCR初始化失败")
                    return@launch
                }
            }
            mTessApi?.pageSegMode = mPageSegMode
            mTessApi?.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, mBlackList)//黑名单
            mTessApi?.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, mWhiteList)//白名单
            mListener?.onInit(InitStatus.COMPLETE)
            if (isOpenCamera) {
                CaptureActivity.start(context)
            }
        }
    }

    /** 核对文件路径 */
    private fun checkPath(context: Context) {
        if (mDataPath.isEmpty()) {
            mDataPath = context.getExternalFilesDir("")?.absolutePath ?: context.cacheDir.absolutePath
        }
        if (!mDataPath.endsWith(File.separator)) {
            mDataPath += File.separator
        }
    }
}