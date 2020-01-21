package com.ttphoto.android.resources.watch

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.ttphoto.resource.watch.report.Report
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        if (!Report.running) {

            if (PermissionManager.check(this)) {
                Report.start("watch", "/sdcard/app_watch", Config.reportUrl,this)
                Report.startWatchMainLooper()
                cpuTestThread.start()
            } else {
                PermissionManager.request(this, 1000)
            }
        } else {
            cpuTestThread.start()
        }

        findViewById<View>(R.id.Button_ANR).setOnClickListener {
            Process.sendSignal(Process.myPid(), Process.SIGNAL_QUIT)
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

            val handler = Handler(Looper.getMainLooper())
            handler.post(object: Runnable {
                override fun run() {
                    sleep(10000)
                    Log.d("SLOW_TEST", "Slow message ...")
                    handler.post(Runnable@this)
                }
            })

            while (true) {
                sleep(3000);

                for (i in 0..1000) {
                    var str = String.format("this is string %d, %d , %d, %d", i, i, i, i);
                    for (j in 0..10000) {
                        sqrt(i.toDouble()) * sqrt(i.toDouble()) * sqrt(i.toDouble()) * sqrt(i.toDouble())
                    }
//                    Log.d("cpuTestThread", str);
                }

                bytes *= 2
                if (bytes > 20 * 1024 * 1024)
                    bytes = 1024 * 1024

                buffer = ByteArray(bytes.toInt())
            }
        }
    }

}
