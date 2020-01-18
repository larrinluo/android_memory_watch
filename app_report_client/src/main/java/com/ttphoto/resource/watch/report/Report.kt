package com.ttphoto.resource.watch.report

import android.content.Context
import com.ttphoto.resource.watch.sdk.AppResourceInfo
import com.ttphoto.resource.watch.sdk.IAppResourceWatchClient
import com.ttphoto.resource.watch.sdk.IAppWatchCallback
import com.ttphoto.resource.watch.sdk.client.AppResourceWatchClient
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

                AppResourceWatchClient.start(context)

            } else if (Utils.isWatchProcess(processName)) { // set callback in watch process

                AppResourceWatchService.setWatchCallback(object : IAppWatchCallback {

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

                    override fun onUpdate(info: AppResourceInfo) {
                        mCurrentApp?.onUpdate(info)
                    }

                    override fun onAnrWarnning(client: IAppResourceWatchClient, pid: Int, message: Int, delay: Long, timeout: Long) {
                        mCurrentApp?.let {
                            if (pid == it.pid) {
                                it.onAnrWarning(client, message, delay, timeout)
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
            AppResourceWatchClient.startWartchMainLooper(anrTimeout)
        }

        fun startBatchUploadTask() {
        }

    } // companion
}