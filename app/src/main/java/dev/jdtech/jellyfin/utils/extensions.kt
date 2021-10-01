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
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.models.View
import dev.jdtech.jellyfin.repository.JellyfinRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import timber.log.Timber
import java.io.File
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

fun Fragment.requestDownload(uri: Uri, title: String, filename: String) {
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
        .setTitle(title)
        .setDescription("Downloading")
        .setDestinationUri(Uri.fromFile(File(defaultStorage, filename)))
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

    requireContext().downloadFile(downloadRequest, 1)
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

suspend fun Context.loadDownloadedEpisodes(jellyfinRepository: JellyfinRepository): List<PlayerItem> {
    var items = mutableListOf<PlayerItem>()
    val defaultStorage = getDownloadLocation()
    defaultStorage?.walk()?.forEach {
        if (it.isFile) {
            items.add(PlayerItem("Test", UUID.fromString(it.name), "", 0, it.absolutePath))
        }
    }
    return items.toList()
}