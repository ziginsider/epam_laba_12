package io.github.ziginsider.epam_laba_12.image

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.provider.MediaStore
import android.support.v7.app.AlertDialog
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import java.lang.ref.WeakReference

class SaveImageHelper(): Target {
    private lateinit var context: Context
    private lateinit var alertDialogWeakReference: WeakReference<AlertDialog>
    private lateinit var contentResolverWeakReference: WeakReference<ContentResolver>
    private lateinit var name: String
    private lateinit var description: String

    constructor(context: Context,
                alertDialog: AlertDialog,
                contentResolver: ContentResolver,
                name: String,
                description: String) : this() {
        this.context = context
        alertDialogWeakReference = WeakReference<AlertDialog>(alertDialog)
        contentResolverWeakReference = WeakReference<ContentResolver>(contentResolver)
        this.name = name
        this.description = description
    }

    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {

    }

    override fun onBitmapFailed(errorDrawable: Drawable?) {
    }

    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
        val resolver = contentResolverWeakReference.get()
        val dialog =  alertDialogWeakReference.get()

        if (resolver != null) {
            MediaStore.Images.Media.insertImage(resolver, bitmap, name, description)
        }
        dialog?.dismiss()

        val intent = Intent()
        with(intent) {
            setType("image/*")
            setAction(Intent.ACTION_GET_CONTENT)
        }
        context.startActivity(Intent.createChooser(intent, "View image"))
    }

}
