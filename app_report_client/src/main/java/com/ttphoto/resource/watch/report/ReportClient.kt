package com.ttphoto.resource.watch.report

import android.content.Context
import android.util.Log
import com.ttphoto.resource.watch.sdk.AppResourceInfo
import com.ttphoto.resource.watch.sdk.IAppWatchCallback
import com.ttphoto.resource.watch.sdk.services.AppResourceWatchClient
import com.ttphoto.resource.watch.sdk.services.AppResourceWatchService
import com.ttphoto.resource.watch.sdk.utils.Utils
import java.io.File

class ReportClient {

    companion object {

        var running = false
            set

        fun start(processName: String, root: String, context: Context) {

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
                        mCurrentApp = AppWatch(pid, "/sdcard/app_watch")
                        mCurrentApp?.start()
                    }

                    override fun onAppExist(pid: Int) {
                        mCurrentApp?.stop()
                    }

                    override fun onUpdate(info: AppResourceInfo) {
                        mCurrentApp?.onUpdate(info)
                    }
                })
            }
        }
    }
}