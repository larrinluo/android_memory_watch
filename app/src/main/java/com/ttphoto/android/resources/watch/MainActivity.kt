package com.ttphoto.android.resources.watch

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.ttphoto.resource.watch.sdk.services.AppResourceWatchClient
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //How to use:
        AppResourceWatchClient.start(this)

        cpuTestThread.start();
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
