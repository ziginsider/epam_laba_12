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
import io.github.ziginsider.epam_laba_12.download.RetrofitService
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
    private var totalMBFileSize: Int = 0
    private lateinit var filePath: File

    override fun onBind(intent: Intent?): IBinder {
        return myBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        notificationManager?.cancel(0)
        return super.onUnbind(intent)
    }

    inner class MyBinder: Binder() {
        fun getService() : BoundService {
            return this@BoundService
        }
    }

    /**
     *
     *
     */
    fun doFileDownloading(urlBase: String,
                          urlFile: String,
                          nameDownloadedFile: String,
                          listener: ServiceFileLoadingListener) {
        Thread(Runnable {
            this.listener = listener
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
            notificationBuilder = NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_file_download)
                    .setContentTitle("Download")
                    .setContentText("Downloading file")
                    .setAutoCancel(true)
            notificationManager?.notify(0, notificationBuilder?.build())
            initDownload(urlBase, urlFile, nameDownloadedFile)
        }).start()
    }

    private fun initDownload(urlBase: String, urlFile: String, nameDownloadedFile: String) {
        val retrofit = Retrofit.Builder()
                .baseUrl(urlBase)
                .build()
        val retrofitService = retrofit.create(RetrofitService::class.java)
        val request = retrofitService.downloadFile(urlFile)
        try {
            request.execute().body()?.let { downloadFile(it, nameDownloadedFile) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun downloadFile(body: ResponseBody, newFileName: String) {
        val data = ByteArray(1024 * 4)
        val fileSize = body.contentLength()
        val bis = BufferedInputStream(body.byteStream(), 1024 * 8)
        filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val outputFile = File(filePath, newFileName)
        val output = FileOutputStream(outputFile)
        val startTime = System.currentTimeMillis()
        var count: Int
        var currentBytesFileSize = 0L
        var timeCount = 1

        count = bis.read(data)
        while (count != -1) {
            currentBytesFileSize += count
            totalMBFileSize = (fileSize / Math.pow(1024.0, 2.0)).toInt()
            val currentMBFileSize = Math.round(currentBytesFileSize / Math.pow(1024.0, 2.0))
            val progress = (currentBytesFileSize * 100 / fileSize).toInt()
            val currentTime = System.currentTimeMillis() - startTime
            //renew 1 time per second
            if (currentTime > 1000 * timeCount) {
                val download = Download(progress, currentMBFileSize.toInt(), totalMBFileSize)
                sendNotification(download)
                timeCount++
            }
            output.write(data, 0, count)
            count = bis.read(data)
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
                    + totalMBFileSize + " MB")
            notificationManager?.notify(0, it.build())
        }
    }

    private fun onDownloadComplete() {
        // (1) change ProgressBar in Activity
        listener?.onFileLoadingComplete(filePath)
        // (2) change in notifications
        notificationManager?.cancel(0)
        notificationBuilder?.let {
            it.setProgress(0, 0, false)
            it.setContentText("File Downloaded")
            notificationManager?.notify(0, it.build())
        }
    }
}
