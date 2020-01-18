package com.ttphoto.android.resources.watch

import android.app.Application
import android.os.Looper
import com.ttphoto.resource.watch.report.Report
import com.ttphoto.resource.watch.sdk.utils.Utils

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if (PermissionManager.check(this)) {
            Report.start("watch", "/sdcard/app_watch",Config.reportUrl,this)
            Report.startWatchMainLooper()
        }

        if (Utils.isMainProcess()) {
        }
    }
}