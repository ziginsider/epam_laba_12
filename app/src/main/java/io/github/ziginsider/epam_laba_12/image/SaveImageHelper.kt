package io.github.ziginsider.epam_laba_12.image

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.support.v7.app.AlertDialog
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import java.lang.ref.WeakReference

class SaveImageHelper(private val alertDialogWeakReference: WeakReference<AlertDialog>,
                      private val contentResolverWeakReference: WeakReference<ContentResolver>,
                      private val name: String,
                      private val description: String): Target {
    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {

    }

    override fun onBitmapFailed(errorDrawable: Drawable?) {
    }

    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
    }

}
