package io.github.ziginsider.epam_laba_12.image

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.provider.MediaStore
import android.util.Log
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import java.lang.ref.WeakReference

class SaveImageHelper(): Target {
    interface ImageLoadingListener {
        fun imageLoaded(url: String?)
    }

    private var context: Context? = null
    private lateinit var contentResolverWeakReference: WeakReference<ContentResolver>
    private lateinit var name: String
    private lateinit var description: String
    private var imageLoadingListener: ImageLoadingListener? = null

    constructor(context: Context?,
                contentResolver: ContentResolver,
                name: String,
                description: String) : this() {
        this.context = context
        contentResolverWeakReference = WeakReference(contentResolver)
        this.name = name
        this.description = description
        if (context is ImageLoadingListener) {
            imageLoadingListener = context
        } else {
            throw RuntimeException(context.toString()
                    + " must implement interface ImageLoadingListener")
        }
    }

    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
    }

    override fun onBitmapFailed(errorDrawable: Drawable?) {
        Log.d("TAG", errorDrawable?.toString())
    }

    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
        val resolver = contentResolverWeakReference.get()
        val url: String?
        if (resolver != null) {
            url = MediaStore.Images.Media.insertImage(resolver, bitmap, name, description)
            imageLoadingListener?.imageLoaded(url)
        }
        imageLoadingListener = null
        context = null
    }
}
