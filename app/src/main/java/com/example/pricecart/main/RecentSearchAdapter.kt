package com.example.pricecart.main

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pricecart.data.model.RecentSearchItem
import com.example.pricecart.databinding.ItemRecentSearchBinding

class RecentSearchAdapter(
    private val onItemClicked: (RecentSearchItem) -> Unit,
) : RecyclerView.Adapter<RecentSearchAdapter.RecentSearchViewHolder>() {
    private val items = mutableListOf<RecentSearchItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentSearchViewHolder {
        val binding = ItemRecentSearchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return RecentSearchViewHolder(binding, onItemClicked)
    }

    override fun onBindViewHolder(holder: RecentSearchViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<RecentSearchItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class RecentSearchViewHolder(
        private val binding: ItemRecentSearchBinding,
        private val onItemClicked: (RecentSearchItem) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(recentSearchItem: RecentSearchItem) {
            val relativeTime = DateUtils.getRelativeTimeSpanString(
                recentSearchItem.searchedAt,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
            )
            binding.recentSearchNameTextView.text = recentSearchItem.queryDisplayName
            binding.recentSearchMetaTextView.text = relativeTime
            binding.root.setOnClickListener {
                onItemClicked(recentSearchItem)
            }
        }
    }
}
