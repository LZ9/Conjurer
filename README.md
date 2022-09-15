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
- [1、引用方式](https://github.com/LZ9/Conjurer#1引用方式)
- [2、内部依赖库](https://github.com/LZ9/Conjurer#2内部依赖库)
- [3、使用教程](https://github.com/LZ9/Conjurer#3使用教程)
- [4、注意事项](https://github.com/LZ9/Conjurer#4注意事项)
- [扩展](https://github.com/LZ9/Conjurer#扩展)

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

## 2、内部依赖库
Conjurer引用了OCR的开源库，你可以确认你的项目，排除重复的引用。
```
    dependencies {
        api 'com.rmtheis:tess-two:9.1.0'
    }
```

## 3、使用教程
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
    .addOcrResultTransformer(XXXTransformer())//添加识别结果转换器，可通过自定义的算法对识别结果进行优化，提升识别率
    .setOnConjurerListener(object :OnConjurerListener{//添加识别监听器
        override fun onInit(status: InitStatus) {}//初始化状态回调
        override fun onOcrResult(bean: OcrResultBean) {}//识别结果回调
        override fun onError(type: Int, t: Throwable, msg: String) {}//异常回调
    })
    .openCamera(getContext())//调起相机拍照识别
    //.recogAsync(getContext(), bitmap)//传入图片异步识别，除了bitmap也支持base64传入图片
```
- 白名单setWhiteList(XXXX)切勿设置空字符串，否则识别不出结果
- 白名单不设置时的默认值：Constant.DEFAULT_WHITELIST
- 黑名单不设置时的默认值：Constant.DEFAULT_BLACKLIST
- 训练文件用zip压缩时，请不要在最外层用文件夹包裹，避免解压错误
- 请根据实际业务需要到[tessdata](https://github.com/tesseract-ocr/tessdata/tree/3.04.00)下载对应语言的训练包
- 如果有添加识别结果转换器addOcrResultTransformer()，相机拍照的识别结果页面会展示转换器处理过后的内容，并非原始识别内容
- 我简单实现了默认英文识别、身份证识别、中文大写数字金额识别和手机号识别的用例，具体调用方法可以参考[demo](https://github.com/LZ9/Conjurer/blob/master/app/src/main/java/com/lodz/android/conjurerdemo/MainActivity.kt)

## 4、注意事项
目前OCR的开源库仍然存在较多的限制，若在项目中使用须注意以下情况：
##### 1）OCR识别率较低的场景：
- 手写体文字（基本识别不出来）
- 复杂背景（例如文字的背景存在很多线条或花纹，会干扰识别）
- 文字存在高亮色差时识别率低（基本只能识别到高亮色的文字，其他文字识别不到）
- 文字颜色和背景颜色十分相近（混合在一起识别率较低）
- 同时对中文、英文、数字等多种类文字进行混合识别，例如车牌（**毫无识别率可言**）

##### 2）OCR识别率较高的场景：
- 识别纯色背景和纯色印刷体文字（例如白底黑字）
- 较大较清晰的文字识别率较高
- 识别单一的文字类型，例如纯数字、纯字母、在白名单范围内的纯中文
- 通过添加识别结果转换器可以用算法（例如特定结果验证或者正则表达式）来提升识别率
- 启动相机识别时，要尽量将所要识别的内容充满识别框内，这样识别率高
- 使用图片识别时，要提前对图片进行裁剪，将要识别的区域裁剪出来后再传入，这样识别率高

## 扩展

- [更新记录](https://github.com/LZ9/Conjurer/blob/master/conjurer/readme_update.md)
- [回到顶部](https://github.com/LZ9/Conjurer#conjurer库)

## License
- [Apache Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

Copyright 2022 Lodz

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.