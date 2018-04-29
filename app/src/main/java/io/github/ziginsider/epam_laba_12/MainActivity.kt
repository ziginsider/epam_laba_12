package io.github.ziginsider.epam_laba_12

import android.Manifest
import android.app.ProgressDialog
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
import org.jetbrains.anko.indeterminateProgressDialog
import org.jetbrains.anko.toast
import java.io.File
import java.lang.ref.WeakReference
import android.graphics.BitmapFactory
import io.github.ziginsider.epam_laba_12.download.DOWNLOADED_FILE_NAME
import io.github.ziginsider.epam_laba_12.download.RETROFIT_BASE_URL
import io.github.ziginsider.epam_laba_12.download.RETROFIT_GET_REQUEST


class MainActivity : AppCompatActivity() {
    private val REQUEST_PERMISSION_EXTERNAL_STORAGE = 1 //TODO const val ?
    private lateinit var progressDialog: ProgressDialog
    var boundService: BoundService? = null
    var isBound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindService(Intent(this, BoundService::class.java),
                myConnection,
                Service.BIND_AUTO_CREATE)
        isBound = true

        downloadButton.setOnClickListener {
            if (!checkPermission()) {
                this.toast("You should grant permission")
                requestPermission()
            } else {
                progressDialog = indeterminateProgressDialog("Loading", "Image loading...")
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                progressDialog.max = 100
                progressDialog.setMessage("File Downloading...")
                progressDialog.show()

                boundService?.let {
                    it.doFileDownloading(RETROFIT_BASE_URL, RETROFIT_GET_REQUEST,
                            DOWNLOADED_FILE_NAME, BoundServiceListener(this))
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

    private val myConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder = service as BoundService.MyBinder
            boundService = binder.getService()
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            boundService = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            try {
                unbindService(myConnection)
                isBound = false
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
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


    private class BoundServiceListener(activity: MainActivity)
        : BoundService.ServiceFileLoadingListener {
        private val weakActivity: WeakReference<MainActivity> = WeakReference(activity)

        override fun onFileLoadingComplete(pathFile: File) {
            weakActivity.get()?.let {
                with(it) {
                    runOnUiThread({
                        if (progressDialog.isShowing) {
                            progressDialog.progress = 100
                            progressDialog.dismiss()

                            imageView.setImageBitmap(BitmapFactory.decodeFile(pathFile.path
                                    + File.separator + DOWNLOADED_FILE_NAME))
                        }
                    })
                }
            }
        }

        override fun onFileLoadingProgress(download: Download) {
            weakActivity.get()?.let {
                with(it) {
                    runOnUiThread({
                        if (progressDialog.isShowing) {
                            progressDialog.progress = download.progress
                        }
                    })
                }
            }
        }
    }

}
