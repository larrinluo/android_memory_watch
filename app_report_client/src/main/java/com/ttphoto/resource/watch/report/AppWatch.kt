package com.ttphoto.resource.watch.report

import android.util.Log
import com.ttphoto.resource.watch.sdk.AppResourceInfo
import com.ttphoto.resource.watch.sdk.utils.Utils
import java.io.File
import java.io.FileWriter
import java.lang.Exception

class AppWatch(val pid: Int, val folder: String) {

    val mFile: File
    val mWriter: FileWriter
    val startTime = System.currentTimeMillis()
    var stoped = false
    var status: UploadStatus? = null

    var lines = ArrayList<String>(10) // lines
    var lineBegin = 0

    init {
        mFile = File(String.format("%s/%d/resource_watch.log", folder, pid)).apply {
            if (!parentFile.exists()) {
                parentFile.mkdirs()
            }
        }

        mWriter = FileWriter(mFile, false)
    }

    private fun writeLine(line: String, flush: Boolean = true) {
        with(mWriter) {
            try {
                write(line)
                write("\n")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        synchronized(lines) {
            lines.add(line)
        }
    }

    fun getLines(callback: (line0: Int, lines: ArrayList<String>) -> Unit) {
        synchronized(lines) {
            callback(lineBegin, lines)
        }
    }

    fun onUploadedLines(line0: Int, lineCount: Int) {
        synchronized(lines) {
            for(i in 0 until lineCount) {
                lines.removeAt(0)
            }

            lineBegin = line0 + lineCount
        }
    }

    fun start() {
//        writeLine(AppResourceInfo.getCSVHeaderString())
    }

    fun stop() {
        Utils.closeSilently(mWriter)
        stoped = true;
        collect()
        report()
    }

    fun onUpdate(info: AppResourceInfo) {
        writeLine(info.toString())
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
