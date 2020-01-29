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

import android.content.Context
import android.os.Looper
import com.ttphoto.resource.watch.sdk.AppPerformanceInfo
import com.ttphoto.resource.watch.sdk.services.IAppWatchCallback
import com.ttphoto.resource.watch.sdk.client.AppWatchClient
import com.ttphoto.resource.watch.sdk.services.AppResourceWatchService
import com.ttphoto.resource.watch.sdk.utils.Utils
import java.io.File

class Report {

    companion object {

        var running = false
            set

        fun start(processName: String, root: String, reportUrl: String, context: Context) {

            ReportClient.reportServerUrl = reportUrl
            running = true

            var dir = File(root)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            if (Utils.isMainProcess()) { // Start watch service in remote process

                AppWatchClient.start(context, ".*\\libappWatch.so$")

            } else if (Utils.isWatchProcess(processName)) { // set callback in watch process

                AppResourceWatchService.setWatchCallback(object :
                    IAppWatchCallback {

                    var mCurrentApp: AppWatch? = null

                    override fun onAppStart(pid: Int) {
                        mCurrentApp = AppWatch(pid,"/sdcard/app_watch").apply {
                            start()
                            ReportClient.onAppStart(this)
                        }
                    }

                    override fun onAppExist(pid: Int) {
                        mCurrentApp?.stop()
                        ReportClient.onAppStoped()
                    }

                    override fun onUpdate(info: AppPerformanceInfo) {
                        mCurrentApp?.onUpdate(info)
                    }

                    override fun onAnr(pid: Int) {
                        mCurrentApp?.let {
                            if (pid == it.pid) {
                                it.onAnr()
                            }
                        }
                    }

                    override fun onMessageSlow(pid: Int, message: Int, delay: Long, dispatch: Long) {
                        mCurrentApp?.let {
                            if (pid == it.pid) {
                                it.onMessageSlow(message, delay, dispatch)
                            }
                        }
                    }
                })

                ReportClient.start()
            }
        }

        fun startWatchMainLooper(anrTimeout: Int = 5000) {
            AppWatchClient.startMainLooper(anrTimeout, Looper.getMainLooper())
        }

        fun startBatchUploadTask() {
        }

    } // companion
}