package io.github.ziginsider.epam_laba_12

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

class BoundService: Service() {
    interface ServiceImageLoadingListener {
        fun onImageLoadingDone(url: String)
    }
    private val myBinder = MyBinder()

    override fun onBind(intent: Intent?): IBinder {
        val str = intent?.extras?.get("URL")
        return myBinder
    }

    inner class MyBinder: Binder() {
        fun getService() : BoundService {
            return this@BoundService
        }
    }

    fun doImageLoading(url: String, listener: ServiceImageLoadingListener) {
        Thread(Runnable {
            val storageUrl = imageLoading(url)
            listener.onImageLoadingDone(storageUrl)
        })
    }

    private fun imageLoading(url: String): String {
        return ""
    }
}