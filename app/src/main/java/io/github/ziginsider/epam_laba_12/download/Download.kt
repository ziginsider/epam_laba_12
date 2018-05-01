package io.github.ziginsider.epam_laba_12.download

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Model of data of downloading file
 *
 * @since 2018-04-25
 * @author Alex Kisel
 */
@Parcelize
data class Download(val progress: Int,
                    val currentFileSize: Int,
                    val totalFileSize: Int) : Parcelable
