package com.example.pricecart.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pricecart.R
import com.example.pricecart.data.model.StoreOfferResult
import com.example.pricecart.databinding.ItemStorePriceBinding

class StorePriceAdapter(
    private val onItemClicked: (StoreOfferResult) -> Unit,
) : RecyclerView.Adapter<StorePriceAdapter.StorePriceViewHolder>() {
    private val items = mutableListOf<StoreOfferResult>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StorePriceViewHolder {
        val binding = ItemStorePriceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return StorePriceViewHolder(binding, onItemClicked)
    }

    override fun onBindViewHolder(holder: StorePriceViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<StoreOfferResult>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class StorePriceViewHolder(
        private val binding: ItemStorePriceBinding,
        private val onItemClicked: (StoreOfferResult) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(storeOfferResult: StoreOfferResult, position: Int) {
            val context = binding.root.context
            binding.productNameTextView.text = storeOfferResult.productName
            binding.storeNameTextView.text = context.getString(
                R.string.store_name_prefix,
                storeOfferResult.storeName,
            )
            binding.priceTextView.text = context.getString(
                R.string.price_format,
                storeOfferResult.currency,
                storeOfferResult.price,
            )
            binding.rankBadgeTextView.text = if (position == 0) {
                context.getString(R.string.best_match_badge)
            } else {
                context.getString(R.string.rank_badge, position + 1)
            }
            binding.distanceTextView.text = storeOfferResult.distanceKilometers?.let { distance ->
                context.getString(R.string.distance_kilometers, distance)
            } ?: context.getString(R.string.distance_unavailable)
            binding.detailHintTextView.text = context.getString(R.string.tap_for_details)
            binding.root.setOnClickListener {
                onItemClicked(storeOfferResult)
            }
        }
    }
}
