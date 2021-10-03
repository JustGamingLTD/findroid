package dev.jdtech.jellyfin.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.*

@Parcelize
data class PlayerItem(
    val name: String?,
    val itemId: UUID,
    val mediaSourceId: String,
    val playbackPosition: Long,
    val mediaSourceUri: String = "",
    val metadata: DownloadMetadata? = null
) : Parcelable