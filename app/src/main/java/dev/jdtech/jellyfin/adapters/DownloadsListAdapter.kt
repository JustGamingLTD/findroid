package dev.jdtech.jellyfin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dev.jdtech.jellyfin.databinding.DownloadSectionBinding
import dev.jdtech.jellyfin.models.DownloadSection

class DownloadsListAdapter(
    private val onClickListener: ViewItemListAdapter.OnClickListener,
    private val onEpisodeClickListener: HomeEpisodeListAdapter.OnClickListener
) : ListAdapter<DownloadSection, DownloadsListAdapter.SectionViewHolder>(DiffCallback) {
    class SectionViewHolder(private var binding: DownloadSectionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            section: DownloadSection,
            onClickListener: ViewItemListAdapter.OnClickListener,
            onEpisodeClickListener: HomeEpisodeListAdapter.OnClickListener
        ) {
            binding.section = section
            if (section.name == "Movies" || section.name == "Shows") {
                binding.itemsRecyclerView.adapter =
                    ViewItemListAdapter(onClickListener, fixedWidth = true)
                (binding.itemsRecyclerView.adapter as ViewItemListAdapter).submitList(section.items)
            } else if (section.name == "Episodes") {
                binding.itemsRecyclerView.adapter =
                    HomeEpisodeListAdapter(onEpisodeClickListener)
                (binding.itemsRecyclerView.adapter as HomeEpisodeListAdapter).submitList(section.items)
            }
            binding.executePendingBindings()
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<DownloadSection>() {
        override fun areItemsTheSame(oldItem: DownloadSection, newItem: DownloadSection): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: DownloadSection,
            newItem: DownloadSection
        ): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        return SectionViewHolder(
            DownloadSectionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        val collection = getItem(position)
        holder.bind(collection, onClickListener, onEpisodeClickListener)
    }
}