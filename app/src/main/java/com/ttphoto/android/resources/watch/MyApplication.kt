package com.ttphoto.android.resources.watch

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import com.ttphoto.resource.watch.report.ReportClient

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            ReportClient.start("watch", "/sdcard/app_watch",this)
        }

    }
}