package io.github.ziginsider.epam_laba_12.image

import android.app.ProgressDialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.provider.MediaStore
import android.util.Log
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import java.lang.ref.WeakReference

class SaveImageHelper(): Target {
    private var context: Context? = null
    private lateinit var contentResolverWeakReference: WeakReference<ContentResolver>
    private lateinit var name: String
    private lateinit var description: String

    constructor(context: Context?,
                contentResolver: ContentResolver,
                name: String,
                description: String) : this() {
        this.context = context
        contentResolverWeakReference = WeakReference(contentResolver)
        this.name = name
        this.description = description
    }

    interface TargetCallback {
        fun imageLoaded(url: String?)
    }

    private var imageLoadingListener: TargetCallback? = null

    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
        if (context is TargetCallback) {
            imageLoadingListener = context as TargetCallback
        } else {
            throw RuntimeException(context.toString()
                    + " must implement interface TargetCallback")
        }
    }

    override fun onBitmapFailed(errorDrawable: Drawable?) {
        Log.d("TAG", errorDrawable?.toString())
    }

    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
        val resolver = contentResolverWeakReference.get()
        val url: String?
        if (resolver != null) {
            url = MediaStore.Images.Media.insertImage(resolver, bitmap, name, description)
            //Log.d("TAG", "internal url = $url")
            imageLoadingListener?.imageLoaded(url)
        }
        context = null
    }


}
