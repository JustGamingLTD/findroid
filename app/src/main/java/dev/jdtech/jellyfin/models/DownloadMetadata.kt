package dev.jdtech.jellyfin.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DownloadMetadata(
    val seriesName: String?,
    val name: String?,
    val parentIndexNumber: Int?,
    val indexNumber: Int?,
    val playbackPosition: Long?,
) : Parcelable