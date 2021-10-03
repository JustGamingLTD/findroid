package dev.jdtech.jellyfin.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.*

@Parcelize
data class DownloadMetadata(
    val seriesName: String?,
    val name: String?,
    val parentIndexNumber: Int?,
    val indexNumber: Int?,
) : Parcelable