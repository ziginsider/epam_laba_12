package io.github.ziginsider.epam_laba_12

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Environment
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import io.github.ziginsider.epam_laba_12.download.Download
import io.github.ziginsider.epam_laba_12.retrofit.RetrofitService
import okhttp3.ResponseBody
import retrofit2.Retrofit
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class BoundService: Service() {
    interface ServiceFileLoadingListener {
        fun onFileLoadingComplete(pathFile: File)
        fun onFileLoadingProgress(download: Download)
    }

    private val myBinder = MyBinder()
    private var listener: ServiceFileLoadingListener? = null

    private var notificationBuilder: NotificationCompat.Builder? = null
    private var notificationManager: NotificationManager? = null
    private var totalFileSize: Int = 0
    private lateinit var filePath: File

    override fun onBind(intent: Intent?): IBinder {
        val str = intent?.extras?.get("URL")
        return myBinder
    }

    inner class MyBinder: Binder() {

        fun getService() : BoundService {
            return this@BoundService
        }
    }
    fun doFileDownloading(url: String, listener: ServiceFileLoadingListener) {
        this.listener = listener

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        notificationBuilder = NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_file_download)
                .setContentTitle("Download")
                .setContentText("Downloading file")
                .setAutoCancel(true)
        notificationManager?.notify(0, notificationBuilder?.build())

        //Thread(Runnable {
            initDownload(url)
        //}).start()
    }

    private fun initDownload(url: String): String {
//        val fileName = UUID.randomUUID().toString() + ".jpg"
//        Picasso.with(this)
//                .load("https://us.123rf.com/450wm/mondaian/mondaian1701/mondaian170100117/71437596-roman-coliseum.jpg")
//                //.resize(2000, 1000)
//                .into(SaveImageHelper(
//                        this,
//                        applicationContext.contentResolver,
//                        fileName,
//                        "image description"))

        val retrofit = Retrofit.Builder() //TODO in RetrofitService.kt like an companion object
                .baseUrl(url)
                .build()

        val retrofitService = retrofit.create(RetrofitService::class.java)

        val request = retrofitService.downloadFile()
        try {
            request.execute().body()?.let { downloadFile(it) }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return ""
    }

    @Throws(IOException::class)
    private fun downloadFile(body: ResponseBody) {
        var count: Int
        val data = ByteArray(1024 * 4)
        val fileSize = body.contentLength()

        val bis = BufferedInputStream(body.byteStream(), 1024 * 8)
        filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val outputFile = File(filePath, "file.zip")
        val output = FileOutputStream(outputFile)
        var total = 0L
        val startTime = System.currentTimeMillis()
        var timeCount = 1

        while (bis.read(data) != -1) {
            count = bis.read(data)

            total += count
            totalFileSize = (fileSize / Math.pow(1024.0, 2.0)).toInt()
            val current = Math.round(total / Math.pow(1024.0, 2.0))

            val progress = (total * 100 / fileSize).toInt()
            val currentTime = System.currentTimeMillis() - startTime

            if (currentTime > 1000 * timeCount) {
                val download = Download(progress, current.toInt(), totalFileSize)
                sendNotification(download)
                //listener?.onFileLoadingProgress(download)
                timeCount++
            }

            output.write(data, 0, count)
        }

        onDownloadComplete()
        output.flush()
        output.close()
        bis.close()
    }

    private fun sendNotification(download: Download) {
        // (1) change ProgressBar in Activity
        listener?.onFileLoadingProgress(download)
        // (2) change in notifications
        notificationBuilder?.let {
            it.setProgress(100, download.progress, false)
            it.setContentText("Downloading file " + download.currentFileSize + "/"
                    + totalFileSize + " MB")
            notificationManager?.notify(0, it.build())
        }
    }

    private fun onDownloadComplete() {
        listener?.onFileLoadingComplete(filePath)
    }




    //TODO TaskCancel notificationManager.cancel(0)
}
