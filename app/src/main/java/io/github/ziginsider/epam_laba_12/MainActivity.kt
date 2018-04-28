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
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.indeterminateProgressDialog
import org.jetbrains.anko.toast
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity(), BoundService.ServiceImageLoadingListener {
    private val REQUEST_PERMISSION_EXTERNAL_STORAGE = 1
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
            if (checkPermission()) {
                this.toast("You should grant permission")
                requestPermission()
            } else {
                progressDialog = indeterminateProgressDialog("Loading", "Image loading...")
                progressDialog.show()

                boundService?.let {
                    it.doImageLoading("https://us.123rf.com/450wm/mondaian/mondaian1701/mondaian170100117/71437596-roman-coliseum.jpg",
                            BoundServiceListener(this))
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
            //boundService?.doImageLoading("https://us.123rf.com/450wm/mondaian/mondaian1701/mondaian170100117/71437596-roman-coliseum.jpg",
            //        BoundServiceListener(this@MainActivity))
            Log.d("TAG", "onServiceConnected")
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            boundService = null
            Log.d("TAG", "onServiceDisconnected")
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

    override fun onImageLoadingDone(url: String?) {

    }

    private fun checkPermission() = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_PERMISSION_EXTERNAL_STORAGE)
    }


    private class BoundServiceListener(activity: MainActivity)
        : BoundService.ServiceImageLoadingListener {
        private val weakActivity: WeakReference<MainActivity>
                = WeakReference(activity)

        override fun onImageLoadingDone(url: String?) {
            val localReferenceActivity = weakActivity.get()
            localReferenceActivity?.let {
                with(it) {
                    runOnUiThread({
                        if (progressDialog.isShowing) {
                            progressDialog.dismiss()
                        }
                    })
                }
            }
        }
    }

}
