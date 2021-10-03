package dev.jdtech.jellyfin.utils

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dev.jdtech.jellyfin.MainNavigationDirections
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.models.DownloadMetadata
import dev.jdtech.jellyfin.models.DownloadRequestItem
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.models.View
import dev.jdtech.jellyfin.repository.JellyfinRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.file.Paths
import java.util.*

fun BaseItemDto.toView(): View {
    return View(
        id = id,
        name = name,
        type = collectionType
    )
}

fun Fragment.checkIfLoginRequired(error: String) {
    if (error.contains("401"))  {
        Timber.d("Login required!")
        findNavController().navigate(MainNavigationDirections.actionGlobalLoginFragment())
    }
}

inline fun Context.toast(@StringRes text: Int, duration: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(this, text, duration).show()

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
        .setTitle(downloadRequestItem.itemId.toString())
        .setDescription("Downloading")
        .setDestinationUri(Uri.fromFile(File(defaultStorage, downloadRequestItem.itemId.toString())))
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

    requireContext().downloadFile(downloadRequest, 1)
    requireContext().createMetadataFile(downloadRequestItem)

}

private fun Context.createMetadataFile(downloadRequestItem: DownloadRequestItem) {
    val defaultStorage = getDownloadLocation()
    val metadata = downloadRequestItem.metadata
    var metadataFile = File(defaultStorage, "${downloadRequestItem.itemId.toString()}.metadata")

    metadataFile.writeText("") //This might be necessary to make sure that the metadata file is empty

    metadataFile.printWriter().use { out ->
        out.println(metadata.seriesName.toString())
        out.println(metadata.name.toString())
        out.println(metadata.parentIndexNumber.toString())
        out.println(metadata.indexNumber.toString())
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
    var items = mutableListOf<PlayerItem>()
    val defaultStorage = getDownloadLocation()
    defaultStorage?.walk()?.forEach {
        if (it.isFile && it.extension == "") {
            val metadataFile = File(defaultStorage, "${it.name}.metadata").readLines()
            var metadata = DownloadMetadata(metadataFile[0], metadataFile[1], metadataFile[2].toInt(), metadataFile[3].toInt())
            items.add(PlayerItem(metadata.name, UUID.fromString(it.name), "", 0, it.absolutePath, metadata))
        }
    }
    return items.toList()
}

fun deleteDownloadedEpisode(uri: String) {
    try {
        File(uri).delete()
        File("${uri}.metadata").delete()
    } catch (e: Exception) {

    }

}
