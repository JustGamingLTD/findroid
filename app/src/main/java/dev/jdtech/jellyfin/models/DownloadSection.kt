package dev.jdtech.jellyfin.models

import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.*

data class DownloadSection(
    val id: UUID,
    val name: String,
    var items: List<PlayerItem>
)