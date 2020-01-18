package com.ttphoto.resource.watch.report

import android.util.Log
import com.ttphoto.resource.watch.sdk.AppResourceInfo
import com.ttphoto.resource.watch.sdk.IAppResourceWatchClient
import com.ttphoto.resource.watch.report.utils.DumpUtil
import com.ttphoto.resource.watch.sdk.utils.FileUtil
import com.ttphoto.resource.watch.sdk.utils.Utils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
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

    val mPerformanceFile: File
    val mPerformanceWriter: FileWriter
    val startTime = System.currentTimeMillis()
    var stoped = false

    /**
     * 实时监控上报数据
     */
    var status: UploadStatus? = null

    val performanceLines = LiveReportLines(10)
    val slowMessageLines = LiveReportLines(4)
    var traceVersion = 0
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
        mPerformanceFile = File(String.format("%s/%d/resource_watch.log", folder, pid)).apply {
            if (!parentFile.exists()) {
                parentFile.mkdirs()
            }
        }

        mPerformanceWriter = FileWriter(mPerformanceFile, false)
    }

    fun start() {
    }

    fun stop() {
        Utils.closeSilently(mPerformanceWriter)
        stoped = true;
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

    fun onUpdate(info: AppResourceInfo) {
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

    fun onAnrWarning(client: IAppResourceWatchClient, message: Int, delay: Long, timeout: Long) {
        GlobalScope.async {
            synchronized(AppWatch@this) {
                if (DumpUtil.dumpTrace(client, pid, timeout, traceFile())) {
                    traceVersion++
                    Log.d("ANR_WARN", "anr warning[" + traceVersion + "], trace dumped to " + traceFile())
                } else {
                    Log.d("ANR_WARN", "trace dumped failed!!!")
                }
            }
        }
    }

    fun onMessageSlow(message: Int, delay: Long, dispatch: Long) {
        val slowMessagFile = String.format("%s/%d/slowMessage.txt", folder, pid)
        val txt = String.format("%d\t%d\t%d\n", message, delay, dispatch)
        Log.d("SLOW_MESG", "foud slow message-->" + txt)
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
