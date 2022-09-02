# Conjurer库
我将[rmtheis](https://github.com/rmtheis)的[android-ocr](https://github.com/rmtheis/android-ocr)库转为了kotlin版本并进行了以下重构：
1. 将取景框控件ViewfinderView和识别API、相机进行解耦
2. 去掉Handler和AsyncTask的写法，改为使用协程来进行异步操作
3. 将识别API封装成独立的类，方便自定义UI时直接调用
4. 增加了传入图片进行识别的功能
5. 去掉了实时预览的逻辑
6. 修复拍照后无法再自动聚焦的BUG
7. 增加了若干配置功能

## 目录
- [1、引用方式](https://github.com/LZ9/JsBridgeKt/blob/master/README_CN.md#1引用方式)
- [2、使用教程](https://github.com/LZ9/JsBridgeKt/blob/master/README_CN.md#2android端使用方式)
- [3、注意事项](https://github.com/LZ9/JsBridgeKt/blob/master/README_CN.md#2android端使用方式)
- [扩展](https://github.com/LZ9/JsBridgeKt/blob/master/README_CN.md#扩展)

## 1、引用方式
由于jcenter删库跑路，请大家添加mavenCentral依赖
```
repositories {
    ...
    mavenCentral()
    ...
}
```
在你需要调用的module里的dependencies中加入以下依赖
```
implementation 'ink.lodz:conjurer:1.0.0'
```

## 2、使用教程
Conjurer默认为你提供了两种调用方式，一种是调起相机拍照识别，另一种是传入图片异步识别。完整的调用方式如下：
```
Conjurer.create()
    .setDataPath(path)//设置训练文件存储路径，不设置默认存储在应用的沙盒目录下
    .setTrainedDataFileName("xxxxx.zip")//设置训练文件，请将自己的训练文件压缩到zip里，并放在assets目录下，工程已经内置了英文和数字的训练文件
    .setLanguage(Constant.DEFAULT_LANGUAGE)//设置识别语言，不设置默认英文
    .setEngineMode(TessBaseAPI.OEM_TESSERACT_ONLY)//设置识别引擎模式，不设置默认TessBaseAPI.OEM_TESSERACT_ONLY
    .setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_OSD)//设置页面分段模式，不设置默认TessBaseAPI.PageSegMode.PSM_AUTO_OSD
    .setBlackList("")//设置识别黑名单
    .setWhiteList("0123456789")//设置识别白名单，必须设置要识别的值，否则无法识别出想要的结果
    .addOcrResultTransformer(XXXTransformer())//添加识别结果转换器，可通过自定义算法对识别结果进行优化，提升识别率
    .setOnConjurerListener(object :OnConjurerListener{//添加识别监听器
        override fun onInit(status: InitStatus) {}//初始化状态回调
        override fun onOcrResult(bean: OcrResultBean) {}//识别结果回调
        override fun onError(type: Int, t: Throwable, msg: String) {}//异常回调
    })
    .openCamera(getContext())//调起相机拍照识别
    //.recogAsync(getContext(), bitmap)//传入图片异步识别
```
- 白名单setWhiteList(XXXX)一定要设置，否则基本识别不出来


## 3、注意事项
目前OCR的开源库仍然存在较多的限制，若在项目中使用须注意以下情况：
#### 1）OCR识别率较低的场景：
- 手写体文字（基本识别不出来）
- 复杂背景（例如文字的背景存在很多线条或花纹，会干扰识别）
- 文字存在高亮色差时识别率低（基本只能识别到高亮色的文字，其他文字识别不到）
- 文字颜色和背景颜色十分相近（混 ~ 合 ~ 在 ~ 一 ~ 起 ~ ）
- 同时对中文、英文、数字等多种类文字进行混合识别，例如车牌（**每一次**的识别结果都不是你想要的）

#### 2）OCR识别率较高的场景：
- 纯色背景和纯色印刷体文字（例如白底黑字）


## 扩展

- [更新记录](https://github.com/LZ9/JsBridgeKt/blob/master/jsbridgekt/readme_update.md)
- [回到顶部](https://github.com/LZ9/JsBridgeKt/blob/master/README_CN.md#jsbridgekt库)
