package dev.jdtech.jellyfin.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class DownloadMetadata(
    val id: UUID,
    val seriesName: String?,
    val name: String?,
    val parentIndexNumber: Int?,
    val indexNumber: Int?,
    val playbackPosition: Long?,
    val playedPercentage: Double?
) : Parcelable