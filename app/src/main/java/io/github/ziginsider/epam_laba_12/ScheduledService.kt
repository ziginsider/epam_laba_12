package io.github.ziginsider.epam_laba_12

import android.app.IntentService
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import io.github.ziginsider.epam_laba_12.download.*
import okhttp3.ResponseBody
import retrofit2.Retrofit
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

const val ACTION = "sheduled_action"

/**
 * Implementation IntentService. Runs file downloading with showing notification and
 * progress of download
 *
 * @since 2018-04-29
 * @author Alex Kisel
 */
class ScheduledService(name: String? = "Scheduled Service") : IntentService(name) {
    private var notificationBuilder: NotificationCompat.Builder? = null
    private var notificationManager: NotificationManager? = null
    private var totalMBFileSize: Int = 0
    private lateinit var filePath: File

    companion object {
        /**
         * Runs file downloading with showing notification and progress of download
         *
         * @param urlBase URL address of file downloading without file's name
         * @param urlFile Get request with file's name
         * @param nameDownloadedFile new file's name for external storage
         */
        fun startScheduledJob(context: Context, urlBase: String, urlFile: String,
                              nameDownloadedFile: String) {
            val intent = Intent(context, ScheduledService::class.java)
            intent.action = ACTION
            intent.putExtra(KEY_BASE_URL, urlBase)
            intent.putExtra(KEY_GET_REQUEST, urlFile)
            intent.putExtra(KEY_FILE_NAME, nameDownloadedFile)
            context.startService(intent)
        }
    }

    override fun onHandleIntent(intent: Intent?) {
        intent?.let {
            if (it.action == ACTION) {
                val urlBase = it.getStringExtra(KEY_BASE_URL)
                val urlFile = it.getStringExtra(KEY_GET_REQUEST)
                val nameDownloadedFile = it.getStringExtra(KEY_FILE_NAME)
                notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager?
                notificationBuilder = NotificationCompat.Builder(this, "ch1")
                        .setSmallIcon(R.drawable.ic_file_download)
                        .setContentTitle("Download")
                        .setContentText("Downloading file")
                        .setAutoCancel(true)
                notificationManager?.notify(0, notificationBuilder?.build())
                initDownload(urlBase, urlFile, nameDownloadedFile)
            }
        }
    }

    private fun initDownload(urlBase: String, urlFile: String, nameDownloadedFile: String) {
        val retrofit = Retrofit.Builder()
                .baseUrl(urlBase)
                .build()
        val retrofitService = retrofit.create(RetrofitDownload::class.java)
        val request = retrofitService.downloadFile(urlFile)
        try {
            request.execute().body()?.let { downloadFile(it, nameDownloadedFile) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun downloadFile(body: ResponseBody, newFileName: String) {
        filePath = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        } else {
            applicationContext.filesDir
        }
        val data = ByteArray(1024 * 4)
        val fileSize = body.contentLength()
        val outputFile = File(filePath, newFileName)
        val startTime = System.currentTimeMillis()
        var count: Int
        var currentBytesFileSize = 0L
        var timeCount = 1
        var output: FileOutputStream? = null
        var bis: BufferedInputStream? = null
        try {
            output = FileOutputStream(outputFile)
            bis = BufferedInputStream(body.byteStream(), 1024 * 8)
            count = bis.read(data)
            while (count != -1) {
                currentBytesFileSize += count
                totalMBFileSize = (fileSize / Math.pow(1024.0, 2.0)).toInt()
                val currentMBFileSize
                        = Math.round(currentBytesFileSize / Math.pow(1024.0, 2.0))
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
        } finally {
            output?.flush()
            output?.close()
            bis?.close()
        }
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