package com.ttphoto.android.resources.watch

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.ttphoto.resource.watch.sdk.services.AppResourceWatchClient

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //How to use:
        AppResourceWatchClient.start(this)
    }
}
