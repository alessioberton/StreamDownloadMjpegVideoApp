package com.example.aletzz.streamdownloadmjpegvideoapp

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory

import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Properties

class MjpegInputStream(`in`: InputStream) : DataInputStream(BufferedInputStream(`in`, FRAME_MAX_LENGTH)) {
    private val SOI_MARKER = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
    private val EOF_MARKER = byteArrayOf(0xFF.toByte(), 0xD9.toByte())
    private val CONTENT_LENGTH = "Content-Length"
    private var mContentLength = -1

    interface MJpegListener {
        fun onStream(stream: MjpegInputStream)
    }

    private class MyRunnable internal constructor(internal var inputStream: InputStream, internal var sListener: MJpegListener) : Runnable {
        override fun run() {
            sListener.onStream(MjpegInputStream(inputStream))
        }
    }

    @Throws(IOException::class)
    private fun getEndOfSeqeunce(`in`: DataInputStream, sequence: ByteArray): Int {

        var seqIndex = 0
        var c: Byte
        for (i in 0 until FRAME_MAX_LENGTH) {
            c = `in`.readUnsignedByte().toByte()
            if (c == sequence[seqIndex]) {
                seqIndex++
                if (seqIndex == sequence.size) return i + 1
            } else
                seqIndex = 0
        }
        return -1
    }

    @Throws(IOException::class)
    private fun getStartOfSequence(`in`: DataInputStream, sequence: ByteArray): Int {

        val end = getEndOfSeqeunce(`in`, sequence)
        return if (end < 0) -1 else end - sequence.size
    }

    @Throws(IOException::class, NumberFormatException::class)
    private fun parseContentLength(headerBytes: ByteArray): Int {

        val headerIn = ByteArrayInputStream(headerBytes)
        val props = Properties()
        props.load(headerIn)
        return Integer.parseInt(props.getProperty(CONTENT_LENGTH))
    }

    @Throws(IOException::class)
    fun readMjpegFrame(): MjpegContainer {
        mark(FRAME_MAX_LENGTH)
        val headerLen = getStartOfSequence(this, SOI_MARKER)
        reset()
        val header = ByteArray(headerLen)
        readFully(header)
        try {
            mContentLength = parseContentLength(header)
        } catch (nfe: NumberFormatException) {
            mContentLength = getEndOfSeqeunce(this, EOF_MARKER)
        }

        reset()
        val frameData = ByteArray(mContentLength)

        skipBytes(headerLen)
        readFully(frameData)

        val bm = BitmapFactory.decodeStream(ByteArrayInputStream(frameData))
        return MjpegContainer(bm, frameData)
    }

    companion object {
        private val HEADER_MAX_LENGTH = 100
        private val FRAME_MAX_LENGTH = 40000 + HEADER_MAX_LENGTH

        fun read(urlString: String?, act: Activity, listener: MJpegListener): MjpegInputStream? {

            Thread(Runnable {
                try {
                    val url = URL(urlString)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.setRequestProperty("User-Agent", "")
                    connection.requestMethod = "GET"
                    connection.doInput = true
                    connection.connect()
                    val inputStream = connection.inputStream
                    act.runOnUiThread(MyRunnable(inputStream, listener))
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }).start()

            return null
        }
    }
}