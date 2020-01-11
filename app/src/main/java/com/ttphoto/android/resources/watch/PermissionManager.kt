package com.ttphoto.android.resources.watch

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import com.ttphoto.resource.watch.report.Report

class PermissionManager {

    companion object {
        fun check(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED
        }

        fun request(activity: Activity, requestCode: Int) {
            var permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET)
            ActivityCompat.requestPermissions(activity, permissions, requestCode)
        }

        fun checkResult(requestCode: Int, grantResults: IntArray): Boolean {
            if (requestCode == 1000 && grantResults.size == 2 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                return true
            }

            return false
        }
    }
}