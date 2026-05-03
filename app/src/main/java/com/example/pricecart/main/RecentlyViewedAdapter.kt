package com.example.pricecart.main

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pricecart.R
import com.example.pricecart.data.model.RecentlyViewedOffer
import com.example.pricecart.databinding.ItemRecentlyViewedBinding

class RecentlyViewedAdapter(
    private val onItemClicked: (RecentlyViewedOffer) -> Unit,
) : RecyclerView.Adapter<RecentlyViewedAdapter.RecentlyViewedViewHolder>() {
    private val items = mutableListOf<RecentlyViewedOffer>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentlyViewedViewHolder {
        val binding = ItemRecentlyViewedBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return RecentlyViewedViewHolder(binding, onItemClicked)
    }

    override fun onBindViewHolder(holder: RecentlyViewedViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<RecentlyViewedOffer>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class RecentlyViewedViewHolder(
        private val binding: ItemRecentlyViewedBinding,
        private val onItemClicked: (RecentlyViewedOffer) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(recentlyViewedOffer: RecentlyViewedOffer) {
            val context = binding.root.context
            val relativeTime = DateUtils.getRelativeTimeSpanString(
                recentlyViewedOffer.viewedAt,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
            )
            binding.viewedProductNameTextView.text = recentlyViewedOffer.productName
            binding.viewedStoreNameTextView.text = recentlyViewedOffer.storeName
            binding.viewedPriceTextView.text = context.getString(
                R.string.price_format,
                recentlyViewedOffer.currency,
                recentlyViewedOffer.price,
            )
            binding.viewedMetaTextView.text = relativeTime
            binding.root.setOnClickListener {
                onItemClicked(recentlyViewedOffer)
            }
        }
    }
}
