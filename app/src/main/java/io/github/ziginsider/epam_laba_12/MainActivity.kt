package io.github.ziginsider.epam_laba_12

import android.Manifest
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import com.squareup.picasso.Picasso
import io.github.ziginsider.epam_laba_12.image.SaveImageHelper
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.indeterminateProgressDialog
import org.jetbrains.anko.toast
import java.util.*

class MainActivity : AppCompatActivity(), SaveImageHelper.TargetCallback {
    private val REQUEST_PERMISSION_CODE = 1
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        downloadButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                this.toast("You should grant permission")
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_PERMISSION_CODE)
            } else {
                progressDialog = indeterminateProgressDialog("Loading", "Image loading...")
                progressDialog.show()

                val fileName = UUID.randomUUID().toString() + ".jpg"
                Picasso.with(this)
                        .load("https://us.123rf.com/450wm/mondaian/mondaian1701/mondaian170100117/71437596-roman-coliseum.jpg")
                        //.resize(2000, 1000)
                        .into(SaveImageHelper(
                                this,
                                applicationContext.contentResolver,
                                fileName,
                                "image description"))
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION_CODE -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    toast("Permission granted")
                } else {
                    toast("Permission denied")
                }
            }
        }
    }

    override fun imageLoaded(url: String?) {
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
        Log.d("TAG", "url = $url")
    }
}
