package com.example.aletzz.streamdownloadmjpegvideoapp

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.*
import com.example.aletzz.streamdownloadmjpegvideoapp.mjpeg.MjpegView
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception

class MainActivity : Activity() {

    private var rootPath: String? = null
    var streamVideoOne: String? = null
    var streamVideoTwo: String? = null

    var haveToContinueVideoOne: Boolean = false
    var haveToContinueVideoTwo: Boolean = false

    var isStreamOnePlayingAndDownloading = false
    var isStreamTwoPlayingAndDownloading = false

    var isSaveToSD: Boolean? = null

    var runningTestOne = true
    var runningTestTwo = true

    private var mMjpegWriterVideoOne: MjpegWriter? = null
    private var mMjpegWriterVideoTwo: MjpegWriter? = null

    private lateinit var mjpegThread: Thread
    private lateinit var mjpegThreadTwo: Thread
    private lateinit var t: Thread
    private lateinit var t2: Thread

    companion object {
        private val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar_main?.title = "StreamAndDownloadMjpegVideoApp"
        toolbar_main?.setTitleTextColor(Color.WHITE)
        setActionBar(toolbar_main)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        mMjpegWriterVideoOne = MjpegWriter()
        mMjpegWriterVideoTwo = MjpegWriter()

        streamVideoOne = "PutYourlURLHere"
        streamVideoTwo = "PutYourlURLHere"

        btn_choice_one?.setOnClickListener {
            initializePlayerOne(true)
        }

        btn_choice_two?.setOnClickListener {
            initializePlayerTwo(true)
        }

    }

    private fun initializePlayerOne(isPortrait: Boolean) {
        if (isStreamOnePlayingAndDownloading) {
            return
        }

        if (isPortrait) {
            if (isStreamTwoPlayingAndDownloading) {
                surface_container_video_two?.stopPlayback()
                mMjpegWriterVideoTwo?.stopRecording()
                isStreamTwoPlayingAndDownloading = false
            }
            surface_container_video_two?.visibility = View.GONE
            surface_container_video_one?.visibility = View.VISIBLE
        }

        if (Utils.Util.isConnectedToInternet(this)) {
            try {
                surface_container_video_one?.setSource(com.example.aletzz.streamdownloadmjpegvideoapp.mjpeg.MjpegInputStream.read(streamVideoOne))
                surface_container_video_one?.setDisplayMode(MjpegView.SIZE_FULLSCREEN)

                MjpegInputStream.read(streamVideoOne, this, {
                    mjpegThread = Thread(Runnable {
                        //                        synchronized (runningTestOne) {
                        while (runningTestOne) {
                            try {
                                val mjpegContainer = it.readMjpegFrame()
                                if (mMjpegWriterVideoOne != null) mMjpegWriterVideoOne?.saveByteArrayToFile(mjpegContainer.data)
                            } catch (e: Exception) {
                                Log.e(TAG, "-> " + e.toString())
                            }
                        }
                    })
                    mjpegThread.start()
                })

                t = Thread(Runnable {
                    try {
                        Thread.sleep(500)
                        runOnUiThread {
                            mMjpegWriterVideoOne?.recordMjpeg(rootPath, streamVideoOne)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "-> " + e.toString())
                    }
                })
                t.start()
                isStreamOnePlayingAndDownloading = true
            } catch (ex: Exception) {
                Log.e("THREAD", ex.toString())
                Utils.Util.showSnackBar(this, "Errore primo stream (server)")
            }
        }
    }

    private fun initializePlayerTwo(isPortrait: Boolean) {
        if (isStreamTwoPlayingAndDownloading) {
            return
        }

        if (isPortrait) {
            surface_container_video_one?.stopPlayback()
            if (isStreamOnePlayingAndDownloading) {
                surface_container_video_one?.stopPlayback()
                mMjpegWriterVideoOne?.stopRecording()
                isStreamOnePlayingAndDownloading = false
            }
            surface_container_video_one?.visibility = View.GONE
            surface_container_video_two?.visibility = View.VISIBLE
        }

        if (Utils.Util.isConnectedToInternet(this)) {
            try {
                surface_container_video_two?.setSource(com.example.aletzz.streamdownloadmjpegvideoapp.mjpeg.MjpegInputStream.read(streamVideoTwo))
                surface_container_video_two?.setDisplayMode(MjpegView.SIZE_FULLSCREEN)

                MjpegInputStream.read(streamVideoTwo, this, {
                    mjpegThreadTwo = Thread(Runnable {
                        //                        synchronized (runningTestTwo) {
                        while (runningTestTwo) {
                            try {
                                val mjpegContainer = it.readMjpegFrame()
                                if (mMjpegWriterVideoTwo != null) mMjpegWriterVideoTwo?.saveByteArrayToFile(mjpegContainer.data)
                            } catch (e: Exception) {
                                Log.e(TAG, "-> " + e.toString())
                            }
                        }
                    })
                    mjpegThreadTwo.start()
                })

                t2 = Thread(Runnable {
                    try {
                        Thread.sleep(500)
                        runOnUiThread { mMjpegWriterVideoTwo?.recordMjpeg(rootPath, streamVideoTwo) }
                    } catch (e: Exception) {
                        Log.e(TAG, "-> " + e.toString())
                    }
                })
                t2.start()
                isStreamTwoPlayingAndDownloading = true
            } catch (ex: Exception) {
                Log.e("surfacetwo", ex.toString())
                Utils.Util.showSnackBar(this, "Errore col server")
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        if (haveToContinueVideoOne && haveToContinueVideoTwo) {
            Log.e(TAG, "ONRESUMEALL")
            runningTestOne = true
            runningTestTwo = true
            surface_container_video_one?.setSource(com.example.aletzz.streamdownloadmjpegvideoapp.mjpeg.MjpegInputStream.read(streamVideoOne))
            surface_container_video_two?.setDisplayMode(MjpegView.SIZE_FULLSCREEN)
            surface_container_video_two?.setSource(com.example.aletzz.streamdownloadmjpegvideoapp.mjpeg.MjpegInputStream.read(streamVideoTwo))
            surface_container_video_one?.setDisplayMode(MjpegView.SIZE_FULLSCREEN)
            mjpegThread.start()
            mjpegThreadTwo.start()
        } else if (haveToContinueVideoOne) {
            Log.e(TAG, "ONRESUMEONE")
            runningTestOne = true
            mjpegThread.start()
            surface_container_video_one?.setSource(com.example.aletzz.streamdownloadmjpegvideoapp.mjpeg.MjpegInputStream.read(streamVideoOne))
            surface_container_video_one?.setDisplayMode(MjpegView.SIZE_FULLSCREEN)
        } else if (haveToContinueVideoTwo) {
            Log.e(TAG, "ONRESUMETWO")
            runningTestTwo = true
            mjpegThreadTwo.start()
            surface_container_video_two?.setSource(com.example.aletzz.streamdownloadmjpegvideoapp.mjpeg.MjpegInputStream.read(streamVideoTwo))
            surface_container_video_two?.setDisplayMode(MjpegView.SIZE_FULLSCREEN)
        }
    }

    public override fun onPause() {
        super.onPause()
        Log.e(TAG, "onPause")

        runningTestTwo = false

        if (isStreamOnePlayingAndDownloading) {
            surface_container_video_one?.stopPlayback()
            runningTestOne = false
            haveToContinueVideoOne = true
        }
        if (isStreamTwoPlayingAndDownloading) {
            surface_container_video_two?.stopPlayback()
            runningTestTwo = false
            haveToContinueVideoTwo = true
        }
        if (!isStreamOnePlayingAndDownloading) {
            haveToContinueVideoOne = false
        }
        if (!isStreamTwoPlayingAndDownloading) {
            haveToContinueVideoTwo = false
        }

    }

    public override fun onStop() {
        super.onStop()
        Log.e(TAG, "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG, "onDestroy")

        if (isStreamOnePlayingAndDownloading) {
            surface_container_video_one?.stopPlayback()
            mMjpegWriterVideoOne?.stopRecording()
            runningTestOne = false
            surface_container_video_one?.destroyDrawingCache()
        }
        if (isStreamTwoPlayingAndDownloading) {
            surface_container_video_two?.stopPlayback()
            mMjpegWriterVideoTwo?.stopRecording()
            runningTestTwo = false
            surface_container_video_two?.destroyDrawingCache()

        }
    }
}