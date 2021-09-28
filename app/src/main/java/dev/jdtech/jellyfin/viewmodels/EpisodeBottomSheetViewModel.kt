package dev.jdtech.jellyfin.viewmodels

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings.Global.getString
import androidx.fragment.app.Fragment
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.R
import dev.jdtech.jellyfin.fragments.EpisodeBottomSheetFragment
import dev.jdtech.jellyfin.models.PlayerItem
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.utils.toast
import kotlinx.coroutines.*
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.LocationType
import timber.log.Timber
import java.io.File
import java.text.DateFormat
import java.time.ZoneOffset
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class EpisodeBottomSheetViewModel
@Inject
constructor(
    private val jellyfinRepository: JellyfinRepository
) : ViewModel() {

    private val _item = MutableLiveData<BaseItemDto>()
    val item: LiveData<BaseItemDto> = _item

    private val _runTime = MutableLiveData<String>()
    val runTime: LiveData<String> = _runTime

    private val _dateString = MutableLiveData<String>()
    val dateString: LiveData<String> = _dateString

    private val _played = MutableLiveData<Boolean>()
    val played: LiveData<Boolean> = _played

    private val _favorite = MutableLiveData<Boolean>()
    val favorite: LiveData<Boolean> = _favorite

    private val _navigateToPlayer = MutableLiveData<Boolean>()
    val navigateToPlayer: LiveData<Boolean> = _navigateToPlayer

    var playerItems: MutableList<PlayerItem> = mutableListOf()

    private val _playerItemsError = MutableLiveData<String>()
    val playerItemsError: LiveData<String> = _playerItemsError
    fun loadEpisode(episodeId: UUID) {
        viewModelScope.launch {
            try {
                val item = jellyfinRepository.getItem(episodeId)
                _item.value = item
                _runTime.value = "${item.runTimeTicks?.div(600000000)} min"
                _dateString.value = getDateString(item)
                _played.value = item.userData?.played
                _favorite.value = item.userData?.isFavorite
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun preparePlayerItems() {
        _playerItemsError.value = null
        viewModelScope.launch {
            try {
                createPlayerItems(_item.value!!)
                _navigateToPlayer.value = true
            } catch (e: Exception) {
                _playerItemsError.value = e.toString()
            }
        }
    }

    private suspend fun createPlayerItems(startEpisode: BaseItemDto) {
        playerItems.clear()

        val playbackPosition = startEpisode.userData?.playbackPositionTicks?.div(10000) ?: 0
        // Intros
        var introsCount = 0

        if (playbackPosition <= 0) {
            val intros = jellyfinRepository.getIntros(startEpisode.id)
            for (intro in intros) {
                if (intro.mediaSources.isNullOrEmpty()) continue
                playerItems.add(PlayerItem(intro.name, intro.id, intro.mediaSources?.get(0)?.id!!, 0))
                introsCount += 1
            }
        }

        val episodes = jellyfinRepository.getEpisodes(
            startEpisode.seriesId!!,
            startEpisode.seasonId!!,
            startItemId = startEpisode.id,
            fields = listOf(ItemFields.MEDIA_SOURCES)
        )
        for (episode in episodes) {
            if (episode.mediaSources.isNullOrEmpty()) continue
            if (episode.locationType == LocationType.VIRTUAL) continue
            playerItems.add(
                PlayerItem(
                    episode.name,
                    episode.id,
                    episode.mediaSources?.get(0)?.id!!,
                    playbackPosition
                )
            )
        }

        if (playerItems.isEmpty() || playerItems.count() == introsCount) throw Exception("No playable items found")
    }

    fun markAsPlayed(itemId: UUID) {
        viewModelScope.launch {
            jellyfinRepository.markAsPlayed(itemId)
        }
        _played.value = true
    }

    fun markAsUnplayed(itemId: UUID) {
        viewModelScope.launch {
            jellyfinRepository.markAsUnplayed(itemId)
        }
        _played.value = false
    }

    fun markAsFavorite(itemId: UUID) {
        viewModelScope.launch {
            jellyfinRepository.markAsFavorite(itemId)
        }
        _favorite.value = true
    }

    fun unmarkAsFavorite(itemId: UUID) {
        viewModelScope.launch {
            jellyfinRepository.unmarkAsFavorite(itemId)
        }
        _favorite.value = false
    }

    fun download(itemId: UUID, fragment: EpisodeBottomSheetFragment){
        viewModelScope.launch {

            //val episode : BaseItemDto = jellyfinRepository.getItem(itemId)
            loadEpisode(itemId)
            val episode = _item.value
            val uri = jellyfinRepository.getStreamUrl(itemId, episode?.mediaSources?.get(0)?.id!!)
            val title = "${episode.seriesName} S${episode.parentIndexNumber}E${episode.indexNumber} ID${episode.id}"
            //val title = episode.seriesName + "-" + itemId.toString() + ".mkv"
            Timber.d(title)
            fragment.requestDownload(Uri.parse(uri), title, title)

        }

    }

    suspend fun EpisodeBottomSheetFragment.requestDownload(uri: Uri, title: String, filename: String) {
        // Storage permission for downloads isn't necessary from Android 10 onwards
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            @Suppress("MagicNumber")

            val granted = false
            /*
            val granted = withTimeout(2 * 60 * 1000 /* 2 minutes */) {
                suspendCoroutine<Boolean> { continuation ->
                    requireActivity().requestPermission(WRITE_EXTERNAL_STORAGE) { requestPermissionsResult ->
                        continuation.resume(requestPermissionsResult[WRITE_EXTERNAL_STORAGE] == PERMISSION_GRANTED)
                    }
                }
            }*/

            if (!granted) {
                requireContext().toast(R.string.download_no_storage_permission)
                return
            }
            requireContext().toast(R.string.download_no_storage_permission)
        }
        val defaultStorage = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath

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

    private fun getDateString(item: BaseItemDto): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val instant = item.premiereDate?.toInstant(ZoneOffset.UTC)
            val date = Date.from(instant)
            DateFormat.getDateInstance(DateFormat.SHORT).format(date)
        } else {
            // TODO: Implement a way to get the year from LocalDateTime in Android < O
            item.premiereDate.toString()
        }
    }

    fun doneNavigateToPlayer() {
        _navigateToPlayer.value = false
    }
}