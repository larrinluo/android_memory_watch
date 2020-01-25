// MIT License
//
// Copyright (c) 2019 larrinluo
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//
// Created by larrin luo on 2020-01-11.
//

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