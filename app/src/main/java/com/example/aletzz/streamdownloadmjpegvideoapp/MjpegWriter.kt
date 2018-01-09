package com.example.aletzz.streamdownloadmjpegvideoapp

import  android.graphics.Bitmap
import android.util.Log

import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class MjpegWriter {

    private val mjpegFile: File? = null
    private var fos: FileOutputStream? = null
    private var bos: BufferedOutputStream? = null

    private var mRecording: Boolean = false

    fun saveByteArrayToFile(b: ByteArray) {
        Log.e(TAG, "saveByteArrayToFile")
        if (mRecording) {
            try {
                val boundaryBytes = (BOUNDARY_PART + b.size + BOUNDARY_DELTA_TIME + BOUNDARY_END).toByteArray()
                bos?.write(boundaryBytes)
                bos?.write(b)
                bos?.flush()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    fun saveBitmapToFile(bmp: Bitmap, w: Int, h: Int) {
        if (mRecording) {
            try {
                val jpegByteArrayOutputStream = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 75, jpegByteArrayOutputStream)
                val jpegByteArray = jpegByteArrayOutputStream.toByteArray()
                val boundaryBytes = (BOUNDARY_PART + jpegByteArray.size + BOUNDARY_DELTA_TIME + BOUNDARY_END).toByteArray()
                bos?.write(boundaryBytes)
                bos?.write(jpegByteArray)
                bos?.flush()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    fun recordMjpeg(path: String, nameStream: String) {
        Log.e(TAG, "recordMjpeg")


        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss")
        val sdftwo = SimpleDateFormat("yyyyMMdd")
        val currentDateAndTimeLong = sdf.format(Date())
        val currentDateAndTime = sdftwo.format(Date())

        val file = File(path + "/" + nameStream.substring(12) + currentDateAndTime)
        Log.e(TAG, file.absolutePath)
        if (!file.exists()) {
            val wasSuccessful = file.mkdirs()
            if (wasSuccessful) {
                Log.e(TAG, "CARTELLA CREATA !")
            }
        }

        val szFileName = "vid-$currentDateAndTimeLong.mjpeg"

        try {
            startRecording(file.absolutePath + "/" + szFileName)
            Log.e(TAG, "adb pull " + mjpegFile!!.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, e.message)
        }

    }

    private fun setMRunning() {
        mRecording = true
    }

    private fun startRecording(path: String) {
        Log.e("startRecording", "startRecording")
        try {
            fos = FileOutputStream(path)
            bos = BufferedOutputStream(fos!!)
            mRecording = true
            Log.e(TAG, "Recording Started")
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

    }

    fun stopRecording() {
        Log.e(TAG, "Recording Stopped")
        mRecording = false
        try {
            bos?.flush()
        } catch (e: IOException) {
            Log.e("mjpegwriter", ": " + e.toString())
        }

    }

    companion object {

        private val TAG = "MjpegWriter"

        private val BOUNDARY_PART = "\r\n\r\n--myboundary\r\nContent-Type: image/jpeg\r\nContent-Length: "
        private val BOUNDARY_DELTA_TIME = "\r\nDelta-time: 110"
        private val BOUNDARY_END = "\r\n\r\n"
    }
}