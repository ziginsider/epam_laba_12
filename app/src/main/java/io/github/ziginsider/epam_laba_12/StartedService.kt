package io.github.ziginsider.epam_laba_12

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import io.github.ziginsider.epam_laba_12.download.*
import okhttp3.ResponseBody
import retrofit2.Retrofit
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.support.v4.content.LocalBroadcastManager
import io.github.ziginsider.epam_laba_12.download.Download

class StartedService : Service() {
    private var notificationBuilder: NotificationCompat.Builder? = null
    private var notificationManager: NotificationManager? = null
    private var totalMBFileSize: Int = 0
    private lateinit var filePath: File

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            doFileDownloading(it.getStringExtra(KEY_BASE_URL), it.getStringExtra(KEY_GET_REQUEST),
                    it.getStringExtra(KEY_FILE_NAME))
        }
        return Service.START_REDELIVER_INTENT
    }

    fun doFileDownloading(urlBase: String,
                          urlFile: String,
                          nameDownloadedFile: String) {
        Thread(Runnable {
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager?
            notificationBuilder = NotificationCompat.Builder(this, "ch1")
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
            //renew 10 times per second
            if (currentTime > 100 * timeCount) {
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
        sendIntent(download)
        // (2) change in notifications
        notificationBuilder?.let {
            it.setProgress(100, download.progress, false)
            it.setContentText("Downloading file " + download.currentFileSize + "/"
                    + totalMBFileSize + " MB")
            notificationManager?.notify(0, it.build())
        }
    }

    private fun sendIntent(download: Download) {
        val intent = Intent(ACTION_MESSAGE_PROGRESS)
        intent.putExtra(KEY_MESSAGE_PROGRESS, download)
        intent.putExtra(KEY_MESSAGE_FILE, filePath.path)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun onDownloadComplete() {
        // (1) change ProgressBar in Activity
        sendIntent(Download(100, 0, 0))
        // (2) change in notifications
        notificationManager?.cancel(0)
        notificationBuilder?.let {
            it.setProgress(0, 0, false)
            it.setContentText("File Downloaded")
            notificationManager?.notify(0, it.build())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationManager?.cancel(0)
    }
}