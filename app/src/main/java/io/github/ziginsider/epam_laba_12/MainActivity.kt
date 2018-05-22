package io.github.ziginsider.epam_laba_12

import android.Manifest
import android.annotation.TargetApi
import android.app.Service
import android.content.*
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.lang.ref.WeakReference
import android.graphics.BitmapFactory
import android.view.MenuItem
import io.github.ziginsider.epam_laba_12.utils.toast
import android.support.v4.content.LocalBroadcastManager
import io.github.ziginsider.epam_laba_12.download.Download
import android.content.ComponentName
import android.os.Build
import io.github.ziginsider.epam_laba_12.download.Contract.DOWNLOADED_FILE_NAME
import io.github.ziginsider.epam_laba_12.download.Contract.KEY_BASE_URL
import io.github.ziginsider.epam_laba_12.download.Contract.KEY_FILE_NAME
import io.github.ziginsider.epam_laba_12.download.Contract.KEY_GET_REQUEST
import io.github.ziginsider.epam_laba_12.download.Contract.RETROFIT_BASE_URL
import io.github.ziginsider.epam_laba_12.download.Contract.RETROFIT_GET_REQUEST
import java.util.concurrent.TimeUnit

/**
 * Activity that displays bottom navigation view, that lets to choose different way download
 * file (image) into external storage.
 *
 * Button (start download) runs execute [StartedService], [BoundService] or [JobSchedulerService]
 * depending on the choice bottom's navigation view.
 *
 * While downloading the file, a notification with a progress of download is displayed.
 *
 * After downloading the file (image), it is displayed on the screen.
 *
 * @since 2018-04-25
 * @author Alex Kisel
 */
class MainActivity : AppCompatActivity() {

    private var boundService: BoundService? = null
    private var isBound = false
    private var downloadingWay = CHOOSE_STARTED_SERVICE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        showBottomNavigationMenu()
        initDownloadButton()
        registerReceiver()
        textView.text = getString(R.string.download_way_started_service)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    toast("Permission granted")
                } else {
                    toast("Permission denied")
                }
            }
        }
    }

    private fun checkPermission() = ContextCompat.checkSelfPermission(this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_PERMISSION_EXTERNAL_STORAGE)
    }

    private fun initDownloadButton() {
        downloadButton.setOnClickListener {
            if (!checkPermission()) {
                this.toast("You should grant permission")
                requestPermission()
            } else {
                when (downloadingWay) {
                    CHOOSE_STARTED_SERVICE -> runStartedService()
                    CHOOSE_BOUND_SERVICE -> runBoundService()
                    CHOOSE_JOB_SCHEDULER -> runJobScheduler()
                }
            }
        }
    }

    private fun showBottomNavigationMenu() {
        bottomNavigation.setOnNavigationItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.chooseStartedService -> {
                    downloadingWay = CHOOSE_STARTED_SERVICE
                    textView.text = getString(R.string.download_way_started_service)
                }
                R.id.chooseBoundService -> {
                    downloadingWay = CHOOSE_BOUND_SERVICE
                    textView.text = getString(R.string.download_way_bound_service)
                }
                R.id.chooseJobScheduler -> {
                    downloadingWay = CHOOSE_JOB_SCHEDULER
                    textView.text = getString(R.string.download_way_job_scheduler)
                }
            }
            true
        }
    }

    private fun runStartedService() {
        val intent = Intent(this, StartedService::class.java)
        with(intent) {
            putExtra(KEY_BASE_URL, RETROFIT_BASE_URL)
            putExtra(KEY_GET_REQUEST, RETROFIT_GET_REQUEST)
            putExtra(KEY_FILE_NAME, DOWNLOADED_FILE_NAME)
            startService(this)
        }
    }

    private fun runBoundService() {
        bindService(Intent(this, BoundService::class.java),
                myConnection,
                Service.BIND_AUTO_CREATE)
        isBound = true
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun runJobScheduler() {
        JobSchedulerService.schedule(this, TimeUnit.SECONDS.toMillis(20))
    }

    private fun registerReceiver() {
        val manager = LocalBroadcastManager.getInstance(this)
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_MESSAGE_PROGRESS)
        manager.registerReceiver(broadcastReceiver, intentFilter)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_MESSAGE_PROGRESS) {
                val (progress, currentFileSize, totalFileSize)
                        = intent.getParcelableExtra<Download>(KEY_MESSAGE_PROGRESS)
                val pathFile = intent.getStringExtra(KEY_MESSAGE_FILE)
                progressBar.progress = progress
                if (progress == 100) {
                    imageView.setImageBitmap(BitmapFactory.decodeFile(pathFile
                            + File.separator + DOWNLOADED_FILE_NAME))
                    textView.text = "Image Download Complete"
                    stopService(Intent(this@MainActivity, StartedService::class.java))
                } else {
                    textView.text = String.format("Downloaded (%d/%d) MB", currentFileSize,
                            totalFileSize)
                }
            }
        }
    }

    private val myConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder = service as BoundService.MyBinder
            boundService = binder.getService()
            boundService?.doFileDownloading(RETROFIT_BASE_URL, RETROFIT_GET_REQUEST,
                    DOWNLOADED_FILE_NAME, BoundServiceListener(this@MainActivity))
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            boundService = null
        }
    }

    private class BoundServiceListener(activity: MainActivity)
        : BoundService.ServiceFileLoadingListener {
        private val weakActivity: WeakReference<MainActivity> = WeakReference(activity)

        override fun onFileLoadingProgress(download: Download, pathFile: File) {
            weakActivity.get()?.let {
                with(it) {
                    runOnUiThread({
                        progressBar.progress = download.progress
                        if (download.progress == 100) {
                            imageView.setImageBitmap(BitmapFactory.decodeFile(pathFile.path
                                    + File.separator + DOWNLOADED_FILE_NAME))
                            textView.text = "Image Download Complete"
                            unbindBoundService()
                        } else {
                            textView.text = String.format("Downloaded (%d/%d) MB",
                                    download.currentFileSize, download.totalFileSize)
                        }
                    })
                }
            }
        }
    }

    private fun unbindBoundService() {
        try {
            unbindService(myConnection)
            isBound = false
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    companion object {

        const val REQUEST_PERMISSION_EXTERNAL_STORAGE = 1
        const val CHOOSE_STARTED_SERVICE = 0
        const val CHOOSE_BOUND_SERVICE = 1
        const val CHOOSE_JOB_SCHEDULER = 2
        const val ACTION_MESSAGE_PROGRESS = "message_started_service_progress"
        const val KEY_MESSAGE_PROGRESS = "download"
        const val KEY_MESSAGE_FILE = "file_name"
    }
}
