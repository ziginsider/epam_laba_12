package io.github.ziginsider.epam_laba_12.download

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Download(val progress: Int,
                    val currentFileSize: Int,
                    val totalFileSize: Int): Parcelable