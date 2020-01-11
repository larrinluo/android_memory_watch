package com.ttphoto.resource.watch.report

import com.ttphoto.resource.watch.sdk.AppResourceInfo
import com.ttphoto.resource.watch.sdk.utils.Utils
import java.io.File
import java.io.FileWriter
import java.lang.Exception

class AppWatch(val pid: Int, val folder: String) {

    val mFile: File
    val mWriter: FileWriter

    init {
        mFile = File(String.format("%s/resource_%d.log", folder, pid))
        val parent = mFile.parentFile
        if (!parent.exists()) {
            parent.mkdirs()
        }
        mWriter = FileWriter(mFile, true)
    }

    fun start() {
        try {
            mWriter.write(AppResourceInfo.getCSVHeaderString())
            mWriter.write("\n")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        Utils.closeScilently(mWriter)
        collect()
        report()
    }

    fun onUpdate(info: AppResourceInfo) {
        try {
            mWriter.write(info.toString())
            mWriter.write("\n")
            mWriter.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 收集进程相关信息
     * 1. trace - ANR
     * 2. tombstone
     * 3. dropbox
     */
    fun collect() {
    }

    /**
     * 上传到服务器
     */
    fun report() {
    }
}
