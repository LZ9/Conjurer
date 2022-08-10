package com.lodz.android.conjurer.util

import android.content.Context
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * OCR帮助类
 * @author zhouL
 * @date 2022/8/9
 */
object OcrUtils {

    /** 按照Assets里的ZIP压缩内容，上下文[context]，根路径[rootPath]，压缩包名称[zipFileName] */
    fun installZipFromAssets(context: Context, rootPath: String, zipFileName: String): Pair<Boolean, Throwable?> {
        if (!zipFileName.endsWith(".zip")) {
            return Pair(false, IllegalArgumentException("trained data is not zip file"))
        }
        try {
            ZipInputStream(context.assets.open(zipFileName)).use { zis ->
                var hasEntry = true
                while (hasEntry) {
                    val entry: ZipEntry? = zis.nextEntry
                    if (entry == null){
                        hasEntry = false
                        continue
                    }
                    val file = File(rootPath + entry.name)
                    if (file.exists()) {//若文件已经存在则不处理
                        continue
                    }
                    if (file.isDirectory) {
                        file.mkdirs()
                        continue
                    }
                    FileOutputStream(file).use { fos ->
                        val buffer = 8192
                        BufferedOutputStream(fos, buffer).use { bos ->
                            var count = 0
                            val data = ByteArray(buffer)
                            while (zis.read(data, 0, buffer).also { count = it } != -1) {
                                bos.write(data, 0, count)
                            }
                        }
                    }
                }
            }
            return Pair(true, null)
        } catch (e: Exception) {
            e.printStackTrace()
            return Pair(false, e)
        }
    }

    /** 删除路径[filePath]的文件 */
    fun delFile(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            return
        }

        try {
            if (file.isDirectory) {
                val files: Array<File>? = file.listFiles()
                if (files == null || files.isEmpty()) {
                    file.delete()
                    return
                }

                for (f in files) {
                    if (f.isFile) {
                        f.delete()
                    } else if (f.isDirectory) {
                        delFile(f.absolutePath)
                    }
                }
                file.delete()
            } else if (file.isFile) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}