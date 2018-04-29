package io.github.ziginsider.epam_laba_12

import android.Manifest
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import io.github.ziginsider.epam_laba_12.download.Download
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast
import java.io.File
import java.lang.ref.WeakReference
import android.graphics.BitmapFactory
import io.github.ziginsider.epam_laba_12.download.DOWNLOADED_FILE_NAME
import io.github.ziginsider.epam_laba_12.download.RETROFIT_BASE_URL
import io.github.ziginsider.epam_laba_12.download.RETROFIT_GET_REQUEST

const val REQUEST_PERMISSION_EXTERNAL_STORAGE = 1

class MainActivity : AppCompatActivity() {
    private var boundService: BoundService? = null
    private var isBound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        downloadButton.setOnClickListener {
            if (!checkPermission()) {
                this.toast("You should grant permission")
                requestPermission()
            } else {
                bindService(Intent(this, BoundService::class.java),
                        myConnection,
                        Service.BIND_AUTO_CREATE)
                isBound = true
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

                            if (isBound) {
                                try {
                                    unbindService(myConnection)
                                    isBound = false
                                } catch (e: IllegalArgumentException) {
                                    e.printStackTrace()
                                }
                            }
                        } else {
                            textView.text = String.format("Downloaded (%d/%d) MB",
                                    download.currentFileSize, download.totalFileSize)
                        }
                    })
                }
            }
        }
    }
}
