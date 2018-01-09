package com.example.aletzz.streamdownloadmjpegvideoapp.mjpeg

import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.Properties

import org.apache.http.HttpResponse
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log

class MjpegInputStream(`in`: InputStream) : DataInputStream(BufferedInputStream(`in`, FRAME_MAX_LENGTH)) {
    private val SOI_MARKER = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
    private val EOF_MARKER = byteArrayOf(0xFF.toByte(), 0xD9.toByte())
    private val CONTENT_LENGTH = "Content-Length"
    private var mContentLength = -1

    init {
        Log.e("INPUTSTREAM", "ENTRO? " + `in`.toString())
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
    fun readMjpegFrame(): Bitmap {
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
        return BitmapFactory.decodeStream(ByteArrayInputStream(frameData))
    }

    companion object {
        private val HEADER_MAX_LENGTH = 100
        private val FRAME_MAX_LENGTH = 40000 + HEADER_MAX_LENGTH

        fun read(url: String): MjpegInputStream? {
            val res: HttpResponse
            val httpclient = DefaultHttpClient()
            try {
                res = httpclient.execute(HttpGet(URI.create(url)))
                return MjpegInputStream(res.entity.content)
            } catch (e: ClientProtocolException) {
            } catch (e: IOException) {
            }

            return null
        }
    }
}