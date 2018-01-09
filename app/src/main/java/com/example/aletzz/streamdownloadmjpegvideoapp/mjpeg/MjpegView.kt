package com.example.aletzz.streamdownloadmjpegvideoapp.mjpeg

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView

import java.io.IOException

class MjpegView : SurfaceView, SurfaceHolder.Callback {

    private var thread: MjpegViewThread? = null
    private var mIn: MjpegInputStream? = null
    private var showFps = false
    private var mRun = false
    private var surfaceDone = false
    private var overlayPaint: Paint? = null
    private var overlayTextColor: Int = 0
    private var overlayBackgroundColor: Int = 0
    private var ovlPos: Int = 0
    private var dispWidth: Int = 0
    private var dispHeight: Int = 0
    private var displayMode: Int = 0

    inner class MjpegViewThread(private val mSurfaceHolder: SurfaceHolder, context: Context) : Thread() {
        private var frameCounter = 0
        private var start: Long = 0
        private var ovl: Bitmap? = null

        private fun destRect(bmw: Int, bmh: Int): Rect? {
            var bmw = bmw
            var bmh = bmh
            val tempx: Int
            val tempy: Int
            if (displayMode == MjpegView.SIZE_STANDARD) {
                tempx = dispWidth / 2 - bmw / 2
                tempy = dispHeight / 2 - bmh / 2
                return Rect(tempx, tempy, bmw + tempx, bmh + tempy)
            }
            if (displayMode == MjpegView.SIZE_BEST_FIT) {
                val bmasp = bmw.toFloat() / bmh.toFloat()
                bmw = dispWidth
                bmh = (dispWidth / bmasp).toInt()
                if (bmh > dispHeight) {
                    bmh = dispHeight
                    bmw = (dispHeight * bmasp).toInt()
                }
                tempx = dispWidth / 2 - bmw / 2
                tempy = dispHeight / 2 - bmh / 2
                return Rect(tempx, tempy, bmw + tempx, bmh + tempy)
            }
            return if (displayMode == MjpegView.SIZE_FULLSCREEN) Rect(0, 0, dispWidth, dispHeight) else null
        }

        fun setSurfaceSize(width: Int, height: Int) {
            synchronized(mSurfaceHolder) {
                dispWidth = width
                dispHeight = height
            }
        }

        private fun makeFpsOverlay(p: Paint?, text: String): Bitmap {
            val b = Rect()
            p?.getTextBounds(text, 0, text.length, b)
            val bwidth = b.width() + 2
            val bheight = b.height() + 2
            val bm = Bitmap.createBitmap(bwidth, bheight, Bitmap.Config.ARGB_8888)
            val c = Canvas(bm)
            p?.color = overlayBackgroundColor
            c.drawRect(0f, 0f, bwidth.toFloat(), bheight.toFloat(), p)
            p?.color = overlayTextColor
            c.drawText(text, (-b.left + 1).toFloat(), bheight / 2 - (p!!.ascent() + p.descent()) / 2 + 1, p)
            return bm
        }

        override fun run() {
            start = System.currentTimeMillis()
            val mode = PorterDuffXfermode(PorterDuff.Mode.DST_OVER)
            var bm: Bitmap
            var width: Int
            var height: Int
            var destRect: Rect?
            var c: Canvas? = null
            val p = Paint()
            var fps = ""
            while (mRun) {
                if (surfaceDone) {
                    try {
                        c = mSurfaceHolder.lockCanvas()
                        synchronized(mSurfaceHolder) {
                            try {

                                if (c != null) {

                                    bm = mIn!!.readMjpegFrame()
                                    destRect = destRect(bm.width, bm.height)
                                    c.drawColor(Color.BLACK)
                                    c.drawBitmap(bm, null, destRect!!, p)
                                    if (showFps) {
                                        p.xfermode = mode
                                        if (ovl != null) {
                                            height = if (ovlPos and 1 == 1) destRect!!.top else destRect!!.bottom - ovl!!.height
                                            width = if (ovlPos and 8 == 8) destRect!!.left else destRect!!.right - ovl!!.width
                                            c.drawBitmap(ovl!!, width.toFloat(), height.toFloat(), null)
                                        }
                                        p.xfermode = null
                                        frameCounter++
                                        if (System.currentTimeMillis() - start >= 1000) {
                                            fps = frameCounter.toString() + "fps"
                                            frameCounter = 0
                                            start = System.currentTimeMillis()
                                            ovl = makeFpsOverlay(overlayPaint, fps)
                                        }
                                    }
                                }

                            } catch (e: IOException) {
                            }

                        }
                    } finally {
                        if (c != null) mSurfaceHolder.unlockCanvasAndPost(c)
                    }
                }
            }
        }
    }

    private fun init(context: Context) {
        val holder = holder
        holder.addCallback(this)
        thread = MjpegViewThread(holder, context)
        isFocusable = true
        overlayPaint = Paint()
        overlayPaint?.textAlign = Paint.Align.LEFT
        overlayPaint?.textSize = 12f
        overlayPaint?.typeface = Typeface.DEFAULT
        overlayTextColor = Color.WHITE
        overlayBackgroundColor = Color.BLACK
        ovlPos = MjpegView.POSITION_LOWER_RIGHT
        displayMode = MjpegView.SIZE_STANDARD
        dispWidth = width
        dispHeight = height
    }

    fun startPlayback() {
        if (mIn != null) {
            mRun = true
            thread?.start()
        }
    }

    fun stopPlayback() {
        mRun = false
        var retry = true
        while (retry) {
            try {
                thread?.join()
                retry = false
            } catch (e: InterruptedException) {
            }

        }
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    override fun surfaceChanged(holder: SurfaceHolder, f: Int, w: Int, h: Int) {
        thread?.setSurfaceSize(w, h)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceDone = false
        stopPlayback()
    }

    constructor(context: Context) : super(context) {
        init(context)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceDone = true
    }

    fun showFps(b: Boolean) {
        showFps = b
    }

    fun setSource(source: MjpegInputStream) {
        mIn = source
        startPlayback()
    }

    fun setOverlayPaint(p: Paint) {
        overlayPaint = p
    }

    fun setOverlayTextColor(c: Int) {
        overlayTextColor = c
    }

    fun setOverlayBackgroundColor(c: Int) {
        overlayBackgroundColor = c
    }

    fun setOverlayPosition(p: Int) {
        ovlPos = p
    }

    fun setDisplayMode(s: Int) {
        displayMode = s
    }

    companion object {
        val POSITION_UPPER_LEFT = 9
        val POSITION_UPPER_RIGHT = 3
        val POSITION_LOWER_LEFT = 12
        val POSITION_LOWER_RIGHT = 6

        val SIZE_STANDARD = 1
        val SIZE_BEST_FIT = 4
        val SIZE_FULLSCREEN = 8
    }
}