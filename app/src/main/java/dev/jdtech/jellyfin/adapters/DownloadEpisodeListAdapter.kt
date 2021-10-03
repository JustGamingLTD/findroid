package dev.jdtech.jellyfin.adapters

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.databinding.DownloadEpisodeItemBinding
import dev.jdtech.jellyfin.databinding.HomeEpisodeItemBinding
import dev.jdtech.jellyfin.models.PlayerItem
import org.jellyfin.sdk.model.api.BaseItemDto

class DownloadEpisodeListAdapter(private val onClickListener: OnClickListener) : ListAdapter<PlayerItem, DownloadEpisodeListAdapter.EpisodeViewHolder>(DiffCallback) {
    class EpisodeViewHolder(private var binding: DownloadEpisodeItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(episode: PlayerItem) {
            binding.episode = episode
            /*if (episode.userData?.playedPercentage != null) {
                binding.progressBar.layoutParams.width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    (episode.userData?.playedPercentage?.times(2.24))!!.toFloat(), binding.progressBar.context.resources.displayMetrics).toInt()
                binding.progressBar.visibility = View.VISIBLE
            }*/
            /* //TODO MAKE THIS WORK WITH MOVIES
            if (episode.type == "Movie") {
                binding.primaryName.text = episode.name
                binding.secondaryName.visibility = View.GONE
            } else if (episode.type == "Episode") {
                binding.primaryName.text = episode.seriesName
            }*/
            binding.primaryName.text = episode.metadata!!.seriesName
            binding.executePendingBindings()
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<PlayerItem>() {
        override fun areItemsTheSame(oldItem: PlayerItem, newItem: PlayerItem): Boolean {
            return oldItem.itemId == newItem.itemId
        }

        override fun areContentsTheSame(oldItem: PlayerItem, newItem: PlayerItem): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        return EpisodeViewHolder(
            DownloadEpisodeItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.setOnClickListener {
            onClickListener.onClick(item)
        }
        holder.bind(item)
    }

    class OnClickListener(val clickListener: (item: PlayerItem) -> Unit) {
        fun onClick(item: PlayerItem) = clickListener(item)
    }
}