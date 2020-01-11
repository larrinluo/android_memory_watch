package com.ttphoto.android.resources.watch

import android.app.Application
import com.ttphoto.resource.watch.report.Report

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if (PermissionManager.check(this)) {
            Report.start("watch", "/sdcard/app_watch",Config.reportUrl,this)
        }

    }
}