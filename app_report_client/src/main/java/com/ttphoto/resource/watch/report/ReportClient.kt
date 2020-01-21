package com.ttphoto.resource.watch.report

import android.util.Log
import com.ttphoto.resource.watch.report.utils.HttpUtil
import com.ttphoto.resource.watch.sdk.utils.FileUtil
import org.json.JSONObject
import java.lang.Exception
import java.lang.StringBuilder
import kotlin.collections.HashMap

/**
 * 实现所有上传逻辑
 * 1. ReportClient在自己的现场中运行, 不影响正常性能数据抓取
 */
class ReportClient {

    companion object {

        var reportServerUrl = "http://192.168.1.16:8000/report"
        var currentApp: AppWatch? = null
        var running = true
        var stopSignal = Object()

        fun reportUrl(service: String): String {
            return String.format("%s%s", reportServerUrl, service)
        }

        fun start() {
            object : Thread() {
                override fun run() {
                    reportLoop()
                }
            }.start()
        }

        fun stop() {
            synchronized(stopSignal) {
                running = false
                stopSignal.notifyAll()
            }
        }

        fun onAppStart(app: AppWatch) {
            updateAppUpdateStatus(app, -1, false)
            currentApp = app
        }

        fun updateAppUpdateStatus(app: AppWatch, line: Int, completed: Boolean) {
            with(JSONObject()) {
                put("pid", app.pid)
                put("upload_id", 0)
                put("start_time", app.startTime)
                put("resource_upload_line", line)
                put("resource_upload_completed", completed)
                val jsonString = toString()
                FileUtil.writeStringToFile(String.format("%s/%d/upload_stat.json", app.folder, app.pid), jsonString)
            }
        }

        fun onAppStoped() {
            currentApp = null
        }

        fun reportLoop() {
            while (running) {

                reportLive()

                synchronized(stopSignal) {
                    stopSignal.wait(10000)  // 30秒check一次
                }
            }
        }

        /**
         * 实时监控上报
         */
        fun reportLive() {

            currentApp?.let {

                var app = it

                val performanceReportInfo = LiveReportLinesInfo("perf", it.performanceLines)
                val slowMessageReportInfo = LiveReportLinesInfo("slow", it.slowMessageLines)
                var traceVersion = 0

                Log.d("ReportClient", String.format("%d %d\n%s", performanceReportInfo.lineBegin,
                    performanceReportInfo.lineCount, performanceReportInfo.lineString))

                val params = HashMap<String, String>(3).apply {
                    //收集上报信息

                    performanceReportInfo.fillUploadData(this)
                    slowMessageReportInfo.fillUploadData(this)

                    app.getAnrTrace { traces, version ->
                        if (traces != null) {
                            this["trace"] = traces
                            traceVersion = version
                        }
                    }

                    app.javaUncachedException?.let {
                        this["jExcp"] = it
                    }
                }

                if (params.isNotEmpty()) {

                    // send to server
                    if (app.status == null) {
                        // need to build upload session
                        var status = query_live(0, it.pid, it.startTime)
                        if (status != null) {
                            it.status = status
                        }
                    }

                    app.status?.let {
                        try {

                            params["req"] = "live"
                            params["upload_id"] = it.upload_id.toString()
                            params["pid"] = it.pid.toString()

                            // 实时上传接口
                            var result = HttpUtil.postWithStatusCode(reportUrl("/upload_live"), params, true)
                            if (result == 0) { // 上传成功

                                if (params.containsKey("jExcep")) {
                                    app.javaUncachedException = null
                                }

                                if (params.containsKey("trace")) {
                                    app.reportedTraceVersion = traceVersion
                                }

                                // update upload status
                                updateAppUpdateStatus(app, performanceReportInfo.lineEnd, false)

                                if (performanceReportInfo.lineCount > 0) {
                                    app.performanceLines.onUploadedLines(
                                        performanceReportInfo.lineBegin,
                                        performanceReportInfo.lineCount
                                    )
                                }

                                if (slowMessageReportInfo.lineCount > 0) {
                                    app.slowMessageLines.onUploadedLines(
                                        slowMessageReportInfo.lineBegin,
                                        slowMessageReportInfo.lineCount
                                    )
                                }

                            } else {
                                // 终止app的实时上传
                                // currentApp = null
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        /**
         * 查询服务端上传状态 - 实时上传
         */
        fun query_live(upload_id: Int, pid: Int, start_time: Long): UploadStatus? {
            var params = HashMap<String, Any>().apply {
                if (upload_id != 0)
                    this["id"] = upload_id

                this["pid"] = pid
                this["start_time"] = start_time
            }

            try {
                val response = HttpUtil.getHttpResponse(reportUrl("/query_live"), params, true)
                var json = JSONObject(response)
                var status = UploadStatus.fromJson(json)
                if (status != null) {
                    if (status.pid != pid || status.start_time != start_time || (upload_id != 0 && upload_id != status.upload_id)) {
                        status = null // 服务器的信息与我们的信息不匹配
                    }
                }

                return status
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return null
        }

        /**
         * 上传ResourceWatch数据到服务器
         */
        fun upload_resource_live(upload_id: Int, pid: Int, start_time: Long, line0: Int, line1: Int, lines: String): Int {
            var params = HashMap<String, String>().apply {
                this["req"] = "resources"
                this["upload_id"] = upload_id.toString()
                this["pid"] = pid.toString()
                this["start_time"] = start_time.toString()
                this["line0"] = line0.toString()
                this["line1"] = line1.toString()
                this["lines"] = lines
            }

            try {
                // 实时上传接口
                var result = HttpUtil.postWithStatusCode(reportUrl("/upload_live"), params, true)
                if (result == 200)
                    return 0;
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return -1
        }
    }
}

class LiveReportLinesInfo(val reportName: String, val reportLines: LiveReportLines) {
    var lineBegin = 0
    var lineCount = 0
    var lineString = ""
    var lineEnd = 0

    init {
        reportLines.getLines { line0, lines ->
            lineBegin = line0
            lineCount = lines.size

            var builder = StringBuilder()
            for (line in lines) {
                builder.append(line)
                builder.append("\n")
            }

            lineString = builder.toString()
            lineEnd = lineBegin + lineCount - 1
        }
    }

    fun fillUploadData(map: HashMap<String, String>) {
        if (lineCount > 0) {
            map[reportName + "_line0"] = lineBegin.toString()
            map[reportName + "_line1"] = lineEnd.toString()
            map[reportName + "_lines"] = lineString
        }
    }
}
