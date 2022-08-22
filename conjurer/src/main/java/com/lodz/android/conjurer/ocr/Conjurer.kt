package com.lodz.android.conjurer.ocr

import android.content.Context
import com.googlecode.tesseract.android.TessBaseAPI
import com.lodz.android.conjurer.data.status.InitStatus
import com.lodz.android.conjurer.data.event.OcrEvent
import com.lodz.android.conjurer.data.bean.OcrRequestBean
import com.lodz.android.conjurer.config.Constant
import com.lodz.android.conjurer.transformer.OcrResultTransformer
import com.lodz.android.conjurer.util.OcrUtils
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.lang.RuntimeException

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
    /** 转换器列表 */
    private var mTransformerList: ArrayList<OcrResultTransformer> = arrayListOf()
    /** 是否实时预览 */
    private var isRealTimePreview = false

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

    /** 设置是否实时预览[isPreview] */
    fun setRealTimePreview(isPreview: Boolean): Conjurer {
        isRealTimePreview = isPreview
        return this
    }

    /** 添加OCR识别结果转换器 */
    fun addOcrResultTransformer(vararg transformer: OcrResultTransformer): Conjurer {
        mTransformerList.addAll(transformer)
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

    /** 启动相机 */
    fun openCamera(context: Context) {
        EventBus.getDefault().register(this)
        MainScope().launch {
            var isCheckSuccess: Boolean
            withContext(Dispatchers.IO) {
                isCheckSuccess = checkLocalTrainedData(context)
            }
            if (!isCheckSuccess){
                return@launch
            }
            mListener?.onInit(InitStatus.COMPLETE)
            OcrCameraActivity.start(context, OcrRequestBean(mDataPath, mLanguage, mEngineMode, mPageSegMode, mBlackList, mWhiteList, isRealTimePreview, mTransformerList))
        }
    }

    /** 异步识别，上下文[context]，图片的[base64] */
    fun recogAsync(context: Context, base64: String) {
        MainScope().launch {
            var isCheckSuccess: Boolean
            withContext(Dispatchers.IO) {
                isCheckSuccess = checkLocalTrainedData(context)
            }
            if (!isCheckSuccess){
                return@launch
            }
            //完成本地训练文件校验
            if (mTessApi == null) {
                mTessApi = TessBaseAPI()
                val isSuccess = mTessApi?.init(mDataPath, mLanguage, mEngineMode) ?: false
                if (!isSuccess){
                    mListener?.onError(
                        Constant.TYPE_EVENT_ERROR_OCR_INIT_FAIL,
                        IllegalArgumentException("TessApi init fail"),
                        "OCR初始化失败"
                    )
                    return@launch
                }
            }
            mTessApi?.pageSegMode = mPageSegMode
            mTessApi?.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, mBlackList)//黑名单
            mTessApi?.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, mWhiteList)//白名单
            mListener?.onInit(InitStatus.COMPLETE)
            if (base64.isEmpty()){
                return@launch
            }
            // TODO: 2022/8/10 异步识别
        }
    }

    /** 校验本地训练文件，上下文[context] */
    private suspend fun checkLocalTrainedData(context: Context): Boolean {
        checkPath(context)
        runOnMain { mListener?.onInit(InitStatus.START) }
        val path = mDataPath + Constant.DEFAULT_TRAINEDDATA_DIR_NAME + File.separator
        val dir = File(path)
        dir.mkdirs()//创建训练数据存放目录
        if (!dir.exists()) {
            runOnMain {
                mListener?.onError(
                    Constant.TYPE_EVENT_ERROR_DIR_CREATE_FAIL,
                    IllegalArgumentException("couldn't create $path"),
                    "无法创建训练数据存放目录"
                )
            }
            return false
        }
        runOnMain { mListener?.onInit(InitStatus.CHECK_LOCAL_TRAINED_DATA) }
        for (zipFileName in mTrainedDataZipFileNames) {
            val hasTrainedData = OcrUtils.installZipFromAssets(context, path, zipFileName) //训练文件是否安装
            if (!hasTrainedData.first) {
                runOnMain {
                    mListener?.onError(
                        Constant.TYPE_EVENT_ERROR_TRAINED_DATA_INSTALL_FAIL,
                        hasTrainedData.second ?: IllegalArgumentException("trained data install fail"),
                        "训练文件安装失败"
                    )
                }
                return false
            }
        }
        runOnMain { mListener?.onInit(InitStatus.CHECK_LOCAL_TRAINED_DATA_SUCCESS) }
        return true
    }

    /** 核对文件路径，上下文[context]  */
    private fun checkPath(context: Context) {
        if (mDataPath.isEmpty()) {
            mDataPath = context.getExternalFilesDir("")?.absolutePath ?: context.cacheDir.absolutePath
        }
        if (!mDataPath.endsWith(File.separator)) {
            mDataPath += File.separator
        }
    }

    private suspend fun runOnMain(action: () -> Unit) {
        withContext(Dispatchers.Main) { action() }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOcrEvent(event: OcrEvent) {
        if (event.isSuccess()) {
            mListener?.onOcrResult(event.text)
            return
        }
        mListener?.onError(event.type, event.t ?: RuntimeException("orc fail"), event.msg)
        EventBus.getDefault().unregister(this)
    }
}