package com.bignerdranch.android.photogallery

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "ThumbnailDownloader"
private const val MESSAGE_DOWNLOAD = 0

class ThumbnailDownloader<in T>(private val m_responseHandler: Handler,
                                private val m_onThumbnailDownloaded: (T, Bitmap) -> Unit)
    : HandlerThread(TAG), LifecycleObserver
{
    val m_fragmentLifecycleObserver: LifecycleObserver = object: LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
        fun setup() {
            Log.i(TAG, "Start. background thread!")

            start()
            looper
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun tearDown() {
            Log.i(TAG, "Destr. background thread!")

            quit()
        }
    }

    val m_viewLifecycleObserver: LifecycleObserver = object: LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun clearQueue() {
            Log.i(TAG, "Clean up...")

            m_requestHandler.removeMessages(MESSAGE_DOWNLOAD)
            m_requestMap.clear()
        }
    }

    private var m_hasQuit = false
    private lateinit var m_requestHandler: Handler
    private val m_requestMap = ConcurrentHashMap<T, String>()
    private val m_flickrFetcher = FlickrFetcher()

    @Suppress("UNCHECKED_CAST")
    @SuppressLint("HandlerLeak")
    override fun onLooperPrepared() {
        m_requestHandler = object: Handler() {
            override fun handleMessage(msg: Message) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    val target = msg.obj as T

                    Log.i(TAG, "Request has been gotten for URL: ${m_requestMap[target]}")

                    handleRequest(target)
                }
            }
        }

        super.onLooperPrepared()
    }

    override fun quit(): Boolean {
        m_hasQuit = true

        return super.quit()
    }

    fun queueThumbnail(target: T, url: String) {
        Log.i(TAG, "Got new URL: ${url}")

        m_requestMap[target] = url

        m_requestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget()
    }

    private fun handleRequest(target: T) {
        val url = m_requestMap[target] ?: return
        val bitmap = m_flickrFetcher.fetchPhoto(url) ?: return

        m_responseHandler.post(Runnable {
            if (m_requestMap[target] != url || m_hasQuit)
                return@Runnable

            m_requestMap.remove(target)
            m_onThumbnailDownloaded(target, bitmap)
        })
    }
}