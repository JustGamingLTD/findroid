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

    metadataFile.printWriter().use { out ->
        out.println(metadata.seriesName.toString())
        out.println(metadata.name.toString())
        out.println(metadata.parentIndexNumber.toString())
        out.println(metadata.indexNumber.toString())
        out.println(metadata.playbackPosition.toString())
        out.println(metadata.playedPercentage.toString())
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
            val metadata = DownloadMetadata(UUID.fromString(it.name),
                metadataFile[0],
                metadataFile[1],
                metadataFile[2].toInt(),
                metadataFile[3].toInt(),
                metadataFile[4].toLong(),
                if(metadataFile[5] == "null") {null} else {metadataFile[5].toDouble()}) //TODO CONVERT THIS INTO A PROPER FUNCTION
            items.add(PlayerItem(metadata.name, UUID.fromString(it.name), "", metadataFile[4].toLong(), it.absolutePath, metadata))
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
        Timber.d(uri)
        val metadataFile = File("${uri}.metadata")
        val metadataArray = metadataFile.readLines().toMutableList()
        metadataArray[4] = playbackPosition.toString()
        metadataArray[5] = playedPercentage.times(100).toString()
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

    return BaseItemDto(id=metadata.id,
        seriesName = metadata.seriesName,
        name = metadata.name,
        parentIndexNumber = metadata.parentIndexNumber,
        indexNumber = metadata.indexNumber,
        userData = userData
    )
}

fun baseItemDtoToDownloadMetadata(item: BaseItemDto) : DownloadMetadata {
    return DownloadMetadata(item.id, item.seriesName, item.name, item.parentIndexNumber, item.indexNumber,item.userData?.playbackPositionTicks ?: 0, item.userData?.playedPercentage)
}