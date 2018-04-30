package io.github.ziginsider.epam_laba_12

import android.Manifest
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
import io.github.ziginsider.epam_laba_12.download.*
import io.github.ziginsider.epam_laba_12.utils.toast
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import io.github.ziginsider.epam_laba_12.download.Download


const val REQUEST_PERMISSION_EXTERNAL_STORAGE = 1
const val CHOOSE_STARTED_SERVICE = 0
const val CHOOSE_BOUND_SERVICE = 1
const val CHOOSE_JOB_SCHEDULER = 2
const val ACTION_MESSAGE_PROGRESS = "message_started_service_progress"
const val KEY_MESSAGE_PROGRESS = "download"
const val KEY_MESSAGE_FILE = "file_name"

class MainActivity : AppCompatActivity() {
    private var boundService: BoundService? = null
    private var isBound = false
    private var downloadingWay = CHOOSE_STARTED_SERVICE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        showBottomNavigationMenu()

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

    private fun runJobScheduler() {

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

    private fun showBottomNavigationMenu() {
        bottomNavigation.setOnNavigationItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.chooseStartedService -> {
                    downloadingWay = CHOOSE_STARTED_SERVICE
                    registerReceiver()
                }
                R.id.chooseBoundService -> downloadingWay = CHOOSE_BOUND_SERVICE
                R.id.chooseJobScheduler -> downloadingWay = CHOOSE_JOB_SCHEDULER
            }
            true
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

    private fun checkPermission() = ContextCompat.checkSelfPermission(this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_PERMISSION_EXTERNAL_STORAGE)
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
}
