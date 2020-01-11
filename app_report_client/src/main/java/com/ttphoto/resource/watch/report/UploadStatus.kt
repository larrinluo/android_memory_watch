package com.ttphoto.resource.watch.report

import org.json.JSONObject
import java.lang.Exception

/**
 * 上传状态信息
 */
class UploadStatus {
    var pid = 0
    var start_time = 0L
    var upload_id = 0
    var retry = 0
    var resource_upload_line = -1
    var resource_upload_completed = false
    var traces_completed = false
    var tombstones_completed = false
    var exception_completed = false
    var native_crash_completed = false
    var events_completed = false


    companion object {
        fun fromJson(json: JSONObject): UploadStatus? {

            var status = UploadStatus()
            try {
                status.pid = json.getInt("pid")
                status.start_time = json.getLong("start_time")
                status.upload_id = json.getInt("upload_id")
                status.resource_upload_line = json.getInt("resource_upload_line")
                status.resource_upload_completed = json.getInt("resource_upload_completed") != 0

                status.traces_completed = json.optBoolean("traces_completed")
                status.tombstones_completed = json.optBoolean("tombstones_completed")
                status.exception_completed = json.optBoolean("exception_completed")
                status.native_crash_completed = json.optBoolean("native_crash_completed")
                status.events_completed = json.optBoolean("events_completed")

                return status

            } catch (e: Exception) {
                e.printStackTrace()
            }

            return null
        }
    }
}