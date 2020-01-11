package com.ttphoto.android.resources.watch

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.ttphoto.resource.watch.report.ReportClient
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!ReportClient.running) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                ReportClient.start("watch", "/sdcard/app_watch",this)
                cpuTestThread.start();
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1000)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            ReportClient.start("watch", "/sdcard/app_watch",this)
            cpuTestThread.start();
        }
    }

    val cpuTestThread = object: Thread() {
        override fun run() {
            while (true) {
                sleep(3000);
                for (i in 0..1000) {
                    var str = String.format("this is string %d, %d , %d, %d", i, i, i, i);
                    for (j in 0..10000) {
                        sqrt(i.toDouble()) * sqrt(i.toDouble()) * sqrt(i.toDouble()) * sqrt(i.toDouble())
                    }
                    Log.d("cpuTestThread", str);
                }
            }
        }
    }
}
