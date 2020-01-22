package com.ttphoto.resource.watch.report

import android.os.FileObserver
import android.os.SystemClock
import android.util.Log
import com.ttphoto.resource.watch.sdk.AppPerformanceInfo
import com.ttphoto.resource.watch.sdk.utils.FileUtil
import com.ttphoto.resource.watch.sdk.utils.Utils
import java.io.File
import java.io.FileWriter

class LiveReportLines(initSize : Int) {
    var lines = ArrayList<String>(initSize) // lines
    var lineBegin = 0

    fun addLine(line: String) {
        synchronized(this) {
            lines.add(line)
        }
    }

    fun onUploadedLines(line0: Int, lineCount: Int) {
        synchronized(this) {
            for(i in 0 until lineCount) {
                lines.removeAt(0)
            }

            lineBegin = line0 + lineCount
        }
    }

    fun getLines(callback: (line0: Int, lines: ArrayList<String>) -> Unit) {
        synchronized(this) {
            callback(lineBegin, lines)
        }
    }
}

class AppWatch(val pid: Int, val folder: String) {

    private val mPerformanceFile: File = File(String.format("%s/%d/performance.log", folder, pid)).apply {
        if (!parentFile.exists()) {
            parentFile.mkdirs()
        }
    }

    private val mPerformanceWriter: FileWriter
    val startTime = System.currentTimeMillis()
    var stoped = false

    val TAG = "APP_WATCH"

    /**
     * 实时监控上报数据
     */
    var status: UploadStatus? = null

    val performanceLines = LiveReportLines(10)
    val slowMessageLines = LiveReportLines(4)
    var traceVersion = 0
    var lastTraceTime = 0L
    var reportedTraceVersion = 0

    var javaUncachedException: String? = null
        set(value) {
            synchronized(this) {
                field = value
            }
        }
        get() {
            synchronized(this) {
                return field
            }
        }

    init {
        mPerformanceWriter = FileWriter(mPerformanceFile, false)
    }

    private var fileObserver = object : FileObserver(String.format("%s/%d", folder, pid), FileObserver.CLOSE_WRITE) {

        override fun onEvent(event: Int, path: String?) {
            if (event and FileObserver.CLOSE_WRITE != 0) {
                path?.let {
                    if (it.compareTo("traces.txt") == 0) {
                        val tm = SystemClock.elapsedRealtime()
                        if (tm - lastTraceTime > 1000) {
                            lastTraceTime = tm
                            traceVersion++
                            Log.d(TAG, String.format("ANR_WARN: found anr(%d), trace file /%s/%s/traces.txt", traceVersion, folder, pid))
                        }
                    } else if (it.compareTo("exception.txt") == 0) {
                        Log.d(TAG, String.format("Java exception found, exception file /%s/%s/exception.txt", folder, pid))
                    }
                }
            }
        }
    }

    fun start() {
        fileObserver.startWatching()
    }

    fun stop() {
        fileObserver.stopWatching()
        Utils.closeSilently(mPerformanceWriter)
        stoped = true
        collect()
        report()
    }

    private fun writePerformanceLine(line: String, flush: Boolean = true) {
        with(mPerformanceWriter) {
            try {
                write(line)
                write("\n")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        performanceLines.addLine(line)
    }

    fun onUpdate(info: AppPerformanceInfo) {
        writePerformanceLine(info.toString())
    }

    private fun traceFile(): String {
        return String.format("%s/%d/traces.txt", folder, pid)
    }

    fun getAnrTrace(callback: (traces: String?, traceVersion: Int) -> Unit) {
        synchronized(this) {
            var trace: String? = null
            if (traceVersion > reportedTraceVersion) {
                 trace = FileUtil.readStringFromFile(traceFile())
            }

            callback(trace, traceVersion)
        }
    }

    fun onAnr() {
        traceVersion++
        Log.d(TAG, "ANR_WARN: anr warning[" + traceVersion + "], trace dumped to " + traceFile())
    }

    fun onMessageSlow(message: Int, delay: Long, dispatch: Long) {
        val slowMessagFile = String.format("%s/%d/slowMessage.txt", folder, pid)
        val txt = String.format("%d\t%d\t%d\n", message, delay, dispatch)
        Log.d(TAG, "SLOW_MESG: foud slow message-->" + txt)
        FileUtil.appendStringToFile(slowMessagFile, txt)
        slowMessageLines.addLine(txt)
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
