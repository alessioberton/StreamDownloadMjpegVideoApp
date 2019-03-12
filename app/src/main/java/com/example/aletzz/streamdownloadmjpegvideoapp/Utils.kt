package com.example.aletzz.streamdownloadmjpegvideoapp

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.os.Environment
import android.support.v4.content.ContextCompat
import android.support.design.widget.Snackbar
import java.util.*
import android.util.Log

internal class Utils {

    class Util {
        companion object {
            fun createUUID(): String = UUID.randomUUID().toString()

            fun isConnectedToInternet(context: Context): Boolean {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                return connectivityManager.activeNetworkInfo != null && connectivityManager.activeNetworkInfo.isConnectedOrConnecting
            }

            fun showSnackBar(act: Activity, text: String) {
                Snackbar.make(act.findViewById(android.R.id.content), text, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    class Video {
        companion object {
            fun getRootDirPath(context: Context): String {
                if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
                    val file = ContextCompat.getExternalFilesDirs(context.applicationContext, null)[0]
                    Log.e("getRootDirPathIF", file.absolutePath)
                    return file.absolutePath
                } else {
                    Log.e("getRootDirPathELSE", context.applicationContext.filesDir.absolutePath)
                    return context.applicationContext.filesDir.absolutePath
                }
            }

            fun getRootDirPathSD(): String? {
                return System.getenv("EXTERNAL_STORAGE");
            }
        }
    }
}