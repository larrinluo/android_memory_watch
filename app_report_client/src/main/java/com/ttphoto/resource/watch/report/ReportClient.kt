package com.ttphoto.resource.watch.report

import android.util.Log
import com.ttphoto.resource.watch.report.utils.FileUtil
import com.ttphoto.resource.watch.report.utils.HttpUtil
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

                currentApp?.let {

                    var app = it
                    var lineBegin = -1
                    var lineCount = 0
                    var lineString = ""

                    it.getLines {line0, lines ->
                        lineBegin = line0
                        lineCount = lines.size

                        var builder = StringBuilder()
                        for (line in lines) {
                            builder.append(line)
                            builder.append("\n")
                        }

                        lineString = builder.toString()
                    }

                    Log.d("ReportClient", String.format("%d %d\n%s", lineBegin, lineCount, lineString))

                    if (lineCount > 0 && lineString.length > 0) {

                        // send to server
                        if (app.status == null) {
                            // need to build upload session
                            var status = query_upload(0, it.pid, it.startTime)
                            if (status != null) {
                                it.status = status
                            }
                        }

                        app.status?.let {
                            try {
                                var lineEnd = lineBegin + lineCount - 1
                                if (upload_resource_lines(
                                        it.upload_id,
                                        it.pid,
                                        it.start_time,
                                        lineBegin,
                                        lineEnd,
                                        lineString
                                    ) == 0
                                ) {
                                    updateAppUpdateStatus(app, lineEnd, false)
                                    app.onUploadedLines(lineBegin, lineCount)
                                } else { // 终止app的实时上传
                                    //                                currentApp = null
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }

                synchronized(stopSignal) {
                    stopSignal.wait(10000)  // 30秒check一次
                }
            }
        }

        /**
         * 查询服务端上传状态
         */
        fun query_upload(upload_id: Int, pid: Int, start_time: Long): UploadStatus? {
            var params = HashMap<String, Any>().apply {
                if (upload_id != 0)
                    this["id"] = upload_id

                this["pid"] = pid
                this["start_time"] = start_time
            }

            try {
                val response = HttpUtil.getHttpResponse(reportUrl("/query_upload"), params, true)
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
        fun upload_resource_lines(upload_id: Int, pid: Int, start_time: Long, line0: Int, line1: Int, lines: String): Int {
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
                var result = HttpUtil.postWithStatusCode(reportUrl("/upload"), params, true)
                if (result == 200)
                    return 0;
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return -1
        }
    }
}

