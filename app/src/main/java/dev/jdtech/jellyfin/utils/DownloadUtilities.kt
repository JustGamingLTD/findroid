package dev.jdtech.jellyfin.utils

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.models.DownloadMetadata
import dev.jdtech.jellyfin.models.DownloadRequestItem
import dev.jdtech.jellyfin.models.PlayerItem
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.UserItemDataDto
import timber.log.Timber
import java.io.File
import java.util.*

fun Fragment.requestDownload(uri: Uri, downloadRequestItem: DownloadRequestItem) {
    // Storage permission for downloads isn't necessary from Android 10 onwards
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        @Suppress("MagicNumber")
        Timber.d("REQUESTING PERMISSION")

        if (ContextCompat.checkSelfPermission(requireActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(requireActivity(),
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            } else {
                ActivityCompat.requestPermissions(requireActivity(),
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            }
        }

        val granted = ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            requireContext().toast(R.string.download_no_storage_permission)
            return
        }
    }
    val defaultStorage = requireContext().getDownloadLocation()
    Timber.d(defaultStorage.toString())
    val downloadRequest = DownloadManager.Request(uri)
        .setTitle(downloadRequestItem.metadata.name)
        .setDescription("Downloading")
        .setDestinationUri(Uri.fromFile(File(defaultStorage, downloadRequestItem.itemId.toString())))
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

    requireContext().downloadFile(downloadRequest, 1)
    requireContext().createMetadataFile(downloadRequestItem.metadata, downloadRequestItem.itemId)
}

private fun Context.createMetadataFile(metadata: DownloadMetadata, itemId: UUID) {
    val defaultStorage = getDownloadLocation()
    val metadataFile = File(defaultStorage, "${itemId}.metadata")

    metadataFile.writeText("") //This might be necessary to make sure that the metadata file is empty

    if(metadata.type == "Episode") {
        metadataFile.printWriter().use { out ->
            out.println(metadata.id)
            out.println(metadata.type.toString())
            out.println(metadata.seriesName.toString())
            out.println(metadata.name.toString())
            out.println(metadata.parentIndexNumber.toString())
            out.println(metadata.indexNumber.toString())
            out.println(metadata.playbackPosition.toString())
            out.println(metadata.playedPercentage.toString())
            out.println(metadata.seriesId.toString())
        }
    } else if (metadata.type == "Movie") {
        metadataFile.printWriter().use { out ->
            out.println(metadata.id)
            out.println(metadata.type.toString())
            out.println(metadata.name.toString())
            out.println(metadata.playbackPosition.toString())
            out.println(metadata.playedPercentage.toString())
        }
    }

}

private fun Context.downloadFile(request: DownloadManager.Request, downloadMethod: Int) {
    require(downloadMethod >= 0) { "Download method hasn't been set" }
    request.apply {
        setAllowedOverMetered(false)
        setAllowedOverRoaming(false)
    }
    getSystemService<DownloadManager>()?.enqueue(request)
}

private fun Context.getDownloadLocation(): File? {
    return getExternalFilesDir(Environment.DIRECTORY_MOVIES)
}

fun Context.loadDownloadedEpisodes(): List<PlayerItem> {
    val items = mutableListOf<PlayerItem>()
    val defaultStorage = getDownloadLocation()
    defaultStorage?.walk()?.forEach {
        if (it.isFile && it.extension == "") {
            val metadataFile = File(defaultStorage, "${it.name}.metadata").readLines()
            val metadata = parseMetadataFile(metadataFile)
            items.add(PlayerItem(metadata.name, UUID.fromString(it.name), "", metadata.playbackPosition!!, it.absolutePath, metadata))
        }
    }
    return items.toList()
}

fun deleteDownloadedEpisode(uri: String) {
    try {
        File(uri).delete()
        File("${uri}.metadata").delete()
    } catch (e: Exception) {
        Timber.e(e)
    }

}

fun postDownloadPlaybackProgress(uri: String, playbackPosition: Long, playedPercentage: Double) {
    try {
        val metadataFile = File("${uri}.metadata")
        val metadataArray = metadataFile.readLines().toMutableList()
        if(metadataArray[1] == "Episode"){
            metadataArray[6] = playbackPosition.toString()
            metadataArray[7] = playedPercentage.times(100).toString()
        } else if (metadataArray[1] == "Movie") {
            metadataArray[3] = playbackPosition.toString()
            metadataArray[4] = playedPercentage.times(100).toString()
        }

        metadataFile.writeText("") //This might be necessary to make sure that the metadata file is empty
        metadataFile.printWriter().use { out ->
            metadataArray.forEach {
                out.println(it)
            }
        }
    } catch (e: Exception) {
        Timber.e(e)
    }
}

fun downloadMetadataToBaseItemDto(metadata: DownloadMetadata) : BaseItemDto {
    val userData = UserItemDataDto(playbackPositionTicks = metadata.playbackPosition ?: 0,
        playedPercentage = metadata.playedPercentage, isFavorite = false, playCount = 0, played = false) //TODO DO SOMETHING ABOUT THE HARDCODED VALUES

    return BaseItemDto(id = metadata.id,
        type = metadata.type,
        seriesName = metadata.seriesName,
        name = metadata.name,
        parentIndexNumber = metadata.parentIndexNumber,
        indexNumber = metadata.indexNumber,
        userData = userData,
        seriesId = metadata.seriesId
    )
}

fun baseItemDtoToDownloadMetadata(item: BaseItemDto) : DownloadMetadata {
    return DownloadMetadata(id = item.id,
        type = item.type,
        seriesName = item.seriesName,
        name = item.name,
        parentIndexNumber = item.parentIndexNumber,
        indexNumber = item.indexNumber,
        playbackPosition = item.userData?.playbackPositionTicks ?: 0,
        playedPercentage = item.userData?.playedPercentage,
        seriesId = item.seriesId
    )
}

fun parseMetadataFile(metadataFile: List<String>) : DownloadMetadata {
    if (metadataFile[1] == "Episode") {
        return DownloadMetadata(id = UUID.fromString(metadataFile[0]),
            type = metadataFile[1],
            seriesName = metadataFile[2],
            name = metadataFile[3],
            parentIndexNumber = metadataFile[4].toInt(),
            indexNumber = metadataFile[5].toInt(),
            playbackPosition = metadataFile[6].toLong(),
            playedPercentage = if(metadataFile[7] == "null") {null} else {metadataFile[7].toDouble()},
            seriesId = UUID.fromString(metadataFile[8])
        )
    } else {
        return DownloadMetadata(id = UUID.fromString(metadataFile[0]),
            type = metadataFile[1],
            name = metadataFile[2],
            playbackPosition = metadataFile[3].toLong(),
            playedPercentage = if(metadataFile[4] == "null") {null} else {metadataFile[4].toDouble()},
        ) //TODO CHANGE THIS TO PARSE THE FILE IF IT IS A MOVIE
    }

}