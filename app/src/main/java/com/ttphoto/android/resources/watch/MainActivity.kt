package com.ttphoto.android.resources.watch

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.ttphoto.resource.watch.report.Report
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!Report.running) {

            if (PermissionManager.check(this)) {
                Report.start("watch", "/sdcard/app_watch", Config.reportUrl,this)
                cpuTestThread.start();
            } else {
                PermissionManager.request(this, 1000)
            }
        } else {
            cpuTestThread.start();
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (PermissionManager.checkResult(requestCode, grantResults)) {
            Report.start("watch", "/sdcard/app_watch", Config.reportUrl, this)
            cpuTestThread.start();
        }
    }

    val cpuTestThread = object: Thread() {
        override fun run() {
            var buffer: ByteArray? = null
            var bytes = 1024 * 1024
            while (true) {
                sleep(3000);
                for (i in 0..1000) {
                    var str = String.format("this is string %d, %d , %d, %d", i, i, i, i);
                    for (j in 0..10000) {
                        sqrt(i.toDouble()) * sqrt(i.toDouble()) * sqrt(i.toDouble()) * sqrt(i.toDouble())
                    }
                    Log.d("cpuTestThread", str);
                }

                bytes *= 2
                if (bytes > 20 * 1024 * 1024)
                    bytes = 1024 * 1024

                buffer = ByteArray(bytes.toInt())
            }
        }
    }
}
