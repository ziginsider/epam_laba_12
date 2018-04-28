package io.github.ziginsider.epam_laba_12

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.squareup.picasso.Picasso
import io.github.ziginsider.epam_laba_12.image.SaveImageHelper
import java.util.*

class BoundService: Service(), SaveImageHelper.ImageLoadingListener {
    interface ServiceImageLoadingListener {
        fun onImageLoadingDone(url: String?)
    }

    private val myBinder = MyBinder()
    private var listener: ServiceImageLoadingListener? = null

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
        this.listener = listener
        //Thread(Runnable {
            imageLoading(url)
        //}).start()
    }

    private fun imageLoading(url: String): String {
        val fileName = UUID.randomUUID().toString() + ".jpg"
        Picasso.with(this)
                .load("https://us.123rf.com/450wm/mondaian/mondaian1701/mondaian170100117/71437596-roman-coliseum.jpg")
                //.resize(2000, 1000)
                .into(SaveImageHelper(
                        this,
                        applicationContext.contentResolver,
                        fileName,
                        "image description"))

        return ""
    }

    override fun imageLoaded(url: String?) {
        listener?.onImageLoadingDone(url)
    }
}
