package com.ttphoto.android.resources.watch

import android.os.Bundle
import android.os.Handler
import android.os.Process
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.ttphoto.resource.watch.sdk.ProcessResourceInfo

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        start_watch()
    }

    private fun start_watch() {
        val handler = Handler()
        val runnable = object: Runnable {
            override fun run() {
                val resourceInfo = ProcessResourceInfo.dump(this@MainActivity, Process.myPid())
                Log.d("ProcessResource", resourceInfo.toString())
                handler.postDelayed(this, 5000)
            }
        }

        handler.postDelayed(runnable, 5000)
    }

}
